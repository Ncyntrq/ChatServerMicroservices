package gui.components.channels;

import gui.components.AvatarBadge;
import gui.components.AppIcons;
import gui.components.PresenceStatusIcon;
import gui.components.chat.IconButton;
import gui.profile.UserSettingsDialog;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.PresenceApiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel footer: avatar + tên + trạng thái real-time + nút cài đặt.
 *
 * Status dot được vẽ bằng {@link PresenceStatusIcon#paintStatus} trong {@link #paint(Graphics)}
 * — không embed component riêng, tránh hoàn toàn vấn đề z-order avatar đè lên icon.
 *
 * Tất cả icon dùng Java2D (AppIcons) — không phụ thuộc font/emoji, không ô vuông.
 */
public class UserFooterPanel extends JPanel {

    private AvatarBadge userAvatar;
    private final String username;
    private final JLabel statusLabel;
    private final PresenceApiClient presenceApi = new PresenceApiClient();

    private PresenceStatusIcon.Status currentStatus = PresenceStatusIcon.Status.ONLINE;

    private static final int DOT_SIZE   = 13;
    private static final int AVATAR_SIZE = 36;

    public UserFooterPanel(String username, Runnable onUserChanged) {
        this.username = username;
        setLayout(new BorderLayout(8, 0));
        setBackground(AppColors.BG_TERTIARY);
        setPreferredSize(new Dimension(240, 56));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        // ── Avatar ───────────────────────────────────────────────────
        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
        userAvatar = new AvatarBadge(initial, AVATAR_SIZE);
        userAvatar.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        loadUserAvatar(userAvatar, username);

        // ── Identity text ─────────────────────────────────────────────
        JPanel identityPanel = new JPanel();
        identityPanel.setLayout(new BoxLayout(identityPanel, BoxLayout.Y_AXIS));
        identityPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(AppFonts.BODY_BOLD);
        nameLabel.setForeground(AppColors.TEXT_WHITE);

        statusLabel = new JLabel(currentStatus.toVietnamese());
        statusLabel.setFont(AppFonts.CAPTION);
        statusLabel.setForeground(AppColors.TEXT_MUTED);

        identityPanel.add(nameLabel);
        identityPanel.add(statusLabel);

        // ── Left panel: click để mở popup trạng thái ─────────────────
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);
        leftPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        leftPanel.add(userAvatar);
        leftPanel.add(identityPanel);

        MouseAdapter openPopup = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showStatusPopup(leftPanel); }
        };
        leftPanel.addMouseListener(openPopup);
        userAvatar.addMouseListener(openPopup);

        // ── Right controls: theme toggle + settings ───────────────────
        JPanel controlsWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        controlsWrapper.setOpaque(false);

        // Nút đổi theme — dùng AppIcons.sun/moon (Java2D, không ô vuông)
        boolean isDark = gui.theme.ThemeManager.get().isDark();
        Icon themeIcon = isDark ? AppIcons.sun(16) : AppIcons.moon(16);
        String themeTooltip = isDark ? "Chuyển sang giao diện Sáng" : "Chuyển sang giao diện Tối";

        IconButton themeToggle = new IconButton(themeIcon);
        themeToggle.setToolTipText(themeTooltip);
        themeToggle.addActionListener(e -> {
            gui.theme.Theme t = gui.theme.ThemeManager.get().toggle();
            gui.ClientApplication.applyThemeLive(t);
            boolean nowDark = (t == gui.theme.Theme.DARK);
            // Đổi icon sang sun (khi đang dark → có thể chuyển sang sáng) hoặc moon
            themeToggle.setIcon(nowDark ? AppIcons.sun(16) : AppIcons.moon(16));
            themeToggle.setToolTipText(nowDark
                    ? "Chuyển sang giao diện Sáng" : "Chuyển sang giao diện Tối");
        });
        controlsWrapper.add(themeToggle);

        // Nút cài đặt — GearIcon (Java2D)
        IconButton settingsBtn = new IconButton(AppIcons.gear(16), e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            gui.profile.UserSettingsDialog dialog = new gui.profile.UserSettingsDialog(owner, currentStatus, () -> {
                loadUserAvatar(userAvatar, username);
                if (onUserChanged != null) onUserChanged.run();
            });
            dialog.setVisible(true);
        });
        settingsBtn.setToolTipText("Cài đặt");
        controlsWrapper.add(settingsBtn);

        add(leftPanel, BorderLayout.WEST);
        add(controlsWrapper, BorderLayout.EAST);
    }

    // ──────────────────────────────────────────────────────────────────
    // Status dot — vẽ ĐÈ LÊN avatar bằng paint() override
    // ──────────────────────────────────────────────────────────────────

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (userAvatar == null) return;

        Point p = SwingUtilities.convertPoint(userAvatar.getParent(), userAvatar.getLocation(), this);
        int dotX = p.x + AVATAR_SIZE - DOT_SIZE + 1;
        int dotY = p.y + AVATAR_SIZE - DOT_SIZE + 1;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = getBackground() != null ? getBackground() : AppColors.BG_TERTIARY;
        int pad = 2;
        g2.setColor(bg);
        g2.fillOval(dotX - pad, dotY - pad, DOT_SIZE + pad * 2, DOT_SIZE + pad * 2);

        PresenceStatusIcon.paintStatus(g2, currentStatus,
                dotX + DOT_SIZE / 2f,
                dotY + DOT_SIZE / 2f,
                DOT_SIZE / 2f,
                bg);
        g2.dispose();
    }

    // ──────────────────────────────────────────────────────────────────
    // Popup chọn trạng thái
    // ──────────────────────────────────────────────────────────────────

    private void showStatusPopup(Component anchor) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(AppColors.BG_FLOATING);
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.SEPARATOR, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        JLabel header = new JLabel("TRẠNG THÁI HOẠT ĐỘNG");
        header.setFont(AppFonts.CAPTION_BOLD);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
        popup.add(header);
        popup.addSeparator();

        PresenceStatusIcon.Status[] options = {
            PresenceStatusIcon.Status.ONLINE,
            PresenceStatusIcon.Status.IDLE,
            PresenceStatusIcon.Status.AWAY,
            PresenceStatusIcon.Status.DO_NOT_DISTURB,
            PresenceStatusIcon.Status.INVISIBLE,
        };
        for (PresenceStatusIcon.Status st : options) {
            popup.add(buildStatusItem(st));
        }

        popup.show(anchor, 0, -popup.getPreferredSize().height - 4);
    }

    /**
     * JMenuItem: icon status dot (PresenceStatusIcon as Icon) ở trái
     *            + label tiếng Việt ở giữa
     *            + AppIcons.check (Java2D) ở phải nếu đang active.
     * Không dùng ký tự Unicode ✓ → không ô vuông.
     */
    private JMenuItem buildStatusItem(PresenceStatusIcon.Status st) {
        boolean active = (st == currentStatus);
        PresenceStatusIcon statusDotIcon = new PresenceStatusIcon(st, 14, AppColors.BG_FLOATING);

        JMenuItem item;
        if (active) {
            // Active: vẽ checkmark tick bên phải qua custom paintComponent
            item = buildCheckedItem(st, statusDotIcon, AppIcons.check(12));
        } else {
            item = new JMenuItem(st.toVietnamese(), statusDotIcon);
            item.setFont(AppFonts.BODY);
            item.setForeground(AppColors.TEXT_NORMAL);
            item.setBackground(AppColors.BG_FLOATING);
            item.setBorderPainted(false);
            item.setIconTextGap(8);
        }

        item.addActionListener(e -> applyStatus(st));
        return item;
    }


    /** JMenuItem tự paint dấu check ở bên phải khi active. */
    private JMenuItem buildCheckedItem(PresenceStatusIcon.Status st, Icon leftIcon, Icon checkIcon) {
        JMenuItem item = new JMenuItem(st.toVietnamese(), leftIcon) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Vẽ checkmark ở cạnh phải
                int margin = 8;
                int cx = getWidth() - checkIcon.getIconWidth() - margin;
                int cy = (getHeight() - checkIcon.getIconHeight()) / 2;
                checkIcon.paintIcon(this, g, cx, cy);
            }
        };
        item.setFont(AppFonts.BODY_BOLD);
        item.setForeground(AppColors.TEXT_WHITE);
        item.setBackground(AppColors.BG_FLOATING);
        item.setBorderPainted(false);
        item.setIconTextGap(8);
        item.addActionListener(e -> applyStatus(st));
        return item;
    }

    /** Cập nhật trạng thái local + gọi API async. */
    public void setStatusVisual(PresenceStatusIcon.Status st) {
        currentStatus = st;
        statusLabel.setText(st.toVietnamese());
        repaint();
    }

    private void applyStatus(PresenceStatusIcon.Status st) {
        setStatusVisual(st);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                presenceApi.updatePresenceStatus(st.name());
                return null;
            }
            @Override protected void done() {
                try { get(); }
                catch (Exception ex) {
                    System.err.println("[StatusFooter] " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────

    public void loadUserAvatar(AvatarBadge badge, String uname) {
        java.util.Map<String, Object> cachedProfile = network.UserProfileCache.get(uname);
        if (cachedProfile != null) {
            applyProfileToBadge(badge, cachedProfile);
        } else {
            badge.setLoading(true);
            new SwingWorker<java.util.Map<String, Object>, Void>() {
                @Override protected java.util.Map<String, Object> doInBackground() {
                    return new network.UserProfileApiClient().getProfile(uname);
                }
                @Override protected void done() {
                    try {
                        java.util.Map<String, Object> profile = get();
                        if (profile != null) {
                            applyProfileToBadge(badge, profile);
                        } else {
                            badge.setLoading(false);
                        }
                    } catch (Exception ignore) {
                        badge.setLoading(false);
                    }
                }
            }.execute();
        }
    }

    private void applyProfileToBadge(AvatarBadge badge, java.util.Map<String, Object> profile) {
        boolean hasUrl = false;
        if (profile.get("avatarUrl") != null) {
            String url = profile.get("avatarUrl").toString();
            if (!url.isBlank()) {
                hasUrl = true;
                if (!url.startsWith("http")) url = network.ApiConfig.GATEWAY_HTTP + url;
                badge.loadAvatarFromUrl(url);
            }
        }
        if (!hasUrl) {
            badge.setLoading(false);
        }
    }

    public void refreshAvatar() {
        loadUserAvatar(this.userAvatar, this.username);
    }
}