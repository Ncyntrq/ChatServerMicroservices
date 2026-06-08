package gui.chat;

import gui.components.dialogs.PinnedMessagesDialog;
import gui.components.feedback.Toast;
import com.chatsever.common.dto.MessageDTO;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Quản lý danh sách tin nhắn đã ghim của kênh đang mở.
 * Hiện lưu phía client theo phiên — Phase 5 sẽ nối API persist + broadcast WS.
 */
public class PinController {

    private final Window parent;
    private final BiConsumer<Toast.Level, String> feedback;
    private final List<MessageDTO> pinned = new ArrayList<>();

    public PinController(Window parent, BiConsumer<Toast.Level, String> feedback) {
        this.parent = parent;
        this.feedback = feedback;
    }

    /** Ghim 1 tin nhắn (bỏ qua nếu đã ghim). */
    public void pin(MessageDTO msg) {
        if (msg.getMessageId() != null) {
            boolean exists = pinned.stream().anyMatch(m -> msg.getMessageId().equals(m.getMessageId()));
            if (!exists) pinned.add(msg);
        }
        feedback.accept(Toast.Level.SUCCESS, "📌 Đã ghim tin nhắn của " + msg.getSender());
    }

    /** Mở popup danh sách tin đã ghim. */
    public void openDialog() {
        new PinnedMessagesDialog(parent, pinned, this::unpin).setVisible(true);
    }

    private void unpin(MessageDTO m) {
        if (m.getMessageId() != null) {
            pinned.removeIf(p -> m.getMessageId().equals(p.getMessageId()));
        } else {
            pinned.remove(m);
        }
    }

    /** Gỡ ghim theo messageId (khi tin bị xóa). */
    public void removeByMessageId(Long messageId) {
        if (messageId == null) return;
        pinned.removeIf(m -> messageId.equals(m.getMessageId()));
    }

    /** Xóa toàn bộ danh sách ghim (khi chuyển channel/DM). */
    public void clear() {
        pinned.clear();
    }
}
