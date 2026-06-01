package gui.profile;

import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.ApiException;
import network.UserProfileApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Panel chỉnh sửa hồ sơ: displayName, bio, avatar upload.
 */
public class ProfileEditPanel extends JPanel {

    private final FormField displayNameField;
    private final JTextArea bioArea;
    private final JLabel statusLabel;

    public ProfileEditPanel(String username, UserProfileApiClient profileApi) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AppColors.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Khởi tạo statusLabel sớm vì các lambda phía dưới tham chiếu tới nó
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Title ---
        JLabel title = new JLabel("Chỉnh sửa hồ sơ");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(20));

        // --- Display Name ---
        displayNameField = new FormField("TÊN HIỂN THỊ", "Nhập tên hiển thị", false);
        displayNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(displayNameField);
        add(Box.createVerticalStrut(15));

        // --- Bio ---
        JLabel bioLabel = new JLabel("GIỚI THIỆU BẢN THÂN");
        bioLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        bioLabel.setForeground(AppColors.TEXT_MUTED);
        bioLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(bioLabel);
        add(Box.createVerticalStrut(5));

        bioArea = new JTextArea(4, 30);
        bioArea.setBackground(AppColors.BG_TERTIARY);
        bioArea.setForeground(AppColors.TEXT_NORMAL);
        bioArea.setCaretColor(AppColors.TEXT_WHITE);
        bioArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        bioArea.setLineWrap(true);
        bioArea.setWrapStyleWord(true);
        bioArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane bioScroll = new JScrollPane(bioArea);
        bioScroll.setBorder(BorderFactory.createLineBorder(AppColors.BG_HOVER));
        bioScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        bioScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        add(bioScroll);
        add(Box.createVerticalStrut(20));

        // --- Upload Avatar Button ---
        PrimaryButton avatarBtn = new PrimaryButton("Upload Avatar", e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Ảnh (JPEG, PNG)", "jpg", "jpeg", "png"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.io.File selected = fc.getSelectedFile();
                statusLabel.setForeground(AppColors.TEXT_MUTED);
                statusLabel.setText("Đang upload avatar...");

                new SwingWorker<Map<String, Object>, Void>() {
                    @Override
                    protected Map<String, Object> doInBackground() {
                        return profileApi.uploadAvatar(selected);
                    }

                    @Override
                    protected void done() {
                        try {
                            Map<String, Object> profile = get();
                            Object avatarUrl = profile.get("avatarUrl");
                            statusLabel.setForeground(AppColors.SUCCESS);
                            statusLabel.setText("Đã upload avatar"
                                    + (avatarUrl != null ? ": " + avatarUrl : "!"));
                        } catch (Exception ex) {
                            Throwable cause = ex.getCause() instanceof ApiException ? ex.getCause() : ex;
                            statusLabel.setForeground(AppColors.DANGER);
                            statusLabel.setText("Lỗi: " + cause.getMessage());
                        }
                    }
                }.execute();
            }
        });
        avatarBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(avatarBtn);
        add(Box.createVerticalStrut(15));

        // --- Save Button ---
        PrimaryButton saveBtn = new PrimaryButton("Lưu thay đổi", e -> {
            String dn = displayNameField.getText().trim();
            String bio = bioArea.getText().trim();
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Đang lưu...");

            new SwingWorker<Map<String, Object>, Void>() {
                @Override
                protected Map<String, Object> doInBackground() {
                    return profileApi.updateProfile(dn.isEmpty() ? null : dn, bio.isEmpty() ? null : bio);
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusLabel.setForeground(AppColors.SUCCESS);
                        statusLabel.setText("Đã lưu thành công!");
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() instanceof ApiException ? ex.getCause() : ex;
                        statusLabel.setForeground(AppColors.DANGER);
                        statusLabel.setText("Lỗi: " + cause.getMessage());
                    }
                }
            }.execute();
        });
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(saveBtn);
        add(Box.createVerticalStrut(10));

        // --- Status Label ---
        add(statusLabel);

        // --- Load current profile ---
        loadProfile(username, profileApi);
    }

    private void loadProfile(String username, UserProfileApiClient profileApi) {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                return profileApi.getProfile(username);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> profile = get();
                    Object dn = profile.get("displayName");
                    Object bio = profile.get("bio");
                    if (dn != null) displayNameField.setText(dn.toString());
                    if (bio != null) bioArea.setText(bio.toString());
                } catch (Exception ex) {
                    statusLabel.setForeground(AppColors.WARNING);
                    statusLabel.setText("Không tải được hồ sơ — có thể chưa tạo");
                }
            }
        }.execute();
    }
}
