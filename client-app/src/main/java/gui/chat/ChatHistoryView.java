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
    private final JPanel floatingLayer;
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

        floatingLayer = new JPanel(null);
        floatingLayer.setOpaque(false);

        JPanel viewportPanel = new JPanel() {
            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false;
            }
        };
        viewportPanel.setLayout(new OverlayLayout(viewportPanel));
        
        floatingLayer.setAlignmentX(0f);
        floatingLayer.setAlignmentY(0f);
        chatHistoryPanel.setAlignmentX(0f);
        chatHistoryPanel.setAlignmentY(0f);

        viewportPanel.add(floatingLayer);
        viewportPanel.add(chatHistoryPanel);

        setViewportView(viewportPanel);
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

        ChatMessageItem item = new ChatMessageItem(message, isHighlighted, sessionUsername, messageActions, isConsecutive, floatingLayer);

        int insertIndex = chatHistoryPanel.getComponentCount() - 1;
        chatHistoryPanel.add(item, insertIndex);

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

        // Bổ sung: nếu xóa tin đầu tiên (có avatar), biến tin tiếp theo thành tin đầu tiên để hiện avatar
        if (!item.isConsecutive()) {
            if (idx < chatHistoryPanel.getComponentCount() && chatHistoryPanel.getComponent(idx) instanceof ChatMessageItem) {
                ChatMessageItem nextItem = (ChatMessageItem) chatHistoryPanel.getComponent(idx);
                if (nextItem.isConsecutive() && 
                    item.getMessage().getSender().equals(nextItem.getMessage().getSender())) {
                    
                    MessageDTO nextMsg = nextItem.getMessage();
                    boolean highlighted = nextItem.isHighlighted();
                    // Tạo lại item nhưng KHÔNG consecutive (để hiện avatar)
                    ChatMessageItem upgradedItem = new ChatMessageItem(nextMsg, highlighted, sessionUsername, messageActions, false, floatingLayer);
                    
                    messageItems.put(nextMsg.getMessageId(), upgradedItem);
                    chatHistoryPanel.remove(idx);
                    chatHistoryPanel.add(upgradedItem, idx);
                }
            }
        }
        if (item.getToolbar() != null) {
            floatingLayer.remove(item.getToolbar());
        }
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
        floatingLayer.revalidate();
        floatingLayer.repaint();
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

    /** Cuộn đến một tin nhắn cụ thể và chớp nháy màu nền. */
    public void scrollToMessage(Long messageId) {
        if (messageId == null) return;
        ChatMessageItem item = messageItems.get(messageId);
        if (item != null) {
            SwingUtilities.invokeLater(() -> {
                // Cuộn đến vị trí của tin nhắn
                Rectangle bounds = item.getBounds();
                chatHistoryPanel.scrollRectToVisible(bounds);
                
                // Hiệu ứng chớp nền nhẹ
                Color oldColor = item.getBackground();
                item.setBackground(new Color(88, 101, 242, 60)); // Blurple highlight
                item.setOpaque(true);
                item.repaint();
                
                Timer timer = new Timer(1500, e -> {
                    item.setOpaque(false);
                    item.setBackground(oldColor);
                    item.repaint();
                });
                timer.setRepeats(false);
                timer.start();
            });
        } else {
            JOptionPane.showMessageDialog(this, 
                "Tin nhắn này nằm ngoài lịch sử hiện tại. Vui lòng cuộn lên để tải thêm.", 
                "Không tìm thấy", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
