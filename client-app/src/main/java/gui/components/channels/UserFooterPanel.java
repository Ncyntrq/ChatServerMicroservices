package gui.components.channels;

import gui.components.AvatarBadge;
import gui.components.chat.IconButton;
import gui.theme.AppColors;
import javax.swing.*;
import java.awt.*;

public class UserFooterPanel extends JPanel {

    public UserFooterPanel(String username) {
        setLayout(new BorderLayout(8, 0));
        setBackground(AppColors.BG_SECONDARY); // Dark mid-tone gray
        setPreferredSize(new Dimension(240, 52));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // Left Side: Identity Layout
        JPanel identityWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        identityWrapper.setOpaque(false);

        // Reusing your clean fallback Avatar component
        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
        AvatarBadge userAvatar = new AvatarBadge(initial);
        userAvatar.setPreferredSize(new Dimension(36, 36));

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
        controlsWrapper.add(new IconButton("🎙️")); // Mute mic
        controlsWrapper.add(new IconButton("🎧")); // Deafen audio
        controlsWrapper.add(new IconButton("⚙️", e -> {
            System.out.println("Opening Modal Profile & Settings..."); // Core requirement hook
        }));

        add(identityWrapper, BorderLayout.WEST);
        add(controlsWrapper, BorderLayout.EAST);
    }
}