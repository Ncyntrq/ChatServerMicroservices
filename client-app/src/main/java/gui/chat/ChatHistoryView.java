package gui.chat;

import gui.components.chat.ChatMessageItem;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import gui.theme.ThinScrollBarUI;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Khung cuộn hiển thị danh sách tin nhắn của kênh/DM đang mở.
 * Tự quản lý: render item, gộp tin liên tiếp, theo dõi item theo messageId
 * để cập nhật/xóa tại chỗ khi nhận EDIT/DELETE.
 */
public class ChatHistoryView extends JScrollPane {

    private final JPanel chatHistoryPanel;
    private final String sessionUsername;
    private final ChatMessageItem.MessageActions messageActions;

    // Theo dõi item theo messageId để cập nhật/xóa tại chỗ khi nhận EDIT/DELETE
    private final Map<Long, ChatMessageItem> messageItems = new LinkedHashMap<>();
    private String lastSender = null;
    private boolean showingPlaceholder = false;
    private String placeholderText = "Chọn một kênh hoặc người bạn để bắt đầu trò chuyện";

    public ChatHistoryView(String sessionUsername, ChatMessageItem.MessageActions messageActions) {
        this.sessionUsername = sessionUsername;
        this.messageActions = messageActions;

        chatHistoryPanel = new JPanel();
        chatHistoryPanel.setLayout(new BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS));
        chatHistoryPanel.setBackground(AppColors.BG_PRIMARY);
        chatHistoryPanel.add(Box.createVerticalGlue());

        setViewportView(chatHistoryPanel);
        setBorder(BorderFactory.createEmptyBorder());
        getVerticalScrollBar().setUnitIncrement(16);
        ThinScrollBarUI.apply(this);
    }

    /** Thêm 1 tin nhắn vào cuối danh sách và cuộn xuống. */
    public void appendMessage(MessageDTO message) {
        if (showingPlaceholder) {
            chatHistoryPanel.removeAll();
            chatHistoryPanel.add(Box.createVerticalGlue());
            showingPlaceholder = false;
        }

        boolean isHighlighted = message.getContent() != null &&
                message.getContent().contains("@" + sessionUsername);

        boolean isConsecutive = false;
        if (message.getType() != MessageType.SYSTEM && message.getType() != MessageType.JOIN
                && message.getType() != MessageType.LEAVE && message.getType() != MessageType.ERROR
                && !"SYSTEM".equals(message.getSender())) {
            if (message.getSender() != null && message.getSender().equals(lastSender)) {
                isConsecutive = true;
            }
            lastSender = message.getSender();
        } else {
            lastSender = null;
        }

        ChatMessageItem item = new ChatMessageItem(message, isHighlighted, sessionUsername, messageActions, isConsecutive);

        int insertIndex = chatHistoryPanel.getComponentCount() - 1;
        chatHistoryPanel.add(item, insertIndex);
        chatHistoryPanel.add(Box.createVerticalStrut(10), insertIndex + 1);

        if (message.getMessageId() != null) {
            messageItems.put(message.getMessageId(), item);
        }

        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    /** Thêm 1 tin hệ thống (SYSTEM) vào luồng chat. */
    public void appendSystem(String text) {
        MessageDTO sys = new MessageDTO(MessageType.SYSTEM, "SYSTEM", null, text, LocalDateTime.now());
        appendMessage(sys);
    }

    /** Đặt nội dung gợi ý cho màn hình trống (vd Home vs kênh rỗng). */
    public void setPlaceholderText(String text) {
        this.placeholderText = text;
    }

    /** Xóa toàn bộ lịch sử đang hiển thị + hiện empty-state (khi chuyển channel/DM). */
    public void clear() {
        chatHistoryPanel.removeAll();
        lastSender = null;
        messageItems.clear();
        showingPlaceholder = true;
        chatHistoryPanel.add(Box.createVerticalGlue());
        chatHistoryPanel.add(buildPlaceholder());
        chatHistoryPanel.add(Box.createVerticalGlue());
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
    }

    private JComponent buildPlaceholder() {
        JLabel label = new JLabel(
                "<html><div style='text-align:center;'>💬<br><br>" + placeholderText + "</div></html>",
                SwingConstants.CENTER);
        label.setFont(AppFonts.BODY);
        label.setForeground(AppColors.TEXT_MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    /** Áp dụng broadcast EDIT: cập nhật nội dung item tại chỗ. */
    public void applyEdit(MessageDTO msg) {
        if (msg.getMessageId() == null) return;
        ChatMessageItem item = messageItems.get(msg.getMessageId());
        if (item != null) {
            item.updateContent(msg.getContent(), true);
            chatHistoryPanel.revalidate();
            chatHistoryPanel.repaint();
        }
    }

    /** Áp dụng broadcast DELETE: gỡ item khỏi danh sách hiển thị. */
    public void applyDelete(MessageDTO msg) {
        if (msg.getMessageId() == null) return;
        ChatMessageItem item = messageItems.remove(msg.getMessageId());
        if (item == null) return;

        Component[] comps = chatHistoryPanel.getComponents();
        int idx = -1;
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] == item) { idx = i; break; }
        }
        if (idx < 0) return;
        chatHistoryPanel.remove(item);
        // Xóa luôn strut đệm ngay sau tin nhắn (nếu có)
        if (idx < chatHistoryPanel.getComponentCount()) {
            Component next = chatHistoryPanel.getComponent(idx);
            if (next instanceof Box.Filler) chatHistoryPanel.remove(next);
        }
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
    }

    /** Mở chỉnh sửa tin nhắn cuối cùng của chính mình trong kênh đang mở. */
    public void startEditingLastOwn() {
        ChatMessageItem target = null;
        for (ChatMessageItem item : messageItems.values()) {
            MessageDTO m = item.getMessage();
            if (m != null && sessionUsername.equals(m.getSender())) {
                target = item; // LinkedHashMap giữ thứ tự chèn → cái cuối là mới nhất
            }
        }
        if (target != null) target.startEditing();
    }
}
