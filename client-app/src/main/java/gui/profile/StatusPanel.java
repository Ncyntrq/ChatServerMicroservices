package gui.profile;

import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.ApiException;
import network.UserProfileApiClient;

import javax.swing.*;
import java.awt.*;

/**
 * Panel đổi trạng thái: Online, Idle, Do Not Disturb, Invisible + custom text.
 */
public class StatusPanel extends JPanel {

    private static final String[] STATUS_OPTIONS = {"ONLINE", "IDLE", "DO_NOT_DISTURB", "INVISIBLE"};
    private static final String[] STATUS_LABELS = {"🟢 Trực tuyến", "🌙 Chờ", "⛔ Không làm phiền", "👻 Ẩn"};

    public StatusPanel(UserProfileApiClient profileApi) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AppColors.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // --- Title ---
        JLabel title = new JLabel("Trạng thái hoạt động");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(10));

        JLabel subtitle = new JLabel("Chọn trạng thái hiển thị cho người khác");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(subtitle);
        add(Box.createVerticalStrut(25));

        // --- Status Radio Buttons ---
        ButtonGroup group = new ButtonGroup();
        JRadioButton[] radios = new JRadioButton[STATUS_OPTIONS.length];
        for (int i = 0; i < STATUS_OPTIONS.length; i++) {
            radios[i] = new JRadioButton(STATUS_LABELS[i]);
            radios[i].setActionCommand(STATUS_OPTIONS[i]);
            radios[i].setFont(new Font("SansSerif", Font.PLAIN, 14));
            radios[i].setForeground(AppColors.TEXT_NORMAL);
            radios[i].setBackground(AppColors.BG_PRIMARY);
            radios[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            group.add(radios[i]);
            add(radios[i]);
            add(Box.createVerticalStrut(8));
        }
        radios[0].setSelected(true); // Default: ONLINE

        add(Box.createVerticalStrut(15));

        // --- Custom Status Text ---
        JLabel customLabel = new JLabel("TRẠNG THÁI TÙY CHỈNH");
        customLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        customLabel.setForeground(AppColors.TEXT_MUTED);
        customLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(customLabel);
        add(Box.createVerticalStrut(5));

        JTextField customField = new JTextField();
        customField.setBackground(AppColors.BG_TERTIARY);
        customField.setForeground(AppColors.TEXT_NORMAL);
        customField.setCaretColor(AppColors.TEXT_WHITE);
        customField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        customField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BG_HOVER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        customField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        customField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(customField);
        add(Box.createVerticalStrut(20));

        // --- Status Label ---
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Update Button ---
        PrimaryButton updateBtn = new PrimaryButton("Cập nhật trạng thái", e -> {
            String selected = group.getSelection().getActionCommand();
            String custom = customField.getText().trim();
            String statusText = custom.isEmpty() ? selected : custom;

            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Đang cập nhật...");

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    profileApi.updateStatus(statusText);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusLabel.setForeground(AppColors.SUCCESS);
                        statusLabel.setText("Đã cập nhật trạng thái: " + statusText);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() instanceof ApiException ? ex.getCause() : ex;
                        statusLabel.setForeground(AppColors.DANGER);
                        statusLabel.setText("Lỗi: " + cause.getMessage());
                    }
                }
            }.execute();
        });
        updateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        gui.utils.UiKeys.onEnter(customField, updateBtn::doClick); // Enter ở ô tùy chỉnh → Cập nhật
        add(updateBtn);
        add(Box.createVerticalStrut(10));
        add(statusLabel);
    }
}
