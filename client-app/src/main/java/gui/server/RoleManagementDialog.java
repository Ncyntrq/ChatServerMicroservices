package gui.server;

import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.RoleApiClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class RoleManagementDialog extends JDialog {

    private final long serverId;
    private final RoleApiClient roleApi = new RoleApiClient();
    private final DefaultTableModel tableModel;
    private final JTable roleTable;
    private List<Map<String, Object>> currentRoles;

    // Right panel components
    private final JPanel rightPanel;
    private final JLabel formTitle;
    private final JTextField nameField;
    private final JColorChooser colorChooser;
    private final PrimaryButton saveBtn;
    private final PrimaryButton deleteBtn;
    private String selectedRoleId = null;
    private boolean isCreatingNew = false;

    public RoleManagementDialog(Window owner, long serverId) {
        super(owner, "Quản lý Role Server", ModalityType.APPLICATION_MODAL);
        this.serverId = serverId;
        setSize(950, 650);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JLabel title = new JLabel("Danh sách Role");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(AppColors.TEXT_WHITE);
        root.add(title, BorderLayout.NORTH);

        // --- LEFT PANEL: Table & Add Button ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(250, 0));

        String[] columns = {"Tên Role", "Màu sắc"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        roleTable = new JTable(tableModel);
        roleTable.setRowHeight(30);
        roleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleTable.getSelectionModel().addListSelectionListener(e -> onRowSelected());
        JScrollPane scrollPane = new JScrollPane(roleTable);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        PrimaryButton createBtn = new PrimaryButton("+ Thêm Role Mới", e -> prepareCreateNew());
        leftPanel.add(createBtn, BorderLayout.SOUTH);

        // --- RIGHT PANEL: Edit Form ---
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(AppColors.BG_SECONDARY);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        formTitle = new JLabel("Chỉnh sửa Role");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        formTitle.setForeground(AppColors.TEXT_WHITE);
        rightPanel.add(formTitle);
        rightPanel.add(Box.createVerticalStrut(15));

        // Name field
        JLabel nameLbl = new JLabel("Tên Role:");
        nameLbl.setForeground(AppColors.TEXT_MUTED);
        nameField = new JTextField(20);
        
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        namePanel.setOpaque(false);
        namePanel.add(nameField);
        namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        rightPanel.add(nameLbl);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(namePanel);
        rightPanel.add(Box.createVerticalStrut(15));

        // Inline Color Chooser
        JLabel colorLbl = new JLabel("Chọn Màu Sắc Trực Quan:");
        colorLbl.setForeground(AppColors.TEXT_MUTED);
        rightPanel.add(colorLbl);
        rightPanel.add(Box.createVerticalStrut(5));
        
        colorChooser = new JColorChooser(Color.WHITE);
        colorChooser.setPreviewPanel(new JPanel()); // hide default preview
        colorChooser.setEnabled(false);
        // We have to disable its child components if we want true disable effect, but let's just leave it visually
        JPanel colorPanel = new JPanel(new BorderLayout());
        colorPanel.add(colorChooser, BorderLayout.CENTER);
        colorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
        rightPanel.add(colorPanel);
        rightPanel.add(Box.createVerticalStrut(25));

        // Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setOpaque(false);
        saveBtn = new PrimaryButton("Lưu Thay Đổi", e -> saveRole());
        saveBtn.setEnabled(false);
        deleteBtn = new PrimaryButton("Xóa Role", e -> deleteSelectedRole());
        deleteBtn.setBackground(AppColors.DANGER);
        deleteBtn.setEnabled(false);
        actionPanel.add(saveBtn);
        actionPanel.add(deleteBtn);
        rightPanel.add(actionPanel);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerSize(0);
        splitPane.setBorder(null);
        splitPane.setDividerLocation(250);
        root.add(splitPane, BorderLayout.CENTER);

        // --- FOOTER ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        PrimaryButton closeBtn = new PrimaryButton("Đóng", e -> dispose());
        closeBtn.setBackground(AppColors.BG_SECONDARY);
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        gui.utils.UiKeys.onEnter(nameField, this::saveRole); // Enter trong ô tên → Lưu/Thêm role
        setContentPane(root);
        loadRoles();
    }

    private void loadRoles() {
        loadRoles(null);
    }

    /** Tải lại danh sách role; nếu selectRoleId != null thì chọn + cuộn tới role đó sau khi nạp. */
    private void loadRoles(String selectRoleId) {
        tableModel.setRowCount(0);
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return roleApi.getRoles(serverId);
            }
            @Override
            protected void done() {
                try {
                    currentRoles = get();
                    int selectIdx = -1;
                    for (int i = 0; i < currentRoles.size(); i++) {
                        Map<String, Object> r = currentRoles.get(i);
                        String name = String.valueOf(r.get("roleName")); // API uses roleName
                        if (name.equals("null")) name = String.valueOf(r.get("name")); // Fallback just in case

                        String color = String.valueOf(r.get("color"));
                        if (color == null || color.equals("null")) color = "#FFFFFF";
                        tableModel.addRow(new Object[]{name, color});

                        if (selectRoleId != null && selectRoleId.equals(String.valueOf(r.get("id")))) {
                            selectIdx = i;
                        }
                    }
                    tableModel.fireTableDataChanged();

                    if (selectIdx >= 0) {
                        // Hiện rõ role vừa tạo: chọn + cuộn tới
                        roleTable.setRowSelectionInterval(selectIdx, selectIdx);
                        roleTable.scrollRectToVisible(roleTable.getCellRect(selectIdx, 0, true));
                    } else {
                        // Mặc định: sẵn sàng tạo role mới
                        resetForm();
                    }
                    roleTable.revalidate();
                    roleTable.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RoleManagementDialog.this, "Lỗi tải Role: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void resetForm() {
        // By default, the form should be in "Create Mode" so the owner can always type and add a role directly.
        prepareCreateNew();
    }

    private void onRowSelected() {
        int idx = roleTable.getSelectedRow();
        if (idx >= 0 && currentRoles != null && idx < currentRoles.size()) {
            isCreatingNew = false;
            Map<String, Object> roleData = currentRoles.get(idx);
            selectedRoleId = String.valueOf(roleData.get("id"));
            
            // Fix: Use roleName from API response instead of name
            String name = String.valueOf(roleData.get("roleName"));
            if (name.equals("null")) name = String.valueOf(roleData.get("name"));
            
            String color = String.valueOf(roleData.get("color"));
            boolean isDefault = Boolean.TRUE.equals(roleData.get("isDefault"));
            
            formTitle.setText("Chỉnh sửa Role");
            saveBtn.setText("Lưu Thay Đổi");
            nameField.setText(name);
            nameField.setEnabled(!isDefault);
            colorChooser.setEnabled(true);
            saveBtn.setEnabled(true);
            
            if (color == null || color.equals("null")) color = "#FFFFFF";
            try { colorChooser.setColor(Color.decode(color)); } catch(Exception ignore) {}
            
            deleteBtn.setVisible(!isDefault);
            deleteBtn.setEnabled(!isDefault);
        }
    }

    private void prepareCreateNew() {
        roleTable.clearSelection();
        isCreatingNew = true;
        selectedRoleId = null;
        formTitle.setText("Tạo Role Mới");
        saveBtn.setText("Thêm Role Mới");
        nameField.setText("");
        nameField.setEnabled(true);
        colorChooser.setEnabled(true);
        saveBtn.setEnabled(true);
        colorChooser.setColor(Color.WHITE);
        deleteBtn.setVisible(false);
    }

    private void saveRole() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        
        Color c = colorChooser.getColor();
        String hexColor = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        
        saveBtn.setEnabled(false);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                Map<String, Object> result;
                if (isCreatingNew) {
                    result = roleApi.createRole(serverId, name, hexColor, "SEND_MESSAGE,READ_MESSAGE");
                } else if (selectedRoleId != null) {
                    result = roleApi.updateRole(selectedRoleId, name, hexColor, "SEND_MESSAGE,READ_MESSAGE");
                } else {
                    return null;
                }
                return result != null ? String.valueOf(result.get("id")) : null;
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                try {
                    String savedId = get();
                    loadRoles(savedId); // reload + chọn role vừa lưu để hiển thị rõ
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RoleManagementDialog.this, "Lỗi lưu Role: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void deleteSelectedRole() {
        if (selectedRoleId == null) return;
        int conf = JOptionPane.showConfirmDialog(this, "Xóa Role này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        deleteBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                roleApi.deleteRole(selectedRoleId);
                return null;
            }
            @Override protected void done() {
                deleteBtn.setEnabled(true);
                try {
                    get();
                    loadRoles();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RoleManagementDialog.this, "Lỗi xóa Role: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
