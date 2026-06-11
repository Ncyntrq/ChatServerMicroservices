package gui.components.chat;

import gui.components.AvatarBadge;
import gui.components.PresenceStatusIcon;
import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Item hiển thị 1 user trong danh sách bạn bè / thành viên server.
 * Hỗ trợ cập nhật trạng thái real-time qua {@link #updatePresenceStatus(String)}.
 */
public class UserListItem extends JPanel {
    private static final int AVATAR_SIZE = 36;
    // Kích thước icon trạng thái overlay lên avatar (góc dưới phải)
    private static final int STATUS_ICON_SIZE = 14;

    private PresenceStatusIcon.Status currentStatus;
    private final AvatarBadge avatar;
    private final JLabel nameLabel;
    private final JLabel statusTextLabel;
    private boolean isHovered = false;
    private Runnable onContextMenu;

    public void setOnContextMenu(Runnable onContextMenu) {
        this.onContextMenu = onContextMenu;
    }

    private int unreadCount = 0;
    private gui.components.chat.UnreadBadgePanel badgePanel;

    public void setUnreadCount(int count) {
        this.unreadCount = count;
        badgePanel.setCount(count);
        if (count > 0) {
            nameLabel.setForeground(AppColors.TEXT_WHITE);
        } else {
            nameLabel.setForeground(currentStatus == PresenceStatusIcon.Status.OFFLINE
                    || currentStatus == PresenceStatusIcon.Status.INVISIBLE
                    ? AppColors.TEXT_MUTED : AppColors.TEXT_NORMAL);
        }
        revalidate();
        repaint();
    }

    /** Constructor tương thích ngược. */
    public UserListItem(String username, String customStatus, Color statusColor) {
        this(username, customStatus, statusColor, true);
    }

    /**
     * @param statusColor màu dot (dùng để suy ra Status ban đầu từ màu AppColors).
     * @param isOnline    hiển thị màu tên sáng hay mờ ban đầu.
     */
    public UserListItem(String username, String customStatus, Color statusColor, boolean isOnline) {
        this.currentStatus = resolveStatusFromColor(statusColor, isOnline);

        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 10));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover & Right-click
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { isHovered = false; repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && onContextMenu != null) onContextMenu.run();
            }
        });

        // Avatar
        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
        avatar = new AvatarBadge(initial, AVATAR_SIZE);

        JPanel avatarWrapper = new JPanel(new BorderLayout());
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatar, BorderLayout.NORTH);

        // Text
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        nameLabel = new JLabel(username);
        nameLabel.setForeground(isOnline ? AppColors.TEXT_HEADER : AppColors.TEXT_MUTED);
        nameLabel.setFont(AppFonts.BODY_BOLD);
        textPanel.add(nameLabel);

        String defaultStatusText = customStatus != null && !customStatus.isBlank()
                ? customStatus
                : currentStatus.toVietnamese();
        statusTextLabel = new JLabel(defaultStatusText);
        statusTextLabel.setForeground(AppColors.TEXT_MUTED);
        statusTextLabel.setFont(AppFonts.CAPTION);
        textPanel.add(statusTextLabel);

        // Fetch profile async (displayName, avatar, customStatus)
        if (customStatus == null || customStatus.isBlank()) {
            java.util.Map<String, Object> cachedProfile = network.UserProfileCache.get(username);
            if (cachedProfile != null) {
                applyProfileToUI(cachedProfile);
            } else {
                avatar.setLoading(true);
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
                                applyProfileToUI(profile);
                            } else {
                                avatar.setLoading(false);
                            }
                        } catch (Exception ignore) {
                            avatar.setLoading(false);
                        }
                    }
                }.execute();
            }
        }

        // Badge unread
        badgePanel = new gui.components.chat.UnreadBadgePanel();
        JPanel eastWrapper = new JPanel(new GridBagLayout());
        eastWrapper.setOpaque(false);
        eastWrapper.add(badgePanel);

        add(avatarWrapper, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        add(eastWrapper, BorderLayout.EAST);
    }

    private void applyProfileToUI(java.util.Map<String, Object> profile) {
        if (profile.get("displayName") != null
                && !profile.get("displayName").toString().isBlank()) {
            nameLabel.setText(profile.get("displayName").toString());
        }
        boolean hasUrl = false;
        if (profile.get("avatarUrl") != null) {
            String url = profile.get("avatarUrl").toString();
            if (!url.isBlank()) {
                hasUrl = true;
                if (!url.startsWith("http")) url = network.ApiConfig.GATEWAY_HTTP + url;
                avatar.loadAvatarFromUrl(url);
            }
        }
        if (!hasUrl) {
            avatar.setLoading(false);
        }
        
        if (profile.get("customStatus") != null
                && !profile.get("customStatus").toString().isBlank()) {
            statusTextLabel.setText(profile.get("customStatus").toString());
            statusTextLabel.setVisible(true);
        }
    }

    /**
     * Cập nhật trạng thái real-time khi nhận WebSocket STATUS event.
     * Tự động đổi màu dot overlay + text tiếng Việt + độ sáng tên.
     */
    public void updatePresenceStatus(String statusStr) {
        PresenceStatusIcon.Status newStatus = PresenceStatusIcon.Status.from(statusStr);
        this.currentStatus = newStatus;

        boolean online = newStatus != PresenceStatusIcon.Status.OFFLINE
                      && newStatus != PresenceStatusIcon.Status.INVISIBLE;
        if (unreadCount == 0) {
            nameLabel.setForeground(online ? AppColors.TEXT_HEADER : AppColors.TEXT_MUTED);
        }
        // Cập nhật text trạng thái (chỉ nếu không có custom status text)
        String cur = statusTextLabel.getText();
        for (PresenceStatusIcon.Status s : PresenceStatusIcon.Status.values()) {
            if (s.toVietnamese().equals(cur)) {
                // Đang dùng text mặc định → đổi theo status mới
                statusTextLabel.setText(newStatus.toVietnamese());
                break;
            }
        }
        repaint();
    }

    // ---------------------------------------------------------------
    // Paint
    // ---------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        if (isHovered) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(AppColors.BG_HOVER);
            g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 10, 10);
            g2.dispose();
        }
        super.paintComponent(g);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // Vẽ status icon (Discord-style) overlay góc dưới-phải avatar
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Point p = SwingUtilities.convertPoint(avatar.getParent(), avatar.getLocation(), this);
        int iconSize = STATUS_ICON_SIZE;
        int x = p.x + AVATAR_SIZE - iconSize + 2;
        int y = p.y + AVATAR_SIZE - iconSize + 2;

        // Cutout vòng đệm trắng quanh icon để nổi bật trên avatar
        Color bg = isHovered ? AppColors.BG_HOVER : AppColors.BG_SECONDARY;
        g2.setColor(bg);
        int pad = 2;
        g2.fillOval(x - pad, y - pad, iconSize + pad * 2, iconSize + pad * 2);

        // Vẽ icon theo từng trạng thái (giống PresenceStatusIcon logic)
        paintStatusDot(g2, x, y, iconSize, bg);

        g2.dispose();
    }

    private void paintStatusDot(Graphics2D g, int x, int y, int size, Color bg) {
        // Dùng PresenceStatusIcon.paintStatus() để đảm bảo nhất quán với mọi nơi khác
        // (AWAY = đồng hồ cam, IDLE = trăng vàng, etc.)
        gui.components.PresenceStatusIcon.paintStatus(
                g, currentStatus,
                x + size / 2f,
                y + size / 2f,
                size / 2f,
                bg);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Suy ra Status từ màu AppColors để tương thích ngược với code cũ. */
    private static PresenceStatusIcon.Status resolveStatusFromColor(Color c, boolean isOnline) {
        if (!isOnline) return PresenceStatusIcon.Status.OFFLINE;
        if (c == null) return PresenceStatusIcon.Status.ONLINE;
        if (c.equals(AppColors.STATUS_IDLE))    return PresenceStatusIcon.Status.IDLE;
        if (c.equals(AppColors.STATUS_DND))     return PresenceStatusIcon.Status.DO_NOT_DISTURB;
        if (c.equals(AppColors.STATUS_OFFLINE)) return PresenceStatusIcon.Status.OFFLINE;
        return PresenceStatusIcon.Status.ONLINE;
    }
}
