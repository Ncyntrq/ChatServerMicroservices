package gui.server;

import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.ApiException;
import network.ServerApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Dialog tổng hợp quản lý server: xem info, sửa, xóa, tạo invite, rời server.
 * Load chi tiết từ GET /api/servers/{id}.
 */
public class ServerSettingsDialog extends JDialog {

    private final ServerApiClient serverApi = new ServerApiClient();
    private final long serverId;
    private final Runnable onChange;
    private final JLabel nameValue = new JLabel("...");
    private final JLabel descValue = new JLabel("...");
    private final JLabel statusLabel = new JLabel(" ");
    private JLabel titleLabel;
    private String loadedName;
    private String loadedDesc;
    private String loadedIconUrl;

    public ServerSettingsDialog(Window owner, long serverId, Runnable onChange) {
        super(owner, "Cài đặt Server", ModalityType.APPLICATION_MODAL);
        this.serverId = serverId;
        this.onChange = onChange;
        setSize(460, 460);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JLabel title = new JLabel("Cài đặt Server");
        this.titleLabel = title; // Store reference to update later
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(18));

        root.add(infoRow("Tên:", nameValue));
        root.add(Box.createVerticalStrut(8));
        root.add(infoRow("Mô tả:", descValue));
        root.add(Box.createVerticalStrut(20));

        PrimaryButton editBtn = new PrimaryButton("Sửa thông tin", e ->
                new EditServerDialog(this, serverId, loadedName, loadedDesc, loadedIconUrl, () -> {
                    if (onChange != null) onChange.run();
                    loadDetails();
                }).setVisible(true));
        editBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(editBtn);
        root.add(Box.createVerticalStrut(8));

        PrimaryButton manageRoleBtn = new PrimaryButton("Quản lý Role", e ->
                new RoleManagementDialog(this, serverId).setVisible(true));
        manageRoleBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(manageRoleBtn);
        root.add(Box.createVerticalStrut(8));

        PrimaryButton inviteBtn = new PrimaryButton("Tạo Invite Code", e -> createInvite());
        inviteBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(inviteBtn);
        root.add(Box.createVerticalStrut(8));

        PrimaryButton leaveBtn = new PrimaryButton("Rời Server", e -> leaveServer());
        leaveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(leaveBtn);
        root.add(Box.createVerticalStrut(8));

        PrimaryButton deleteBtn = new PrimaryButton("Xóa Server", e -> deleteServer());
        deleteBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(deleteBtn);
        root.add(Box.createVerticalStrut(12));

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(statusLabel);

        setContentPane(root);
        loadDetails();
    }

    private JPanel infoRow(String label, JLabel value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(AppColors.TEXT_MUTED);
        value.setFont(new Font("SansSerif", Font.PLAIN, 13));
        value.setForeground(AppColors.TEXT_NORMAL);
        row.add(l);
        row.add(value);
        return row;
    }

    private void loadDetails() {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                return serverApi.getServerDetails(serverId);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> response = get();
                    Map<String, Object> details = (Map<String, Object>) response.get("server");
                    if (details == null) details = response; // Fallback in case API changes

                    loadedName = str(details.get("name"));
                    loadedDesc = str(details.get("description"));
                    loadedIconUrl = str(details.get("icon"));
                    nameValue.setText(loadedName != null ? loadedName : "(không tên)");
                    descValue.setText(loadedDesc != null && !loadedDesc.isBlank() ? loadedDesc : "(không có mô tả)");
                    if (titleLabel != null) {
                        titleLabel.setText("Cài đặt: " + (loadedName != null ? loadedName : ""));
                    }
                } catch (Exception ex) {
                    statusLabel.setForeground(AppColors.WARNING);
                    statusLabel.setText("Không tải được chi tiết server");
                }
            }
        }.execute();
    }

    private void createInvite() {
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setText("Đang tạo mã mời...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return serverApi.createInviteCode(serverId);
            }

            @Override
            protected void done() {
                try {
                    String code = get();
                    statusLabel.setText(" ");
                    new InviteCodeDialog(ServerSettingsDialog.this, code).setVisible(true);
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    private void leaveServer() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn chắc chắn muốn rời server này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setText("Đang rời server...");
        if (onChange != null) onChange.run(); // Broadcast trước khi rời
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                serverApi.leaveServer(serverId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (onChange != null) onChange.run();
                    dispose();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    private void deleteServer() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa server này vĩnh viễn? Hành động không thể hoàn tác.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setText("Đang xóa...");
        if (onChange != null) onChange.run(); // Broadcast trước khi xóa để gửi được WS
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                serverApi.deleteServer(serverId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (onChange != null) onChange.run();
                    dispose();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    private void showError(Exception ex) {
        Throwable cause = ex.getCause() instanceof ApiException ? ex.getCause() : ex;
        statusLabel.setForeground(AppColors.DANGER);
        statusLabel.setText("Lỗi: " + cause.getMessage());
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
