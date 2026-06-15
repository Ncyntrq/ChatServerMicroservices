package gui.server.settings;

import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.PermissionCache;
import network.RoleApiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

/**
 * Panel quan ly Roles trong Server Settings.
 *
 * Layout: Split 2 cot — Trai la danh sach role, Phai la form tao/chinh sua.
 *
 * Flow:
 *  - Chon role trong list -> form hien thi thong tin de chinh sua
 *  - Nhan "Them Role Moi" -> form reset sang che do tao moi
 *  - Dien ten + chon mau + chon quyen -> Nhan "Tao Role" / "Luu Thay Doi"
 */
public class ServerRolesPanel extends JPanel {

    // ── Palette 16 mau ───────────────────────────────────────────────────
    private static final Color[] PALETTE = {
        Color.decode("#1ABC9C"), Color.decode("#2ECC71"), Color.decode("#3498DB"), Color.decode("#9B59B6"),
        Color.decode("#E91E63"), Color.decode("#F1C40F"), Color.decode("#E67E22"), Color.decode("#E74C3C"),
        Color.decode("#607D8B"), Color.decode("#95A5A6"), Color.decode("#FFD700"), Color.decode("#FF6B6B"),
        Color.decode("#4ECDC4"), Color.decode("#45B7D1"), Color.decode("#FF9FF3"), Color.decode("#A0A0A0"),
    };
    private static final int PALETTE_COLS = 8;
    private static final int SWATCH = 28;

    // ── Dependencies ──────────────────────────────────────────────────────
    private final RoleApiClient roleApi = new RoleApiClient();
    private final long serverId;

    // ── Data ──────────────────────────────────────────────────────────────
    private List<Map<String, Object>> currentRoles;
    private final DefaultListModel<Map<String, Object>> listModel = new DefaultListModel<>();
    private final JList<Map<String, Object>> roleList;

    // ── Form state ────────────────────────────────────────────────────────
    private String editingRoleId = null;          // null = mode tao moi
    private boolean editingIsDefault = false;
    private Color selectedColor = PALETTE[2];     // mac dinh xanh duong
    private int selectedPaletteIdx = 2;
    private int selectedPermBitmask = PermissionCache.READ_MESSAGES;

    // ── Right-panel widgets ───────────────────────────────────────────────
    private final JLabel formTitleLabel;
    private final JTextField nameField;
    private final JButton[] swatchBtns = new JButton[PALETTE.length];
    private final JLabel colorPreviewLabel;
    private final JLabel permSummaryLabel;
    private final JButton saveBtn;
    private final JButton deleteBtn;

    // ── Constructor ───────────────────────────────────────────────────────

    public ServerRolesPanel(long serverId) {
        this.serverId = serverId;
        setLayout(new BorderLayout(0, 0));
        setBackground(AppColors.BG_PRIMARY);

        // ════════════════════════════════════════════════════════════════
        // LEFT PANEL — Danh sach role + nut them moi
        // ════════════════════════════════════════════════════════════════
        JPanel leftPanel = new JPanel(new BorderLayout(0, 0));
        leftPanel.setBackground(AppColors.BG_SECONDARY);
        leftPanel.setPreferredSize(new Dimension(240, 0));
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppColors.SEPARATOR));

        // Header
        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setBackground(AppColors.BG_SECONDARY);
        listHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel listTitle = new JLabel("ROLES");
        listTitle.setFont(AppFonts.CAPTION_BOLD);
        listTitle.setForeground(AppColors.TEXT_MUTED);
        listHeader.add(listTitle, BorderLayout.CENTER);
        leftPanel.add(listHeader, BorderLayout.NORTH);

        // List
        roleList = new JList<>(listModel);
        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleList.setBackground(AppColors.BG_SECONDARY);
        roleList.setBorder(null);
        roleList.setCellRenderer(new RoleCellRenderer());
        roleList.setFixedCellHeight(44);
        roleList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRoleSelected();
        });
        JScrollPane listScroll = new JScrollPane(roleList);
        listScroll.setBorder(null);
        listScroll.setBackground(AppColors.BG_SECONDARY);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        // "Them Role Moi" button at bottom
        JButton addRoleBtn = makeStyledButton("Create Role", AppColors.BRAND_PRIMARY, Color.WHITE);
        addRoleBtn.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        addRoleBtn.addActionListener(e -> prepareCreateNew());
        JPanel addBtnWrapper = new JPanel(new BorderLayout());
        addBtnWrapper.setBackground(AppColors.BG_SECONDARY);
        addBtnWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        addBtnWrapper.add(addRoleBtn, BorderLayout.CENTER);
        leftPanel.add(addBtnWrapper, BorderLayout.SOUTH);

        // ════════════════════════════════════════════════════════════════
        // RIGHT PANEL — Form chinh sua / tao moi
        // ════════════════════════════════════════════════════════════════
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(AppColors.BG_PRIMARY);

        JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setBorder(null);
        rightScroll.setBackground(AppColors.BG_PRIMARY);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);

        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Form title
        formTitleLabel = new JLabel("Create Role");
        formTitleLabel.setFont(AppFonts.HEADING_MD);
        formTitleLabel.setForeground(AppColors.TEXT_WHITE);
        formTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(formTitleLabel);
        rightPanel.add(Box.createVerticalStrut(4));

        JLabel formSubLabel = new JLabel("Use this page to customize the role's appearance and permissions.");
        formSubLabel.setFont(AppFonts.CAPTION);
        formSubLabel.setForeground(AppColors.TEXT_MUTED);
        formSubLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(formSubLabel);
        rightPanel.add(Box.createVerticalStrut(20));

        // ── TEN ROLE ──
        rightPanel.add(makeSectionLabel("ROLE NAME"));
        rightPanel.add(Box.createVerticalStrut(6));
        nameField = new JTextField();
        nameField.setFont(AppFonts.BODY);
        nameField.setBackground(AppColors.BG_TERTIARY);
        nameField.setForeground(AppColors.TEXT_NORMAL);
        nameField.setCaretColor(AppColors.TEXT_WHITE);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.SEPARATOR, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(nameField);
        rightPanel.add(Box.createVerticalStrut(20));

        // ── MAU ROLE ──
        rightPanel.add(makeSectionLabel("ROLE COLOR"));
        rightPanel.add(Box.createVerticalStrut(8));

        // Color preview strip
        colorPreviewLabel = new JLabel("  Selected Color  ");
        colorPreviewLabel.setFont(AppFonts.CAPTION_BOLD);
        colorPreviewLabel.setForeground(Color.WHITE);
        colorPreviewLabel.setOpaque(true);
        colorPreviewLabel.setBackground(selectedColor);
        colorPreviewLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        colorPreviewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(colorPreviewLabel);
        rightPanel.add(Box.createVerticalStrut(8));

        JPanel palettePanel = buildPalettePanel();
        palettePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(palettePanel);
        rightPanel.add(Box.createVerticalStrut(6));

        JButton customColorBtn = makeStyledButton("Custom Color", AppColors.BG_TERTIARY, AppColors.TEXT_MUTED);
        customColorBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        customColorBtn.addActionListener(e -> openCustomColor());
        rightPanel.add(customColorBtn);
        rightPanel.add(Box.createVerticalStrut(20));

        // ── QUYEN HAN ──
        rightPanel.add(makeSectionLabel("PERMISSIONS"));
        rightPanel.add(Box.createVerticalStrut(6));

        JButton permBtn = makeStyledButton("Select Permissions...", AppColors.BG_TERTIARY, AppColors.TEXT_NORMAL);
        permBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        permBtn.addActionListener(e -> openPermPicker());
        rightPanel.add(permBtn);
        rightPanel.add(Box.createVerticalStrut(6));

        permSummaryLabel = new JLabel("<html>" + buildPermSummary(selectedPermBitmask) + "</html>");
        permSummaryLabel.setFont(AppFonts.CAPTION);
        permSummaryLabel.setForeground(AppColors.TEXT_MUTED);
        permSummaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(permSummaryLabel);
        rightPanel.add(Box.createVerticalStrut(28));

        // ── Separator ──
        JSeparator sep = new JSeparator();
        sep.setForeground(AppColors.SEPARATOR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(sep);
        rightPanel.add(Box.createVerticalStrut(16));

        // ── Action buttons ──
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        saveBtn = makeApplyButton("Create Role");
        deleteBtn = makeStyledButton("Delete Role", AppColors.DANGER, Color.WHITE);
        deleteBtn.setVisible(false);

        actionRow.add(saveBtn);
        actionRow.add(deleteBtn);
        rightPanel.add(actionRow);

        saveBtn.addActionListener(e -> saveRole());
        deleteBtn.addActionListener(e -> deleteSelectedRole());

        // ════════════════════════════════════════════════════════════════
        // Assemble
        // ════════════════════════════════════════════════════════════════
        add(leftPanel, BorderLayout.WEST);
        add(rightScroll, BorderLayout.CENTER);

        // Init state
        selectPaletteColor(selectedPaletteIdx);
        loadRoles(null);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Goi tu ben ngoai de reload (vi du sau WS event). */
    public void reloadRoles() {
        loadRoles(editingRoleId);
    }

    // ── Build helpers ─────────────────────────────────────────────────────

    private JLabel makeSectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppFonts.CAPTION_BOLD);
        l.setForeground(AppColors.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel buildPalettePanel() {
        int rows = (int) Math.ceil((double) PALETTE.length / PALETTE_COLS);
        JPanel panel = new JPanel(new GridLayout(rows, PALETTE_COLS, 5, 5));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(PALETTE_COLS * (SWATCH + 5), rows * (SWATCH + 5)));
        for (int i = 0; i < PALETTE.length; i++) {
            final int idx = i;
            JButton btn = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(PALETTE[idx]);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                }
            };
            btn.setPreferredSize(new Dimension(SWATCH, SWATCH));
            btn.setBorder(BorderFactory.createLineBorder(AppColors.SEPARATOR, 2, true));
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(colorToHex(PALETTE[i]));
            btn.addActionListener(e -> selectPaletteColor(idx));
            swatchBtns[i] = btn;
            panel.add(btn);
        }
        return panel;
    }

    private static JButton makeStyledButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(AppFonts.BODY_BOLD);
        b.setForeground(fg);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.SEPARATOR, 1, true),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton makeApplyButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? getBackground() : AppColors.BG_TERTIARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isEnabled() ? getForeground() : AppColors.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        b.setFont(AppFonts.BODY_BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(AppColors.BRAND_PRIMARY);
        b.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(AppColors.BRAND_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(AppColors.BRAND_PRIMARY);
            }
        });
        return b;
    }

    // ── Color palette logic ───────────────────────────────────────────────

    private void selectPaletteColor(int idx) {
        for (JButton b : swatchBtns)
            b.setBorder(BorderFactory.createLineBorder(AppColors.SEPARATOR, 2, true));
        selectedPaletteIdx = idx;
        selectedColor = PALETTE[idx];
        swatchBtns[idx].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2, true),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        updateColorPreview();
    }

    private void openCustomColor() {
        Color chosen = JColorChooser.showDialog(this, "Custom Color", selectedColor);
        if (chosen != null) {
            selectedColor = chosen;
            selectedPaletteIdx = -1;
            for (JButton b : swatchBtns)
                b.setBorder(BorderFactory.createLineBorder(AppColors.SEPARATOR, 2, true));
            updateColorPreview();
        }
    }

    private void updateColorPreview() {
        colorPreviewLabel.setBackground(selectedColor);
        colorPreviewLabel.setText("  " + colorToHex(selectedColor) + "  ");
        // Chon mau chu dua tren do sang nen
        int brightness = (selectedColor.getRed() * 299 + selectedColor.getGreen() * 587 + selectedColor.getBlue() * 114) / 1000;
        colorPreviewLabel.setForeground(brightness > 128 ? Color.BLACK : Color.WHITE);
    }

    // ── Permission picker ─────────────────────────────────────────────────

    private void openPermPicker() {
        int result = PermissionPickerDialog.show(SwingUtilities.getWindowAncestor(this), selectedPermBitmask);
        if (result >= 0) {
            selectedPermBitmask = result;
            permSummaryLabel.setText("<html>" + buildPermSummary(selectedPermBitmask) + "</html>");
        }
    }

    private static String buildPermSummary(int bitmask) {
        if (bitmask == 0) return "<font color='#F0484F'>Không có quyền nào</font>";
        if ((bitmask & PermissionCache.ADMIN) != 0)
            return "<font color='#FFD700'>Quản trị viên - Toàn quyền</font>";
        StringBuilder sb = new StringBuilder();
        addPerm(sb, bitmask, PermissionCache.MANAGE_SERVER,   "Quản lý server");
        addPerm(sb, bitmask, PermissionCache.MANAGE_ROLES,    "Quản lý role");
        addPerm(sb, bitmask, PermissionCache.MANAGE_CHANNEL,  "Quản lý kênh");
        addPerm(sb, bitmask, PermissionCache.KICK_MEMBER,     "Đá thành viên");
        addPerm(sb, bitmask, PermissionCache.BAN_MEMBER,      "Cấm thành viên");
        addPerm(sb, bitmask, PermissionCache.CREATE_INVITE,   "Tạo lời mời");
        addPerm(sb, bitmask, PermissionCache.MANAGE_NICKNAMES,"Quản lý biệt danh");
        addPerm(sb, bitmask, PermissionCache.READ_MESSAGES,   "Đọc kênh");
        addPerm(sb, bitmask, PermissionCache.MANAGE_MESSAGES, "Quản lý tin nhắn");
        return sb.isEmpty() ? "<font color='#F0484F'>Không có quyền nào</font>" : sb.toString();
    }

    private static void addPerm(StringBuilder sb, int mask, int bit, String label) {
        if ((mask & bit) != 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(label);
        }
    }

    private static String colorToHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ── Data loading ──────────────────────────────────────────────────────

    private void loadRoles(String selectRoleId) {
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override protected List<Map<String, Object>> doInBackground() {
                return roleApi.getRoles(serverId);
            }
            @Override protected void done() {
                try {
                    currentRoles = get();
                    listModel.clear();
                    int selectIdx = -1;
                    for (int i = 0; i < currentRoles.size(); i++) {
                        Map<String, Object> r = currentRoles.get(i);
                        listModel.addElement(r);
                        if (selectRoleId != null && selectRoleId.equals(String.valueOf(r.get("id"))))
                            selectIdx = i;
                    }
                    if (selectIdx >= 0) {
                        roleList.setSelectedIndex(selectIdx);
                        roleList.ensureIndexIsVisible(selectIdx);
                    } else {
                        prepareCreateNew();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ServerRolesPanel.this,
                            "Error loading roles: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Form state transitions ────────────────────────────────────────────

    private void prepareCreateNew() {
        SwingUtilities.invokeLater(() -> {
            roleList.clearSelection();
            editingRoleId = null;
            editingIsDefault = false;

            formTitleLabel.setText("Create Role");
            nameField.setText("");
            nameField.setEnabled(true);
            nameField.requestFocusInWindow();
            saveBtn.setText("Create Role");
            saveBtn.setEnabled(true);
            deleteBtn.setVisible(false);
            selectedPermBitmask = PermissionCache.READ_MESSAGES;
            permSummaryLabel.setText("<html>" + buildPermSummary(selectedPermBitmask) + "</html>");
            selectPaletteColor(2); // xanh duong mac dinh
        });
    }

    @SuppressWarnings("unchecked")
    private void onRoleSelected() {
        int idx = roleList.getSelectedIndex();
        if (idx < 0 || currentRoles == null || idx >= currentRoles.size()) return;

        Map<String, Object> r = currentRoles.get(idx);
        editingRoleId = String.valueOf(r.get("id"));
        editingIsDefault = Boolean.TRUE.equals(r.get("isDefault")) || Boolean.TRUE.equals(r.get("default"));

        String name  = resolve(r, "roleName", "name", "Role");
        String color = resolve(r, "color", null, "#3498DB");
        Object permObj = r.get("permissionBitmask");
        selectedPermBitmask = (permObj instanceof Number n) ? n.intValue() : PermissionCache.READ_MESSAGES;

        formTitleLabel.setText("Edit Role: " + name);
        nameField.setText(name);
        nameField.setEnabled(!editingIsDefault);
        saveBtn.setText("Save Changes");
        saveBtn.setEnabled(true);
        deleteBtn.setVisible(!editingIsDefault);
        deleteBtn.setEnabled(!editingIsDefault);
        permSummaryLabel.setText("<html>" + buildPermSummary(selectedPermBitmask) + "</html>");

        try {
            Color c = Color.decode(color);
            selectedColor = c;
            // Tim mau trong palette
            for (int i = 0; i < PALETTE.length; i++) {
                if (PALETTE[i].equals(c)) { selectPaletteColor(i); return; }
            }
            // Mau tuy chinh — xoa highlight palette, cap nhat preview
            selectedPaletteIdx = -1;
            for (JButton b : swatchBtns)
                b.setBorder(BorderFactory.createLineBorder(AppColors.SEPARATOR, 2, true));
            updateColorPreview();
        } catch (Exception ignore) {}
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    private void saveRole() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a role name.", "Missing Name", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocusInWindow();
            return;
        }
        String hexColor = colorToHex(selectedColor);
        String permNames = bitmaskToPermNames(selectedPermBitmask);

        saveBtn.setEnabled(false);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                Map<String, Object> result = (editingRoleId == null)
                        ? roleApi.createRole(serverId, name, hexColor, permNames)
                        : roleApi.updateRole(editingRoleId, name, hexColor, permNames);
                return result != null ? String.valueOf(result.get("id")) : null;
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                try {
                    String savedId = get();
                    loadRoles(savedId);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ServerRolesPanel.this,
                            "Error saving role: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void deleteSelectedRole() {
        if (editingRoleId == null) return;
        int conf = JOptionPane.showConfirmDialog(this,
                "Delete this role? This action cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;

        deleteBtn.setEnabled(false);
        final String id = editingRoleId;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { roleApi.deleteRole(id); return null; }
            @Override protected void done() {
                deleteBtn.setEnabled(true);
                try { get(); loadRoles(null); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(ServerRolesPanel.this,
                            "Error deleting role: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private static String bitmaskToPermNames(int bitmask) {
        int[] bits  = {PermissionCache.READ_MESSAGES, PermissionCache.MANAGE_MESSAGES,
                       PermissionCache.MANAGE_CHANNEL, PermissionCache.KICK_MEMBER,
                       PermissionCache.BAN_MEMBER, PermissionCache.MANAGE_ROLES,
                       PermissionCache.ADMIN, PermissionCache.MANAGE_SERVER,
                       PermissionCache.CREATE_INVITE, PermissionCache.MANAGE_NICKNAMES};
        String[] names = {"READ_MESSAGES","MANAGE_MESSAGES","MANAGE_CHANNEL","KICK_MEMBER",
                          "BAN_MEMBER","MANAGE_ROLES","ADMIN","MANAGE_SERVER",
                          "CREATE_INVITE","MANAGE_NICKNAMES"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits.length; i++) {
            if ((bitmask & bits[i]) != 0) {
                if (sb.length() > 0) sb.append(',');
                sb.append(names[i]);
            }
        }
        return sb.isEmpty() ? "READ_MESSAGES" : sb.toString();
    }

    private static String resolve(Map<String, Object> m, String k1, String k2, String def) {
        Object v = m.get(k1);
        if (v != null && !"null".equals(v.toString())) return v.toString();
        if (k2 != null) { v = m.get(k2); if (v != null && !"null".equals(v.toString())) return v.toString(); }
        return def;
    }

    // ── Custom list cell renderer (no emoji) ──────────────────────────────

    private class RoleCellRenderer extends DefaultListCellRenderer {
        @Override
        @SuppressWarnings("unchecked")
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            row.setBackground(isSelected ? AppColors.BG_ACTIVE : AppColors.BG_SECONDARY);
            row.setOpaque(true);

            if (value instanceof Map<?,?> rawMap) {
                Map<String, Object> m = (Map<String, Object>) rawMap;
                String name  = resolve(m, "roleName", "name", "?");
                String colorHex = resolve(m, "color", null, "#95A5A6");
                boolean isDefault = Boolean.TRUE.equals(m.get("isDefault"))
                        || Boolean.TRUE.equals(m.get("default"));

                // Color dot — painted circle
                Color dotColor;
                try { dotColor = Color.decode(colorHex); } catch (Exception e) { dotColor = Color.GRAY; }
                final Color fc = dotColor;
                JPanel dot = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(fc);
                        g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                        g2.dispose();
                    }
                };
                dot.setPreferredSize(new Dimension(16, 16));
                dot.setOpaque(false);

                // Dot wrapper (vertical center)
                JPanel dotWrapper = new JPanel(new GridBagLayout());
                dotWrapper.setOpaque(false);
                dotWrapper.setPreferredSize(new Dimension(22, 32));
                dotWrapper.add(dot);

                // Name + tag
                JPanel textCol = new JPanel();
                textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
                textCol.setOpaque(false);

                JLabel nameLbl = new JLabel(name);
                nameLbl.setFont(AppFonts.BODY_BOLD);
                nameLbl.setForeground(isSelected ? AppColors.TEXT_WHITE : AppColors.TEXT_NORMAL);
                textCol.add(nameLbl);

                if (isDefault) {
                    JLabel defLbl = new JLabel("Default");
                    defLbl.setFont(AppFonts.TINY);
                    defLbl.setForeground(AppColors.TEXT_MUTED);
                    textCol.add(defLbl);
                }

                row.add(dotWrapper, BorderLayout.WEST);
                row.add(textCol, BorderLayout.CENTER);
            }
            return row;
        }
    }
}
