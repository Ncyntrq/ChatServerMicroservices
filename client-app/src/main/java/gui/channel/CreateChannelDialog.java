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
 * Dialog tạo channel mới trong server đang chọn.
 * POST /api/channels  body {serverId, name, type}.
 */
public class CreateChannelDialog extends JDialog {

    public CreateChannelDialog(Window owner, long serverId, Runnable onSuccess) {
        super(owner, "Tạo Channel", ModalityType.APPLICATION_MODAL);
        setSize(420, 320);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        ChannelApiClient channelApi = new ChannelApiClient();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JLabel title = new JLabel("Tạo Channel Mới");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(20));

        FormField nameField = new FormField("TÊN CHANNEL", "vd: thao-luan-chung", false);
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(nameField);
        root.add(Box.createVerticalStrut(15));

        JLabel typeLabel = new JLabel("LOẠI CHANNEL");
        typeLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        typeLabel.setForeground(AppColors.TEXT_MUTED);
        typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(typeLabel);
        root.add(Box.createVerticalStrut(5));

        JComboBox<String> typeBox = new JComboBox<>(new String[]{"TEXT", "VOICE"});
        typeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        typeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(typeBox);
        root.add(Box.createVerticalStrut(18));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton createBtn = new PrimaryButton("Tạo Channel", e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Vui lòng nhập tên channel");
                return;
            }
            String type = (String) typeBox.getSelectedItem();
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Đang tạo...");

            new SwingWorker<Map<String, Object>, Void>() {
                @Override
                protected Map<String, Object> doInBackground() {
                    return channelApi.createChannel(serverId, name, type);
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
        createBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(createBtn);
        root.add(Box.createVerticalStrut(10));
        root.add(statusLabel);

        gui.utils.UiKeys.onEnter(this, createBtn::doClick); // Enter để Tạo Channel
        setContentPane(root);
    }
}
