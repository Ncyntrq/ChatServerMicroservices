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
    private Runnable onContextMenu;

    public void setOnContextMenu(Runnable onContextMenu) {
        this.onContextMenu = onContextMenu;
    }

    private int unreadCount = 0;
    private JPanel badgePanel;
    private JLabel badgeLabel;
    private final JLabel nameLabel;

    public void setUnreadCount(int count) {
        this.unreadCount = count;
        if (count > 0) {
            badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
            badgePanel.setVisible(true);
            nameLabel.setForeground(AppColors.TEXT_WHITE);
        } else {
            badgePanel.setVisible(false);
            nameLabel.setForeground(statusColor == AppColors.STATUS_OFFLINE ? AppColors.TEXT_MUTED : AppColors.TEXT_NORMAL);
        }
        revalidate();
        repaint();
    }

    public UserListItem(String username, String customStatus, Color statusColor) {
        this(username, customStatus, statusColor, true);
    }

    public UserListItem(String username, String customStatus, Color statusColor, boolean isOnline) {
        this.statusColor = statusColor;

        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 10));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover & Right-click
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
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && onContextMenu != null) {
                    onContextMenu.run();
                }
            }
        });

        // Avatar
        String initial = username.substring(0, 1).toUpperCase();
        avatar = new AvatarBadge(initial, 32);
        
        // Mute offline avatars
        if (!isOnline) {
            avatar.setOpaque(false); // Make avatar visually muted if supported
        }

        JPanel avatarWrapper = new JPanel(new BorderLayout());
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatar, BorderLayout.NORTH);

        // Text
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        nameLabel = new JLabel(username);
        nameLabel.setForeground(isOnline ? AppColors.TEXT_NORMAL : AppColors.TEXT_MUTED);
        nameLabel.setFont(AppFonts.BODY_SM);
        textPanel.add(nameLabel);

        if (customStatus != null && !customStatus.isEmpty()) {
            JLabel statusLabel = new JLabel(customStatus);
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setFont(AppFonts.TINY);
            textPanel.add(statusLabel);
        } else {
            // Add an empty placeholder for dynamic status
            JLabel statusLabel = new JLabel(" ");
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setFont(AppFonts.TINY);
            statusLabel.setVisible(false);
            textPanel.add(statusLabel);

            // Fetch profile async to get displayName, avatar and customStatus
            new SwingWorker<java.util.Map<String, Object>, Void>() {
                @Override
                protected java.util.Map<String, Object> doInBackground() {
                    return new network.UserProfileApiClient().getProfile(username);
                }
                @Override
                protected void done() {
                    try {
                        java.util.Map<String, Object> profile = get();
                        if (profile != null) {
                            if (profile.get("displayName") != null && !profile.get("displayName").toString().isBlank()) {
                                nameLabel.setText(profile.get("displayName").toString());
                            }
                            if (profile.get("avatarUrl") != null) {
                                String url = profile.get("avatarUrl").toString();
                                if (!url.startsWith("http")) url = network.ApiConfig.GATEWAY_HTTP + url;
                                avatar.loadAvatarFromUrl(url);
                            }
                            if (profile.get("customStatus") != null && !profile.get("customStatus").toString().isBlank()) {
                                statusLabel.setText(profile.get("customStatus").toString());
                                statusLabel.setVisible(true);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ignore) {}
                }
            }.execute();
        }

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

        add(avatarWrapper, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        add(eastWrapper, BorderLayout.EAST);
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
