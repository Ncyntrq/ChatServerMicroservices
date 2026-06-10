package gui.server.settings;

import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.RoleApiClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ServerRolesPanel extends JPanel {

    private final RoleApiClient roleApi = new RoleApiClient();
    private final long serverId;

    private final DefaultTableModel rolesTableModel;
    private final JTable roleTable;
    private List<Map<String, Object>> currentRoles;
    private final JTextField roleNameField;
    private final JColorChooser colorChooser;
    private final PrimaryButton saveRoleBtn;
    private final PrimaryButton deleteRoleBtn;
    private final JLabel roleFormTitle;
    private String selectedRoleId = null;
    private boolean isCreatingNewRole = false;

    public ServerRolesPanel(long serverId) {
        this.serverId = serverId;
        setLayout(new BorderLayout(15, 15));
        setBackground(AppColors.BG_PRIMARY);

        JLabel title = new JLabel("Roles");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        add(title, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(250, 0));

        rolesTableModel = new DefaultTableModel(new String[]{"Role Name", "Color"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        roleTable = new JTable(rolesTableModel);
        roleTable.setRowHeight(30);
        roleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleTable.getSelectionModel().addListSelectionListener(e -> onRoleSelected());
        JScrollPane scrollPane = new JScrollPane(roleTable);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        PrimaryButton createBtn = new PrimaryButton("+ Create Role", e -> prepareCreateNewRole());
        leftPanel.add(createBtn, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(AppColors.BG_SECONDARY);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        roleFormTitle = new JLabel("Edit Role");
        roleFormTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        roleFormTitle.setForeground(AppColors.TEXT_WHITE);
        rightPanel.add(roleFormTitle);
        rightPanel.add(Box.createVerticalStrut(15));

        JLabel nameLbl = new JLabel("ROLE NAME");
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        nameLbl.setForeground(AppColors.TEXT_MUTED);
        roleNameField = new JTextField(20);
        
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        namePanel.setOpaque(false);
        namePanel.add(roleNameField);
        namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        rightPanel.add(nameLbl);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(namePanel);
        rightPanel.add(Box.createVerticalStrut(15));

        JLabel colorLbl = new JLabel("ROLE COLOR");
        colorLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        colorLbl.setForeground(AppColors.TEXT_MUTED);
        rightPanel.add(colorLbl);
        rightPanel.add(Box.createVerticalStrut(5));
        
        colorChooser = new JColorChooser(Color.WHITE);
        colorChooser.setPreviewPanel(new JPanel());
        JPanel colorPanel = new JPanel(new BorderLayout());
        colorPanel.add(colorChooser, BorderLayout.CENTER);
        colorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
        rightPanel.add(colorPanel);
        rightPanel.add(Box.createVerticalStrut(25));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setOpaque(false);
        saveRoleBtn = new PrimaryButton("Save Changes", e -> saveRole());
        saveRoleBtn.setEnabled(false);
        deleteRoleBtn = new PrimaryButton("Delete Role", e -> deleteSelectedRole());
        deleteRoleBtn.setBackground(AppColors.DANGER);
        deleteRoleBtn.setEnabled(false);
        actionPanel.add(saveRoleBtn);
        actionPanel.add(deleteRoleBtn);
        rightPanel.add(actionPanel);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerSize(0);
        splitPane.setBorder(null);
        splitPane.setDividerLocation(250);
        add(splitPane, BorderLayout.CENTER);

        loadRoles(null);
    }

    public void reloadRoles() {
        loadRoles(selectedRoleId);
    }

    private void loadRoles(String selectRoleId) {
        rolesTableModel.setRowCount(0);
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override protected List<Map<String, Object>> doInBackground() {
                return roleApi.getRoles(serverId);
            }
            @Override protected void done() {
                try {
                    currentRoles = get();
                    int selectIdx = -1;
                    for (int i = 0; i < currentRoles.size(); i++) {
                        Map<String, Object> r = currentRoles.get(i);
                        String name = String.valueOf(r.get("roleName"));
                        if (name.equals("null")) name = String.valueOf(r.get("name"));

                        String color = String.valueOf(r.get("color"));
                        if (color == null || color.equals("null")) color = "#FFFFFF";
                        rolesTableModel.addRow(new Object[]{name, color});

                        if (selectRoleId != null && selectRoleId.equals(String.valueOf(r.get("id")))) {
                            selectIdx = i;
                        }
                    }
                    rolesTableModel.fireTableDataChanged();

                    if (selectIdx >= 0) {
                        roleTable.setRowSelectionInterval(selectIdx, selectIdx);
                        roleTable.scrollRectToVisible(roleTable.getCellRect(selectIdx, 0, true));
                    } else {
                        prepareCreateNewRole();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ServerRolesPanel.this, "Error loading roles: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onRoleSelected() {
        int idx = roleTable.getSelectedRow();
        if (idx >= 0 && currentRoles != null && idx < currentRoles.size()) {
            isCreatingNewRole = false;
            Map<String, Object> roleData = currentRoles.get(idx);
            selectedRoleId = String.valueOf(roleData.get("id"));
            
            String name = String.valueOf(roleData.get("roleName"));
            if (name.equals("null")) name = String.valueOf(roleData.get("name"));
            
            String color = String.valueOf(roleData.get("color"));
            boolean isDefault = Boolean.TRUE.equals(roleData.get("isDefault"));
            
            roleFormTitle.setText("Edit Role");
            saveRoleBtn.setText("Save Changes");
            roleNameField.setText(name);
            roleNameField.setEnabled(!isDefault);
            colorChooser.setEnabled(true);
            saveRoleBtn.setEnabled(true);
            
            if (color == null || color.equals("null")) color = "#FFFFFF";
            try { colorChooser.setColor(Color.decode(color)); } catch(Exception ignore) {}
            
            deleteRoleBtn.setVisible(!isDefault);
            deleteRoleBtn.setEnabled(!isDefault);
        }
    }

    private void prepareCreateNewRole() {
        roleTable.clearSelection();
        isCreatingNewRole = true;
        selectedRoleId = null;
        roleFormTitle.setText("Create New Role");
        saveRoleBtn.setText("Create Role");
        roleNameField.setText("");
        roleNameField.setEnabled(true);
        colorChooser.setEnabled(true);
        saveRoleBtn.setEnabled(true);
        colorChooser.setColor(Color.WHITE);
        deleteRoleBtn.setVisible(false);
    }

    private void saveRole() {
        String name = roleNameField.getText().trim();
        if (name.isEmpty()) return;
        
        Color c = colorChooser.getColor();
        String hexColor = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        
        saveRoleBtn.setEnabled(false);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                Map<String, Object> result;
                if (isCreatingNewRole) {
                    result = roleApi.createRole(serverId, name, hexColor, "SEND_MESSAGE,READ_MESSAGE");
                } else if (selectedRoleId != null) {
                    result = roleApi.updateRole(selectedRoleId, name, hexColor, "SEND_MESSAGE,READ_MESSAGE");
                } else {
                    return null;
                }
                return result != null ? String.valueOf(result.get("id")) : null;
            }
            @Override protected void done() {
                saveRoleBtn.setEnabled(true);
                try {
                    String savedId = get();
                    loadRoles(savedId);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ServerRolesPanel.this, "Error saving role: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void deleteSelectedRole() {
        if (selectedRoleId == null) return;
        int conf = JOptionPane.showConfirmDialog(this, "Delete this role?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        deleteRoleBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                roleApi.deleteRole(selectedRoleId);
                return null;
            }
            @Override protected void done() {
                deleteRoleBtn.setEnabled(true);
                try {
                    get();
                    loadRoles(null);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ServerRolesPanel.this, "Error deleting role: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
