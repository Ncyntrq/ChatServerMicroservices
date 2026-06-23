package gui.chat;

import gui.components.chat.ChatMessageItem;
import gui.components.chat.VerticalScrollablePanel;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import gui.theme.ThinScrollBarUI;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
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
    private LocalDateTime lastTimestamp = null;
    /** Tin cùng người trong khoảng này → gộp nhóm (chỉ tin đầu hiện giờ). */
    private static final long GROUP_GAP_MINUTES = 5;
    /** Cách tin trước ≥ ngưỡng này (phút) → chừa khoảng trống rộng hơn. */
    private static final long BIG_GAP_MINUTES = 60;
    private static final int BIG_GAP_PX = 18;
    private boolean showingPlaceholder = false;
    private String placeholderText = "Chọn một kênh hoặc người bạn để bắt đầu trò chuyện";

    public ChatHistoryView(String sessionUsername, ChatMessageItem.MessageActions messageActions) {
        this.sessionUsername = sessionUsername;
        this.messageActions = messageActions;

        chatHistoryPanel = new VerticalScrollablePanel(); // bám bề rộng viewport ⇒ tin tự wrap, không cuộn ngang
        chatHistoryPanel.setLayout(new BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS));
        chatHistoryPanel.setOpaque(false); // để lộ hoạ tiết nền vẽ ở paintComponent
        chatHistoryPanel.add(Box.createVerticalGlue());

        setViewportView(chatHistoryPanel);
        getViewport().setOpaque(false); // viewport trong suốt → nền wallpaper hiện xuyên qua
        setBorder(BorderFactory.createEmptyBorder());
        getVerticalScrollBar().setUnitIncrement(16);
        ThinScrollBarUI.apply(this);
    }

    /**
     * Vẽ nền vùng chat: hoạ tiết wallpaper (gradient + pattern theo theme) nếu bật,
     * ngược lại nền phẳng BG_PRIMARY. Nền cố định, không cuộn theo tin nhắn.
     */
    @Override
    protected void paintComponent(java.awt.Graphics g) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        gui.theme.WallpaperManager wm = gui.theme.WallpaperManager.get();
        if (wm.isEnabled()) {
            gui.theme.WallpaperRenderer.paint(g2, getWidth(), getHeight(),
                    gui.theme.ThemeManager.get().current(), wm.activePatternId());
        } else {
            g2.setColor(AppColors.BG_PRIMARY);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }

    /** Thêm 1 tin nhắn vào cuối danh sách và cuộn xuống. */
    public void appendMessage(MessageDTO message) {
        // Tính TRƯỚC khi thêm: chỉ tự cuộn xuống nếu đang ở đáy hoặc là tin của chính mình
        // ⇒ không cướp vị trí khi user đang đọc lịch sử.
        boolean own = sessionUsername != null && sessionUsername.equals(message.getSender());
        boolean stickToBottom = own || isAtBottom();

        if (showingPlaceholder) {
            chatHistoryPanel.removeAll();
            chatHistoryPanel.add(Box.createVerticalGlue());
            showingPlaceholder = false;
        }

        boolean isHighlighted = message.getContent() != null &&
                message.getContent().contains("@" + sessionUsername);

        boolean isConsecutive = false;
        boolean bigGap = false; // cách tin trước ≥ 1 giờ → chừa khoảng rộng hơn
        if (message.getType() != MessageType.SYSTEM && message.getType() != MessageType.JOIN
                && message.getType() != MessageType.LEAVE && message.getType() != MessageType.ERROR
                && !"SYSTEM".equals(message.getSender())) {
            LocalDateTime ts = message.getTimestamp();
            if (lastTimestamp != null && ts != null) {
                long gapMin = Math.abs(Duration.between(lastTimestamp, ts).toMinutes());
                // Gộp nhóm khi: CÙNG người gửi VÀ cách tin trước ≤ 5 phút (chỉ tin đầu hiện giờ).
                if (message.getSender() != null && message.getSender().equals(lastSender)
                        && gapMin < GROUP_GAP_MINUTES) {
                    isConsecutive = true;
                }
                bigGap = gapMin >= BIG_GAP_MINUTES;
            }
            lastSender = message.getSender();
            lastTimestamp = ts;
        } else {
            lastSender = null;
            lastTimestamp = null;
        }

        ChatMessageItem item = new ChatMessageItem(message, isHighlighted, sessionUsername, messageActions, isConsecutive);

        int insertIndex = chatHistoryPanel.getComponentCount() - 1;
        // Cách tin trước ≥ 1 giờ → chèn khoảng trống rộng hơn phía trên tin này.
        if (bigGap) {
            chatHistoryPanel.add(Box.createVerticalStrut(BIG_GAP_PX), insertIndex);
            insertIndex++;
        }
        chatHistoryPanel.add(item, insertIndex);
        chatHistoryPanel.add(Box.createVerticalStrut(3), insertIndex + 1);

        if (message.getMessageId() != null) {
            messageItems.put(message.getMessageId(), item);
        }

        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();

        if (stickToBottom) {
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        }
    }

    /** Đang ở (gần) đáy danh sách? Ngưỡng ~40px để bỏ qua sai số layout. */
    private boolean isAtBottom() {
        JScrollBar v = getVerticalScrollBar();
        return v.getValue() + v.getVisibleAmount() >= v.getMaximum() - 40;
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
        lastTimestamp = null;
        messageItems.clear();
        showingPlaceholder = true;
        chatHistoryPanel.add(Box.createVerticalGlue());
        chatHistoryPanel.add(buildPlaceholder());
        chatHistoryPanel.add(Box.createVerticalGlue());
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
    }

    private JComponent buildPlaceholder() {
        // Empty-state có mascot + animation (hiệu ứng khi chưa mở kênh/DM).
        return new gui.components.chat.EmptyStatePanel(placeholderText);
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

    /** Áp dụng broadcast REACT: cập nhật reaction cho tin nhắn. */
    public void applyReaction(MessageDTO msg) {
        if (msg.getMessageId() == null || msg.getContent() == null) return;
        ChatMessageItem item = messageItems.get(msg.getMessageId());
        if (item == null) return;

        String[] parts = msg.getContent().split(":", 2);
        if (parts.length < 2) return;
        String action = parts[0];
        String emoji = parts[1];
        String userId = msg.getSender();
        
        java.util.List<MessageDTO.ReactionDTO> reactions = item.getMessage().getReactions();
        if (reactions == null) {
            reactions = new java.util.ArrayList<>();
            item.getMessage().setReactions(reactions);
        }

        if ("ADD".equals(action)) {
            boolean found = false;
            for (MessageDTO.ReactionDTO r : reactions) {
                if (r.getUserId().equals(userId) && r.getEmoji().equals(emoji)) {
                    r.setCount(r.getCount() + 1);
                    found = true;
                    break;
                }
            }
            if (!found) reactions.add(new MessageDTO.ReactionDTO(userId, emoji, 1));
        } else if ("REMOVE".equals(action)) {
            reactions.removeIf(r -> r.getUserId().equals(userId) && r.getEmoji().equals(emoji));
        }

        item.renderReactions();
    }

    /** Áp dụng broadcast DELETE: soft-delete — bật cờ isDeleted = true, ẩn nội dung. */
    public void applyDelete(MessageDTO msg) {
        if (msg.getMessageId() == null) return;
        ChatMessageItem item = messageItems.get(msg.getMessageId());
        if (item == null) return;

        item.applySoftDelete();
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
    }

    /** Tin nhắn có nằm trong phần đã tải hiện tại không? */
    public boolean hasMessage(Long messageId) {
        return messageId != null && messageItems.containsKey(messageId);
    }

    /**
     * Cuộn tới tin nhắn theo messageId và nháy viền highlight để định vị.
     * Trả về false nếu tin nhắn chưa được tải (caller có thể fallback).
     */
    public boolean scrollToMessage(Long messageId) {
        if (messageId == null) return false;
        ChatMessageItem item = messageItems.get(messageId);
        if (item == null) return false;

        SwingUtilities.invokeLater(() -> {
            Rectangle bounds = item.getBounds();
            // Căn item vào giữa vùng nhìn nếu có thể
            Rectangle view = new Rectangle(bounds.x, Math.max(0, bounds.y - 40),
                    bounds.width, bounds.height + 80);
            chatHistoryPanel.scrollRectToVisible(view);
            flashHighlight(item);
        });
        return true;
    }

    /** Nháy nền highlight tạm thời ~1.2s để người dùng định vị tin nhắn. */
    private void flashHighlight(ChatMessageItem item) {
        Color original = item.getBackground();
        boolean wasOpaque = item.isOpaque();
        item.setOpaque(true);
        item.setBackground(AppColors.MSG_HIGHLIGHT_BG);
        item.repaint();
        Timer timer = new Timer(1200, e -> {
            item.setBackground(original);
            item.setOpaque(wasOpaque);
            item.repaint();
        });
        timer.setRepeats(false);
        timer.start();
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

    /** Trả về danh sách tất cả các tin nhắn đang hiển thị. */
    public java.util.List<MessageDTO> getAllMessages() {
        java.util.List<MessageDTO> list = new java.util.ArrayList<>();
        for (ChatMessageItem item : messageItems.values()) {
            if (item.getMessage() != null) {
                list.add(item.getMessage());
            }
        }
        return list;
    }
}
