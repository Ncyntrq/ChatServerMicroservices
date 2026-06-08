package gui.chat;

import gui.components.chat.ChatMessageItem;
import gui.components.feedback.Toast;
import network.ChatWebSocketClient;
import network.FileApiClient;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;

import javax.swing.*;
import java.awt.Component;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Chọn 1 tệp tin, upload qua file-service rồi gửi tin nhắn đính kèm qua WebSocket. */
public class FileUploadController {

    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024; // server giới hạn 10MB

    // Whitelist loại tệp được phép gửi (ảnh + tài liệu phổ biến)
    private static final Set<String> ALLOWED_EXT = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "zip", "rar", "7z");

    private final Component parent;
    private final FileApiClient fileApi;
    private final ChatWebSocketClient wsClient;
    private final String sessionUsername;
    private final BiConsumer<Toast.Level, String> feedback;
    private final Consumer<Boolean> uploadingState;

    public FileUploadController(Component parent, FileApiClient fileApi, ChatWebSocketClient wsClient,
                               String sessionUsername, BiConsumer<Toast.Level, String> feedback,
                               Consumer<Boolean> uploadingState) {
        this.parent = parent;
        this.fileApi = fileApi;
        this.wsClient = wsClient;
        this.sessionUsername = sessionUsername;
        this.feedback = feedback;
        this.uploadingState = uploadingState;
    }

    public void chooseAndSend(long activeChannelId, long activeServerId, String activePrivateUser) {
        if (!wsClient.isOpen()) { feedback.accept(Toast.Level.WARN, "WebSocket chưa sẵn sàng"); return; }
        if (activeChannelId == -1 && activePrivateUser == null) {
            feedback.accept(Toast.Level.WARN, "Hãy mở một kênh hoặc cuộc trò chuyện trước khi gửi tệp");
            return;
        }

        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        final java.io.File file = fc.getSelectedFile();
        if (file == null || !file.isFile()) return;

        // Chặn sớm phía client: loại tệp không hợp lệ
        if (!ALLOWED_EXT.contains(extensionOf(file.getName()))) {
            feedback.accept(Toast.Level.WARN, "Loại tệp \"" + file.getName() + "\" không được hỗ trợ");
            return;
        }
        // Chặn sớm phía client: tránh upload tốn công rồi bị server từ chối
        if (file.length() > MAX_UPLOAD_BYTES) {
            feedback.accept(Toast.Level.WARN, "Tệp \"" + file.getName() + "\" vượt quá 10MB, vui lòng chọn tệp nhỏ hơn");
            return;
        }

        final long ch = activeChannelId;
        final long sv = activeServerId;
        final String dm = activePrivateUser;
        feedback.accept(Toast.Level.INFO, "Đang tải lên: " + file.getName() + "…");
        uploadingState.accept(true);

        new SwingWorker<FileApiClient.Uploaded, Void>() {
            @Override
            protected FileApiClient.Uploaded doInBackground() {
                return fileApi.uploadFile(file, ch != -1 ? ch : null);
            }

            @Override
            protected void done() {
                uploadingState.accept(false);
                try {
                    FileApiClient.Uploaded up = get();
                    String content = ChatMessageItem.encodeAttachment(
                            up.contentType, up.size, up.thumbnailUrl, up.url, up.name);
                    MessageDTO out;
                    if (ch == -1 && dm != null) {
                        out = new MessageDTO(MessageType.PRIVATE, sessionUsername, null, content, LocalDateTime.now());
                        out.setReceiver(dm);
                    } else {
                        out = new MessageDTO(MessageType.CHAT, sessionUsername, null, content, LocalDateTime.now());
                        out.setServerId(sv);
                        out.setChannelId(ch);
                    }
                    wsClient.send(out).whenComplete((ws, err) -> {
                        if (err != null) SwingUtilities.invokeLater(
                                () -> feedback.accept(Toast.Level.ERROR, "Gửi tệp thất bại: " + err.getMessage()));
                    });
                } catch (Exception ex) {
                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    feedback.accept(Toast.Level.ERROR, "Tải tệp thất bại: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }
}
