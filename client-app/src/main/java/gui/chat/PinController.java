package gui.chat;

import gui.components.dialogs.PinnedMessagesDialog;
import gui.components.feedback.Toast;
import network.ChannelApiClient;
import com.chatsever.common.dto.MessageDTO;

import javax.swing.*;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

/**
 * Quản lý ghim tin nhắn — nối trực tiếp với backend API (channel-service).
 * Mỗi thao tác ghim/bỏ ghim đều gọi REST API để persist trên server,
 * đảm bảo mọi user trong channel đều thấy cùng danh sách ghim.
 */
public class PinController {

    private final Window parent;
    private final BiConsumer<Toast.Level, String> feedback;
    private final ChannelApiClient channelApi;
    private final LongSupplier activeChannelIdSupplier;

    public PinController(Window parent, BiConsumer<Toast.Level, String> feedback,
                         ChannelApiClient channelApi, LongSupplier activeChannelIdSupplier) {
        this.parent = parent;
        this.feedback = feedback;
        this.channelApi = channelApi;
        this.activeChannelIdSupplier = activeChannelIdSupplier;
    }

    /** Ghim 1 tin nhắn — gọi API lên backend. */
    public void pin(MessageDTO msg) {
        long channelId = activeChannelIdSupplier.getAsLong();
        if (channelId == -1 || msg.getMessageId() == null) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                channelApi.pinMessage(channelId, msg.getMessageId());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    feedback.accept(Toast.Level.SUCCESS, "📌 Đã ghim tin nhắn của " + msg.getSender());
                } catch (Exception e) {
                    feedback.accept(Toast.Level.ERROR, "Lỗi ghim: " + e.getMessage());
                }
            }
        }.execute();
    }

    /** Mở popup danh sách tin đã ghim — fetch từ API. */
    public void openDialog() {
        long channelId = activeChannelIdSupplier.getAsLong();
        if (channelId == -1) return;
        new SwingWorker<List<MessageDTO>, Void>() {
            @Override protected List<MessageDTO> doInBackground() {
                List<Map<String, Object>> pins = channelApi.getPinnedMessages(channelId);
                List<Long> msgIds = new ArrayList<>();
                for (Map<String, Object> p : pins) {
                    if (p.get("messageId") != null) {
                        msgIds.add(((Number) p.get("messageId")).longValue());
                    }
                }
                return channelApi.getMessagesByIds(msgIds);
            }
            @Override protected void done() {
                try {
                    List<MessageDTO> fetched = get();
                    new PinnedMessagesDialog(parent, fetched, PinController.this::unpin).setVisible(true);
                } catch (Exception e) {
                    feedback.accept(Toast.Level.ERROR, "Lỗi lấy danh sách ghim: " + e.getMessage());
                }
            }
        }.execute();
    }

    /** Bỏ ghim — gọi API. */
    private void unpin(MessageDTO m) {
        long channelId = activeChannelIdSupplier.getAsLong();
        if (channelId == -1 || m.getMessageId() == null) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                channelApi.unpinMessage(channelId, m.getMessageId());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    feedback.accept(Toast.Level.SUCCESS, "Đã bỏ ghim tin nhắn.");
                } catch (Exception e) {
                    feedback.accept(Toast.Level.ERROR, "Lỗi bỏ ghim: " + e.getMessage());
                }
            }
        }.execute();
    }

    /** Gỡ ghim theo messageId (khi tin bị xóa) — không cần gọi API vì tin đã bị xóa. */
    public void removeByMessageId(Long messageId) {
        // No-op: backend sẽ tự xử lý khi tin nhắn bị soft-delete
    }

    /** Xóa context cục bộ (khi chuyển channel/DM). */
    public void clear() {
        // No-op: không còn local list
    }
}
