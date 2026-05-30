package gui.server;

import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.ApiException;
import network.ServerApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Dialog tạo server mới: nhập tên + mô tả → POST /api/servers.
 * onSuccess: callback để caller refresh danh sách server.
 */
public class CreateServerDialog extends JDialog {

    public CreateServerDialog(Window owner, Runnable onSuccess) {
        super(owner, "Tạo Server Mới", ModalityType.APPLICATION_MODAL);
        setSize(440, 360);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        ServerApiClient serverApi = new ServerApiClient();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JLabel title = new JLabel("Tạo Server Của Bạn");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(20));

        FormField nameField = new FormField("TÊN SERVER", "Nhập tên server", false);
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(nameField);
        root.add(Box.createVerticalStrut(15));

        JLabel descLabel = new JLabel("MÔ TẢ (TÙY CHỌN)");
        descLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        descLabel.setForeground(AppColors.TEXT_MUTED);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(descLabel);
        root.add(Box.createVerticalStrut(5));

        JTextArea descArea = new JTextArea(3, 30);
        descArea.setBackground(AppColors.BG_TERTIARY);
        descArea.setForeground(AppColors.TEXT_NORMAL);
        descArea.setCaretColor(AppColors.TEXT_WHITE);
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        root.add(descScroll);
        root.add(Box.createVerticalStrut(15));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton createBtn = new PrimaryButton("Tạo Server", e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Vui lòng nhập tên server");
                return;
            }
            String desc = descArea.getText().trim();
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Đang tạo...");

            new SwingWorker<Map<String, Object>, Void>() {
                @Override
                protected Map<String, Object> doInBackground() {
                    return serverApi.createServer(name, desc.isEmpty() ? null : desc);
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

        setContentPane(root);
    }
}
