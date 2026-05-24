package gui.components.chat;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import gui.components.AvatarBadge;
import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;

public class ChatMessageItem extends JPanel {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private boolean isHovered = false;
    private final boolean isHighlighted;
    private final boolean isSystemMsg;

    public ChatMessageItem(MessageDTO message, boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
        this.isSystemMsg = message.getType() == MessageType.SYSTEM
                || message.getType() == MessageType.JOIN
                || message.getType() == MessageType.LEAVE
                || message.getType() == MessageType.ERROR
                || "SYSTEM".equals(message.getSender());

        setLayout(new BorderLayout(12, 0));
        setOpaque(false);

        // Padding
        if (isHighlighted) {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, AppColors.MSG_HIGHLIGHT_BORDER),
                    BorderFactory.createEmptyBorder(8, 16, 8, 20)
            ));
        } else {
            setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        }

        // Hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });

        if (isSystemMsg) {
            buildSystemLayout(message);
        } else {
            buildChatLayout(message);
        }
    }

    private void buildSystemLayout(MessageDTO message) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        row.setOpaque(false);

        // Dash line left
        JLabel dash = new JLabel("—");
        dash.setForeground(AppColors.TEXT_MUTED);
        dash.setFont(AppFonts.CAPTION);

        // System icon
        String icon = "SYSTEM".equals(message.getSender()) ? "ℹ" : "→";
        if (message.getType() == MessageType.ERROR) icon = "⚠";
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setForeground(message.getType() == MessageType.ERROR
                ? AppColors.WARNING : AppColors.TEXT_MUTED);
        iconLabel.setFont(AppFonts.BODY_SM);

        // Content
        JLabel content = new JLabel(message.getContent());
        content.setFont(AppFonts.BODY_SM);
        content.setForeground(AppColors.TEXT_MUTED);

        // Time
        String time = message.getTimestamp() != null
                ? message.getTimestamp().format(TIME_FMT) : "";
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(AppFonts.TINY);
        timeLabel.setForeground(new Color(0x80, 0x84, 0x8E, 0x80));

        row.add(dash);
        row.add(iconLabel);
        row.add(content);
        row.add(timeLabel);
        row.add(new JLabel("—") {{ setForeground(AppColors.TEXT_MUTED); setFont(AppFonts.CAPTION); }});

        add(row, BorderLayout.CENTER);
    }

    private void buildChatLayout(MessageDTO message) {
        String senderName = message.getSender();
        String initial = senderName != null && !senderName.isEmpty()
                ? senderName.substring(0, 1).toUpperCase() : "?";

        // Avatar
        AvatarBadge avatar = new AvatarBadge(initial, 40);
        JPanel avatarWrapper = new JPanel(new BorderLayout());
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatar, BorderLayout.NORTH);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Header row: username + badges + timestamp
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel senderLabel = new JLabel(senderName);
        senderLabel.setFont(AppFonts.BODY_BOLD);
        senderLabel.setForeground(AppColors.avatarColorFor(senderName));
        headerRow.add(senderLabel);

        // Admin badge
        if ("admin".equalsIgnoreCase(message.getSender())) {
            JLabel badge = new JLabel(" ADMIN ") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(AppColors.BRAND_PRIMARY);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setForeground(Color.WHITE);
            badge.setFont(AppFonts.TINY);
            badge.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            headerRow.add(badge);
        }

        // Edited badge
        if (Boolean.TRUE.equals(message.getIsEdited())) {
            JLabel editedBadge = new JLabel("(đã sửa)");
            editedBadge.setFont(AppFonts.TINY);
            editedBadge.setForeground(AppColors.TEXT_MUTED);
            headerRow.add(editedBadge);
        }

        // Timestamp
        String timeStr = message.getTimestamp() != null
                ? message.getTimestamp().format(TIME_FMT)
                : "Bây giờ";
        JLabel timeLabel = new JLabel(timeStr);
        timeLabel.setFont(AppFonts.CAPTION);
        timeLabel.setForeground(new Color(0x80, 0x84, 0x8E, 0x99));
        headerRow.add(timeLabel);

        // Message body
        JTextArea messageBody = new JTextArea(message.getContent());
        messageBody.setLineWrap(true);
        messageBody.setWrapStyleWord(true);
        messageBody.setEditable(false);
        messageBody.setOpaque(false);
        messageBody.setForeground(AppColors.TEXT_NORMAL);
        messageBody.setFont(AppFonts.BODY);
        messageBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageBody.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        contentPanel.add(headerRow);
        contentPanel.add(messageBody);
        add(avatarWrapper, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isHighlighted) {
            g2.setColor(AppColors.MSG_HIGHLIGHT_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else if (isHovered && !isSystemMsg) {
            g2.setColor(new Color(0x04, 0x04, 0x04, 0x10));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
