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
import java.util.function.LongConsumer;
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
    /** Phát "thông báo ngầm" tới client khác sau khi ghim/bỏ ghim (truyền channelId). */
    private final LongConsumer pinChangeBroadcaster;

    // Đảm bảo chỉ 1 popup ghim mở tại 1 thời điểm.
    private PinnedMessagesDialog dialog;
    private boolean loading;

    public PinController(Window parent, BiConsumer<Toast.Level, String> feedback,
                         ChannelApiClient channelApi, LongSupplier activeChannelIdSupplier,
                         LongConsumer pinChangeBroadcaster) {
        this.parent = parent;
        this.feedback = feedback;
        this.channelApi = channelApi;
        this.activeChannelIdSupplier = activeChannelIdSupplier;
        this.pinChangeBroadcaster = pinChangeBroadcaster;
    }

    /** Lấy danh sách tin đã ghim của kênh (chạy nền). Dùng chung cho mở dialog + refresh real-time. */
    private List<MessageDTO> fetchPinned(long channelId) {
        List<Map<String, Object>> pins = channelApi.getPinnedMessages(channelId);
        List<Long> msgIds = new ArrayList<>();
        for (Map<String, Object> p : pins) {
            if (p.get("messageId") != null) {
                msgIds.add(((Number) p.get("messageId")).longValue());
            }
        }
        return channelApi.getMessagesByIds(msgIds);
    }

    /** Báo cho client khác biết danh sách ghim của kênh vừa đổi. */
    private void broadcastPinChange(long channelId) {
        if (pinChangeBroadcaster != null) pinChangeBroadcaster.accept(channelId);
    }

    /**
     * Nhận broadcast ghim/bỏ ghim từ client khác → nếu đang mở dialog của đúng kênh thì refresh.
     */
    public void onRemotePinUpdate(Long channelId) {
        if (channelId == null || dialog == null || !dialog.isVisible()) return;
        if (channelId != activeChannelIdSupplier.getAsLong()) return;
        new SwingWorker<List<MessageDTO>, Void>() {
            @Override protected List<MessageDTO> doInBackground() { return fetchPinned(channelId); }
            @Override protected void done() {
                try { if (dialog != null) dialog.setPinned(get()); } catch (Exception ignore) {}
            }
        }.execute();
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
                    broadcastPinChange(channelId); // real-time cho client khác
                } catch (Exception e) {
                    feedback.accept(Toast.Level.ERROR, "Lỗi ghim: " + e.getMessage());
                }
            }
        }.execute();
    }

    /** Mở popup danh sách tin đã ghim — fetch từ API. */
    public void openDialog() {
        // Nếu popup đang mở → đưa lên trước, không tạo cửa sổ mới.
        if (dialog != null && dialog.isVisible()) {
            dialog.toFront();
            return;
        }
        if (loading) return; // đang fetch dở, bỏ qua click lặp
        long channelId = activeChannelIdSupplier.getAsLong();
        if (channelId == -1) return;
        loading = true;
        new SwingWorker<List<MessageDTO>, Void>() {
            @Override protected List<MessageDTO> doInBackground() {
                return fetchPinned(channelId);
            }
            @Override protected void done() {
                loading = false;
                try {
                    List<MessageDTO> fetched = get();
                    if (dialog != null) dialog.dispose(); // gỡ instance cũ đã ẩn
                    dialog = new PinnedMessagesDialog(parent, fetched, PinController.this::unpin);
                    dialog.setVisible(true);
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
                    broadcastPinChange(channelId); // real-time cho client khác
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
