package gui.profile;

import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.ApiException;
import network.AuthApiClient;

import javax.swing.*;
import java.awt.*;

/**
 * Panel đổi mật khẩu (bảo mật tài khoản).
 */
public class AccountSecurityPanel extends JPanel {

    public AccountSecurityPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AppColors.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // --- Title ---
        JLabel title = new JLabel("Bảo mật tài khoản");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(10));

        JLabel subtitle = new JLabel("Đổi mật khẩu đăng nhập");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(subtitle);
        add(Box.createVerticalStrut(25));

        // --- Fields ---
        FormField oldPassField = new FormField("MẬT KHẨU CŨ", "Nhập mật khẩu hiện tại", true);
        oldPassField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(oldPassField);
        add(Box.createVerticalStrut(15));

        FormField newPassField = new FormField("MẬT KHẨU MỚI", "Nhập mật khẩu mới", true);
        newPassField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(newPassField);
        add(Box.createVerticalStrut(15));

        FormField confirmField = new FormField("XÁC NHẬN MẬT KHẨU MỚI", "Nhập lại mật khẩu mới", true);
        confirmField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(confirmField);
        add(Box.createVerticalStrut(20));

        // --- Status ---
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Change Password Button ---
        PrimaryButton changeBtn = new PrimaryButton("Đổi mật khẩu", e -> {
            String oldPass = oldPassField.getText();
            String newPass = newPassField.getText();
            String confirm = confirmField.getText();

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Vui lòng nhập đầy đủ thông tin");
                return;
            }
            if (!newPass.equals(confirm)) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Mật khẩu mới không khớp");
                return;
            }

            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Đang xử lý...");

            AuthApiClient authClient = new AuthApiClient();
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return authClient.changePassword(oldPass, newPass);
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusLabel.setForeground(AppColors.SUCCESS);
                        statusLabel.setText("Đổi mật khẩu thành công!");
                        oldPassField.setText("");
                        newPassField.setText("");
                        confirmField.setText("");
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() instanceof ApiException ? ex.getCause() : ex;
                        statusLabel.setForeground(AppColors.DANGER);
                        statusLabel.setText("Lỗi: " + cause.getMessage());
                    }
                }
            }.execute();
        });
        changeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(changeBtn);
        add(Box.createVerticalStrut(10));
        add(statusLabel);

        // Enter ở các ô mật khẩu → Đổi mật khẩu
        oldPassField.onEnter(changeBtn::doClick);
        newPassField.onEnter(changeBtn::doClick);
        confirmField.onEnter(changeBtn::doClick);
    }
}
