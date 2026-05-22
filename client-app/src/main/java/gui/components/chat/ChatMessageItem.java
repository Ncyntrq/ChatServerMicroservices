package gui.components.chat;

import com.chatsever.common.dto.MessageDTO;
import gui.components.AvatarBadge;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ChatMessageItem extends JPanel {
    // Boolean determines if the message mentions the current user
    public ChatMessageItem(MessageDTO message, boolean isHighlighted) {
        setLayout(new BorderLayout(15, 0));

        Color defaultBg = Color.decode("#313338");
        Color highlightBg = Color.decode("#3F4147");
        setBackground(isHighlighted ? highlightBg : defaultBg);

        // --- 1. THE HIGHLIGHT BORDER & PADDING ---
        if (isHighlighted) {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, Color.decode("#F9A826")),
                    BorderFactory.createEmptyBorder(10, 16, 10, 20)
            ));
        } else {
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        // --- 2. THE AVATAR ---
        String senderName = message.getSender();
        String initial = senderName != null && !senderName.isEmpty()
                ? senderName.substring(0, 1).toUpperCase()
                : "?";

        AvatarBadge avatar = new AvatarBadge(initial);

        JPanel avatarWrapper = new JPanel(new BorderLayout());
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatar, BorderLayout.NORTH);

        // --- 3. CONTENT CONTAINER ---
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 3a. The header row (Username, Badge, Time)
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel senderLabel = new JLabel(senderName);
        senderLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        senderLabel.setForeground(Color.WHITE);
        headerRow.add(senderLabel);

        // TODO: Change the condition of Admin Badge
        if ("admin".equalsIgnoreCase(message.getSender())) {
            JLabel badge = new JLabel(" ADMIN ");
            badge.setOpaque(true);
            badge.setBackground(Color.decode("#5865F2"));
            badge.setForeground(Color.WHITE);
            badge.setFont(new Font("SansSerif", Font.BOLD, 10));
            badge.putClientProperty("JComponent.arc", 10);
            headerRow.add(badge);
        }

        // Timestamp Formatting
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        String timeStr = message.getTimestamp() != null
                ? message.getTimestamp().format(formatter)
                : "Now";

        JLabel timeLabel = new JLabel(timeStr);
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        timeLabel.setForeground(Color.decode("#80848E"));
        headerRow.add(timeLabel);

        // 3b. The Message Body
        JTextArea messageBody = new JTextArea(message.getContent());
        messageBody.setLineWrap(true);
        messageBody.setWrapStyleWord(true);
        messageBody.setEditable(false);
        messageBody.setOpaque(false);
        messageBody.setForeground(Color.decode("#DBDEE1"));
        messageBody.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageBody.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 0));

        // 4. --- ASSEMBLE ---
        contentPanel.add(headerRow);
        contentPanel.add(messageBody);
        add(avatarWrapper, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
