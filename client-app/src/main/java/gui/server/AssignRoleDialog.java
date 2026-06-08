package gui.server;

import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.RoleApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssignRoleDialog extends JDialog {

    private final long serverId;
    private final String username;
    private final RoleApiClient roleApi = new RoleApiClient();
    private final JPanel rolesPanel;
    private final List<JCheckBox> checkBoxes = new ArrayList<>();
    private final List<String> roleIds = new ArrayList<>();

    public AssignRoleDialog(Window owner, long serverId, String username) {
        super(owner, "Cấp Role cho " + username, ModalityType.APPLICATION_MODAL);
        this.serverId = serverId;
        this.username = username;
        setSize(350, 400);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Chọn Role cho " + username);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(AppColors.TEXT_WHITE);
        root.add(title, BorderLayout.NORTH);

        rolesPanel = new JPanel();
        rolesPanel.setLayout(new BoxLayout(rolesPanel, BoxLayout.Y_AXIS));
        rolesPanel.setBackground(AppColors.BG_SECONDARY);
        
        JScrollPane scrollPane = new JScrollPane(rolesPanel);
        scrollPane.setBorder(null);
        root.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);

        PrimaryButton saveBtn = new PrimaryButton("Lưu", e -> saveRoles());
        PrimaryButton cancelBtn = new PrimaryButton("Hủy", e -> dispose());
        cancelBtn.setBackground(AppColors.BG_SECONDARY);

        footer.add(saveBtn);
        footer.add(cancelBtn);
        root.add(footer, BorderLayout.SOUTH);

        gui.utils.UiKeys.onEnter(this, saveBtn::doClick); // Enter để Lưu
        setContentPane(root);
        loadRoles();
    }

    private void loadRoles() {
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return roleApi.getRoles(serverId);
            }
            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> roles = get();
                    rolesPanel.removeAll();
                    for (Map<String, Object> r : roles) {
                        String id = String.valueOf(r.get("id"));
                        String name = String.valueOf(r.get("name"));
                        String colorHex = String.valueOf(r.get("color"));
                        
                        JCheckBox cb = new JCheckBox(name);
                        cb.setForeground(AppColors.TEXT_NORMAL);
                        cb.setOpaque(false);
                        
                        try {
                            if (colorHex != null && colorHex.startsWith("#")) {
                                cb.setForeground(Color.decode(colorHex));
                            }
                        } catch (Exception ignore) {}

                        checkBoxes.add(cb);
                        roleIds.add(id);
                        rolesPanel.add(cb);
                        rolesPanel.add(Box.createVerticalStrut(5));
                    }
                    rolesPanel.revalidate();
                    rolesPanel.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AssignRoleDialog.this, "Lỗi tải Roles: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void saveRoles() {
        List<String> selectedIds = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                selectedIds.add(roleIds.get(i));
            }
        }
        
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                roleApi.assignRoles(serverId, username, selectedIds);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AssignRoleDialog.this, "Lỗi cấp quyền: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
