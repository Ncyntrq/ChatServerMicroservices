package gui.server;

import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.ApiException;
import network.ServerApiClient;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog tham gia server bằng serverId + invite code.
 * POST /api/servers/{id}/join?code=xxx
 */
public class JoinServerDialog extends JDialog {

    public JoinServerDialog(Window owner, Runnable onSuccess) {
        super(owner, "Tham Gia Server", ModalityType.APPLICATION_MODAL);
        setSize(440, 320);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        ServerApiClient serverApi = new ServerApiClient();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JLabel title = new JLabel("Tham Gia Server");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("Nhập ID server và mã mời để tham gia");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(subtitle);
        root.add(Box.createVerticalStrut(20));

        FormField serverIdField = new FormField("SERVER ID", "Nhập ID server", false);
        serverIdField.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(serverIdField);
        root.add(Box.createVerticalStrut(15));

        FormField codeField = new FormField("MÃ MỜI", "Nhập invite code", false);
        codeField.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(codeField);
        root.add(Box.createVerticalStrut(15));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton joinBtn = new PrimaryButton("Tham gia Server", e -> {
            String idText = serverIdField.getText().trim();
            String code = codeField.getText().trim();
            if (idText.isEmpty() || code.isEmpty()) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Vui lòng nhập đầy đủ ID và mã mời");
                return;
            }
            long serverId;
            try {
                serverId = Long.parseLong(idText);
            } catch (NumberFormatException ex) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Server ID phải là số");
                return;
            }
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Đang tham gia...");

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    serverApi.joinServer(serverId, code);
                    return null;
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
        joinBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(joinBtn);
        root.add(Box.createVerticalStrut(10));
        root.add(statusLabel);

        setContentPane(root);
    }
}
