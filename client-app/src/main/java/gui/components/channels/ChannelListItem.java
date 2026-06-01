package gui.components.channels;

import gui.theme.AppColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChannelListItem extends JPanel {
    private boolean isHovered = false;
    private final boolean isVoice;
    private final JLabel nameLabel;

    private Runnable onClick;          // left-click → chọn channel
    private Runnable onContextMenu;    // right-click → edit/delete

    public void setOnClick(Runnable onClick) { this.onClick = onClick; }
    public void setOnContextMenu(Runnable onContextMenu) { this.onContextMenu = onContextMenu; }

    private int unreadCount = 0;
    private final JPanel badgePanel;
    private final JLabel badgeLabel;

    public void setUnreadCount(int count) {
        this.unreadCount = count;
        if (count > 0) {
            badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
            badgePanel.setVisible(true);
            nameLabel.setForeground(AppColors.TEXT_WHITE);
        } else {
            badgePanel.setVisible(false);
            nameLabel.setForeground(isHovered ? AppColors.TEXT_WHITE : AppColors.TEXT_MUTED);
        }
        revalidate();
        repaint();
    }

    public ChannelListItem(String channelName, boolean isVoice) {
        this.isVoice = isVoice;

        setLayout(new BorderLayout(8, 0));
        setPreferredSize(new Dimension(224, 34));
        setMaximumSize(new Dimension(224, 34));
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        // Prefix Icon (# for text, 🔊 for voice)
        JLabel prefixLabel = new JLabel(isVoice ? "🔊" : "#");
        prefixLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        prefixLabel.setForeground(AppColors.TEXT_MUTED);

        // Channel Name
        nameLabel = new JLabel(channelName);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        nameLabel.setForeground(AppColors.TEXT_MUTED);
        
        // Badge Panel (Red Circle)
        badgeLabel = new JLabel("");
        badgeLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        badgePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppColors.DANGER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badgePanel.setOpaque(false);
        badgePanel.setPreferredSize(new Dimension(20, 16));
        badgePanel.add(badgeLabel, BorderLayout.CENTER);
        badgePanel.setVisible(false);
        
        // Wrapper for badge to align vertically
        JPanel eastWrapper = new JPanel(new GridBagLayout());
        eastWrapper.setOpaque(false);
        eastWrapper.add(badgePanel);

        add(prefixLabel, BorderLayout.WEST);
        add(nameLabel, BorderLayout.CENTER);
        add(eastWrapper, BorderLayout.EAST);

        // Hover Animation logic
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                nameLabel.setForeground(AppColors.TEXT_WHITE);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                if (unreadCount == 0) {
                    nameLabel.setForeground(AppColors.TEXT_MUTED);
                }
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (onContextMenu != null) onContextMenu.run();
                } else if (onClick != null) {
                    onClick.run();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isHovered) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(AppColors.BG_HOVER); // Discord hover overlay gray
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
        }
        super.paintComponent(g);
    }
}