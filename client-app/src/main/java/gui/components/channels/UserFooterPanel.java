package gui.components.channels;

import gui.components.AvatarBadge;
import gui.components.chat.IconButton;
import gui.profile.UserSettingsDialog;
import gui.theme.AppColors;
import javax.swing.*;
import java.awt.*;

public class UserFooterPanel extends JPanel {

    private AvatarBadge userAvatar;
    private String username;

    public UserFooterPanel(String username, Runnable onUserChanged) {
        this.username = username;
        setLayout(new BorderLayout(8, 0));
        setBackground(AppColors.BG_SECONDARY); // Dark mid-tone gray
        setPreferredSize(new Dimension(240, 52));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // Left Side: Identity Layout
        JPanel identityWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        identityWrapper.setOpaque(false);

        // Reusing your clean fallback Avatar component
        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
        this.userAvatar = new AvatarBadge(initial);
        userAvatar.setPreferredSize(new Dimension(36, 36));

        // Asynchronously load avatar if available
        loadUserAvatar(userAvatar, username);

        // Name Details
        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setForeground(AppColors.TEXT_WHITE);

        identityWrapper.add(userAvatar);
        identityWrapper.add(nameLabel);

        // Right Side: Quick System Controls
        JPanel controlsWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        controlsWrapper.setOpaque(false);

        // Reusing your standalone IconButton component with custom offsets
        controlsWrapper.add(new IconButton("⚙", e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            UserSettingsDialog dialog = new UserSettingsDialog(owner, () -> {
                loadUserAvatar(userAvatar, username);
                if (onUserChanged != null) onUserChanged.run();
            });
            dialog.setVisible(true);
        }));

        add(identityWrapper, BorderLayout.WEST);
        add(controlsWrapper, BorderLayout.EAST);
    }

    public void loadUserAvatar(AvatarBadge userAvatar, String username) {
        new SwingWorker<java.util.Map<String, Object>, Void>() {
            @Override
            protected java.util.Map<String, Object> doInBackground() {
                return new network.UserProfileApiClient().getProfile(username);
            }

            @Override
            protected void done() {
                try {
                    java.util.Map<String, Object> profile = get();
                    if (profile != null && profile.get("avatarUrl") != null) {
                        String url = profile.get("avatarUrl").toString();
                        if (!url.startsWith("http")) {
                            url = network.ApiConfig.GATEWAY_HTTP + url;
                        }
                        userAvatar.loadAvatarFromUrl(url);
                    }
                } catch (Exception ignore) {}
            }
        }.execute();
    }

    public void refreshAvatar() {
        loadUserAvatar(this.userAvatar, this.username);
    }
}