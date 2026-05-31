package gui.channel;

import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.ApiException;
import network.ChannelApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Dialog sửa channel (đổi tên, topic) → PUT /api/channels/{id}.
 */
public class EditChannelDialog extends JDialog {

    public EditChannelDialog(Window owner, long channelId, String currentName,
                             String currentTopic, Runnable onSuccess) {
        super(owner, "Sửa Channel", ModalityType.APPLICATION_MODAL);
        setSize(420, 340);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        ChannelApiClient channelApi = new ChannelApiClient();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JLabel title = new JLabel("Chỉnh sửa Channel");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(20));

        FormField nameField = new FormField("TÊN CHANNEL", "Nhập tên channel", false);
        if (currentName != null) nameField.setText(currentName);
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(nameField);
        root.add(Box.createVerticalStrut(15));

        FormField topicField = new FormField("CHỦ ĐỀ (TOPIC)", "Mô tả ngắn về channel", false);
        if (currentTopic != null) topicField.setText(currentTopic);
        topicField.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(topicField);
        root.add(Box.createVerticalStrut(18));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton saveBtn = new PrimaryButton("Lưu", e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Tên channel không được để trống");
                return;
            }
            String topic = topicField.getText().trim();
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Đang lưu...");

            new SwingWorker<Map<String, Object>, Void>() {
                @Override
                protected Map<String, Object> doInBackground() {
                    return channelApi.updateChannel(channelId, name, topic.isEmpty() ? null : topic);
                }

                @Override
                protected void done() {
                    try {
                        get();
                        if (onSuccess != null) onSuccess.run();
                        dispose();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() instanceof ApiException ? ex.getCause() : ex;
                        statusLabel.setForeground(AppColors.DANGER);
                        statusLabel.setText("Lỗi: " + cause.getMessage());
                    }
                }
            }.execute();
        });
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(saveBtn);
        root.add(Box.createVerticalStrut(10));
        root.add(statusLabel);

        setContentPane(root);
    }
}
