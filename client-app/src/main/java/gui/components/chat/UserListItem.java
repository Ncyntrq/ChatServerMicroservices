package gui.components.chat;

import gui.components.AvatarBadge;
import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UserListItem extends JPanel {
    private final Color statusColor;
    private final AvatarBadge avatar;
    private boolean isHovered = false;

    public UserListItem(String username, String customStatus, Color statusColor) {
        this.statusColor = statusColor;

        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 10));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover
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

        // Avatar
        String initial = username.substring(0, 1).toUpperCase();
        avatar = new AvatarBadge(initial, 32);

        JPanel avatarWrapper = new JPanel(new BorderLayout());
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatar, BorderLayout.NORTH);

        // Text
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(username);
        nameLabel.setForeground(AppColors.TEXT_NORMAL);
        nameLabel.setFont(AppFonts.BODY_SM);
        textPanel.add(nameLabel);

        if (customStatus != null && !customStatus.isEmpty()) {
            JLabel statusLabel = new JLabel(customStatus);
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setFont(AppFonts.TINY);
            textPanel.add(statusLabel);
        }

        add(avatarWrapper, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isHovered) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(AppColors.BG_HOVER);
            g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 6, 6);
            g2.dispose();
        }
        super.paintComponent(g);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        // Status dot overlay on avatar
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Point p = SwingUtilities.convertPoint(avatar.getParent(), avatar.getLocation(), this);
        int x = p.x + 22;
        int y = p.y + 22;

        // Cutout circle
        g2.setColor(isHovered ? AppColors.BG_HOVER : AppColors.BG_SECONDARY);
        g2.fillOval(x, y, 14, 14);

        // Status dot
        g2.setColor(statusColor);
        g2.fillOval(x + 2, y + 2, 10, 10);

        g2.dispose();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
