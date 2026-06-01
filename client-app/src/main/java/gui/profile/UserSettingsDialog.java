package gui.profile;

import gui.theme.AppColors;
import network.SessionManager;
import network.UserProfileApiClient;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog modal chứa các tab settings cho user:
 * - Hồ sơ (Profile): displayName, bio, avatar
 * - Bảo mật (Security): đổi mật khẩu
 * - Trạng thái (Status): online/idle/dnd/invisible
 */
public class UserSettingsDialog extends JDialog {

    public UserSettingsDialog(Window owner, Runnable onProfileChanged) {
        super(owner, "Cài đặt người dùng", ModalityType.APPLICATION_MODAL);
        setSize(600, 500);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(AppColors.BG_PRIMARY);

        String username = SessionManager.get().getUsername();
        UserProfileApiClient profileApi = new UserProfileApiClient();

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(AppColors.BG_SECONDARY);
        tabs.setForeground(AppColors.TEXT_NORMAL);

        tabs.addTab("Hồ sơ", new ProfileEditPanel(username, profileApi, onProfileChanged));
        tabs.addTab("Bảo mật", new AccountSecurityPanel());
        tabs.addTab("Trạng thái", new StatusPanel(profileApi));

        add(tabs);
    }
}
