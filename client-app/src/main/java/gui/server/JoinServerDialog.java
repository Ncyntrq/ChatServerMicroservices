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

    public JoinServerDialog(Window owner, java.util.function.Consumer<Long> onSuccess) {
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

        JLabel subtitle = new JLabel("Nhập mã mời để tham gia");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(subtitle);
        root.add(Box.createVerticalStrut(20));

        FormField codeField = new FormField("MÃ MỜI", "Nhập invite code", false);
        codeField.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(codeField);
        root.add(Box.createVerticalStrut(15));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton joinBtn = new PrimaryButton("Tham gia Server", e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Vui lòng nhập mã mời");
                return;
            }
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Đang tham gia...");

            new SwingWorker<Long, Void>() {
                @Override
                protected Long doInBackground() {
                    return serverApi.joinServerByCode(code);
                }

                @Override
                protected void done() {
                    try {
                        long serverId = get();
                        if (onSuccess != null) onSuccess.accept(serverId);
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

        gui.utils.UiKeys.onEnter(this, joinBtn::doClick); // Enter để Tham gia Server
        setContentPane(root);
    }
}
