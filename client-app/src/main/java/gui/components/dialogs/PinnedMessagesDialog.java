package gui.components.dialogs;

import com.chatsever.common.dto.MessageDTO;
import gui.components.AvatarBadge;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import gui.theme.ThinScrollBarUI;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Popup danh sách tin nhắn đã ghim của kênh đang mở (feature #8).
 * Mỗi item: avatar + tên người gửi + ngày gửi + nội dung + nút "Bỏ ghim".
 */
public class PinnedMessagesDialog extends JDialog {

    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final List<MessageDTO> pinned;
    private final Consumer<MessageDTO> onUnpin;
    private final JPanel listPanel;

    public PinnedMessagesDialog(Window owner, List<MessageDTO> pinned, Consumer<MessageDTO> onUnpin) {
        super(owner, "Tin nhắn đã ghim", ModalityType.MODELESS);
        this.pinned = pinned;
        this.onUnpin = onUnpin;

        setSize(420, 460);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppColors.BG_SECONDARY);
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Header
        JLabel header = new JLabel("  📌  Tin nhắn đã ghim");
        header.setFont(AppFonts.BODY_BOLD);
        header.setForeground(AppColors.TEXT_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        header.setOpaque(true);
        header.setBackground(AppColors.BG_TERTIARY);
        root.add(header, BorderLayout.NORTH);

        // List
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_SECONDARY);
        listPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(AppColors.BG_SECONDARY);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // bỏ thanh trượt ngang thừa
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        ThinScrollBarUI.apply(scroll); // style scrollbar mảnh, đồng bộ với chat
        root.add(scroll, BorderLayout.CENTER);

        setContentPane(root);
        renderList();
    }

    private void renderList() {
        listPanel.removeAll();
        if (pinned == null || pinned.isEmpty()) {
            JLabel empty = new JLabel("Chưa có tin nhắn nào được ghim");
            empty.setForeground(AppColors.TEXT_MUTED);
            empty.setFont(AppFonts.BODY);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 8, 0, 0));
            listPanel.add(empty);
        } else {
            for (MessageDTO m : pinned) {
                listPanel.add(buildPinnedItem(m));
                listPanel.add(Box.createVerticalStrut(8));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildPinnedItem(MessageDTO m) {
        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppColors.BG_FLOATING);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        // Avatar
        String sender = m.getSender() != null ? m.getSender() : "?";
        String initial = sender.isEmpty() ? "?" : sender.substring(0, 1).toUpperCase();
        AvatarBadge avatar = new AvatarBadge(initial, 32);
        JPanel avatarWrap = new JPanel(new BorderLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.add(avatar, BorderLayout.NORTH);
        card.add(avatarWrap, BorderLayout.WEST);

        // Center: sender + date + content
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nameLabel = new JLabel(sender);
        nameLabel.setFont(AppFonts.BODY_BOLD);
        nameLabel.setForeground(AppColors.avatarColorFor(sender));
        topRow.add(nameLabel);
        JLabel dateLabel = new JLabel(m.getTimestamp() != null ? m.getTimestamp().format(FULL_FMT) : "");
        dateLabel.setFont(AppFonts.TINY);
        dateLabel.setForeground(AppColors.TEXT_MUTED);
        topRow.add(dateLabel);
        center.add(topRow);

        JLabel contentLabel = new JLabel("<html><body style='width:230px'>"
                + escapeHtml(m.getContent()) + "</body></html>");
        contentLabel.setFont(AppFonts.BODY_SM);
        contentLabel.setForeground(AppColors.TEXT_NORMAL);
        contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(contentLabel);

        card.add(center, BorderLayout.CENTER);

        // Unpin button
        JButton unpinBtn = new JButton("Bỏ ghim");
        unpinBtn.setFont(AppFonts.TINY);
        unpinBtn.setForeground(AppColors.TEXT_NORMAL);
        unpinBtn.setBackground(AppColors.BG_ACTIVE);
        unpinBtn.setFocusPainted(false);
        unpinBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        unpinBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        unpinBtn.addActionListener(e -> {
            if (onUnpin != null) onUnpin.accept(m);
            pinned.remove(m);
            renderList();
        });
        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setOpaque(false);
        btnWrap.add(unpinBtn, BorderLayout.NORTH);
        card.add(btnWrap, BorderLayout.EAST);

        return card;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
