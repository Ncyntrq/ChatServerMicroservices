package gui.chat;

import gui.components.feedback.Toast;
import network.ChatWebSocketClient;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;

import javax.swing.SwingUtilities;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;

/** Tập trung mọi thao tác gửi tin qua WebSocket: chat, sửa, xóa, broadcast thông báo ngầm. */
public class OutboundMessageController {

    private final ChatWebSocketClient wsClient;
    private final String sessionUsername;
    private final BiConsumer<Toast.Level, String> feedback;

    public OutboundMessageController(ChatWebSocketClient wsClient, String sessionUsername, BiConsumer<Toast.Level, String> feedback) {
        this.wsClient = wsClient;
        this.sessionUsername = sessionUsername;
        this.feedback = feedback;
    }

    /** Gửi tin chat (kênh) hoặc tin riêng (DM) tùy ngữ cảnh đang mở. */
    public void sendChat(String text, long activeChannelId, long activeServerId, String activePrivateUser) {
        MessageDTO out;
        if (activeChannelId == -1 && activePrivateUser != null) {
            out = new MessageDTO(MessageType.PRIVATE, sessionUsername, null, text, LocalDateTime.now());
            out.setReceiver(activePrivateUser);
        } else {
            out = new MessageDTO(MessageType.CHAT, sessionUsername, null, text, LocalDateTime.now());
            out.setServerId(activeServerId);
            out.setChannelId(activeChannelId);
        }
        sendWithError(out, "Gửi thất bại: ");
    }

    /** Gửi yêu cầu sửa nội dung 1 tin nhắn. */
    public void sendEdit(MessageDTO original, String newContent) {
        if (!wsClient.isOpen() || original.getMessageId() == null) return;
        MessageDTO out = new MessageDTO(MessageType.EDIT, sessionUsername, null, newContent, LocalDateTime.now());
        copyRouting(original, out);
        sendWithError(out, "Sửa thất bại: ");
    }

    /** Gửi yêu cầu xóa 1 tin nhắn. */
    public void sendDelete(MessageDTO original) {
        if (!wsClient.isOpen() || original.getMessageId() == null) return;
        MessageDTO out = new MessageDTO(MessageType.DELETE, sessionUsername, null, null, LocalDateTime.now());
        copyRouting(original, out);
        sendWithError(out, "Xóa thất bại: ");
    }

    /** Gửi 1 "thông báo ngầm" (cập nhật realtime cho client khác). Bỏ qua nếu WS chưa mở. */
    public void broadcast(MessageType type, String content, Long serverId, Long channelId, String receiver) {
        if (!wsClient.isOpen()) return;
        MessageDTO out = new MessageDTO(type, sessionUsername, null, content, LocalDateTime.now());
        out.setServerId(serverId);
        out.setChannelId(channelId);
        if (receiver != null) out.setReceiver(receiver);
        wsClient.send(out);
    }

    private void copyRouting(MessageDTO from, MessageDTO to) {
        to.setMessageId(from.getMessageId());
        to.setChannelId(from.getChannelId());
        to.setServerId(from.getServerId());
        to.setReceiver(from.getReceiver());
    }

    private void sendWithError(MessageDTO out, String errPrefix) {
        wsClient.send(out).whenComplete((ws, err) -> {
            if (err != null) SwingUtilities.invokeLater(() -> feedback.accept(Toast.Level.ERROR, errPrefix + err.getMessage()));
        });
    }
}
