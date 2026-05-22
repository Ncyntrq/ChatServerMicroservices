package gui.components.navigation;

import gui.theme.AppColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ServerIconItem extends JPanel {
    private boolean isHovered = false;
    private boolean isActive = false;
    private boolean hasUnread = false;
    private final JLabel iconLabel;

    public ServerIconItem(String iconSymbol) {
        setPreferredSize(new Dimension(72, 58));
        setMaximumSize(new Dimension(72, 58));
        setMinimumSize(new Dimension(72, 58)); // ADD THIS
        setAlignmentX(Component.CENTER_ALIGNMENT); // ADD THIS
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setLayout(new BorderLayout());

        iconLabel = new JLabel(iconSymbol);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setOpaque(false);

        add(iconLabel, BorderLayout.CENTER);

        updateStyles();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                updateStyles();
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                updateStyles();
                repaint();
            }
        });
    }

    public void setActive(boolean active) {
        this.isActive = active;
        updateStyles();
        repaint();
    }

    public void setHasUnread(boolean unread) {
        this.hasUnread = unread;
        repaint();
    }

    private void updateStyles() {
        if (isActive || isHovered) {
            iconLabel.setForeground(AppColors.TEXT_WHITE);
        } else {
            iconLabel.setForeground(AppColors.TEXT_MUTED);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- 1. DETERMINE DYNAMIC LOOKS ---
        Color backgroundColor;
        int cornerRadius = 16;

        if (isActive || isHovered) {
            backgroundColor = AppColors.BRAND_PRIMARY;
        } else {
            backgroundColor = AppColors.BG_PRIMARY;
        }

        // --- 2. DRAW BUTTON BACKGROUND CONTAINER ---
        int size = 40;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        g2.setColor(backgroundColor);
        g2.fillRoundRect(x, y, size, size, cornerRadius, cornerRadius);

        // --- 3. DRAW LEFT SIDE STATUS INDICATOR PILL ---
        g2.setColor(Color.WHITE);

        int pillOffset = -getX(); // negative of how far we are from the left

        if (isActive) {
            g2.fillRoundRect(pillOffset, (getHeight() - 40) / 2, 4, 40, 4, 4);
        } else if (isHovered) {
            g2.fillRoundRect(pillOffset, (getHeight() - 20) / 2, 4, 20, 4, 4);
        } else if (hasUnread) {
            g2.fillRoundRect(pillOffset, (getHeight() - 8) / 2, 4, 8, 4, 4);
        }

        // --- 4. DRAW RED UNREAD NOTIFICATION BADGE ---
        if (hasUnread) {
            int badgeX = x + size - 12;
            int badgeY = y;

            g2.setColor(AppColors.BG_TERTIARY);
            g2.fillOval(badgeX - 2, badgeY - 2, 16, 16);

            g2.setColor(Color.decode("#F23F42"));
            g2.fillOval(badgeX, badgeY, 12, 12);
        }

        g2.dispose();
    }
}