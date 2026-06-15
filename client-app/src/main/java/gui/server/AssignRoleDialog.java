package gui.server;

import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.RoleApiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dialog cap Role cho 1 thanh vien trong server.
 *
 * Tinh nang:
 *  - Hien thi toan bo roles cua server voi dot mau + ten + checkbox
 *  - Pre-check roles hien tai ma member dang so huu
 *  - Luu -> goi API assignRoles() roi trigger onSuccess callback (broadcast realtime)
 */
public class AssignRoleDialog extends JDialog {

    private final long serverId;
    private final String username;
    private final Runnable onSuccess;
    private final RoleApiClient roleApi = new RoleApiClient();

    private final JPanel rolesPanel;
    private final List<JCheckBox> checkBoxes = new ArrayList<>();
    private final List<String> roleIds       = new ArrayList<>();
    private final JButton saveBtn;

    public AssignRoleDialog(Window owner, long serverId, String username, Runnable onSuccess) {
        super(owner, "Assign Roles to " + username, ModalityType.APPLICATION_MODAL);
        this.serverId  = serverId;
        this.username  = username;
        this.onSuccess = onSuccess;

        setSize(420, 500);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppColors.BG_PRIMARY);

        // ── Header (custom-painted icon, no emoji) ──
        JPanel header = buildHeader();
        root.add(header, BorderLayout.NORTH);

        // ── Roles list ──
        rolesPanel = new JPanel();
        rolesPanel.setLayout(new BoxLayout(rolesPanel, BoxLayout.Y_AXIS));
        rolesPanel.setBackground(AppColors.BG_PRIMARY);
        rolesPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JLabel loadingLbl = new JLabel("Loading roles...");
        loadingLbl.setFont(AppFonts.BODY);
        loadingLbl.setForeground(AppColors.TEXT_MUTED);
        loadingLbl.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLbl.setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 0));
        rolesPanel.add(loadingLbl);

        JScrollPane scroll = new JScrollPane(rolesPanel);
        scroll.setBorder(null);
        scroll.setBackground(AppColors.BG_PRIMARY);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        // ── Footer ──
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(AppColors.BG_SECONDARY);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JLabel hint = new JLabel("Check the box to assign a role, uncheck to remove.");
        hint.setFont(AppFonts.CAPTION);
        hint.setForeground(AppColors.TEXT_MUTED);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton cancelBtn = makeTextButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        saveBtn = makeApplyButton("Save Changes");
        saveBtn.addActionListener(e -> saveRoles());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        footer.add(hint, BorderLayout.CENTER);
        footer.add(btnPanel, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        loadRolesAndPreSelect();
    }

    // ── Header ────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(AppColors.BG_SECONDARY);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        // Person icon — painted as circle+body
        JComponent personIcon = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(AppColors.BRAND_PRIMARY);
                // Head
                int headR = w / 3;
                g2.fillOval(w / 2 - headR, 1, headR * 2, headR * 2);
                // Body
                g2.fillRoundRect(2, h / 2, w - 4, h / 2, 6, 6);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(28, 28); }
        };

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel titleLbl = new JLabel("Assign Roles");
        titleLbl.setFont(AppFonts.BODY_BOLD);
        titleLbl.setForeground(AppColors.TEXT_WHITE);
        JLabel subLbl = new JLabel(username);
        subLbl.setFont(AppFonts.CAPTION);
        subLbl.setForeground(AppColors.TEXT_MUTED);
        info.add(titleLbl);
        info.add(Box.createVerticalStrut(2));
        info.add(subLbl);

        p.add(personIcon, BorderLayout.WEST);
        p.add(info, BorderLayout.CENTER);
        return p;
    }

    // ── Load roles ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadRolesAndPreSelect() {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{
                    roleApi.getRoles(serverId),
                    roleApi.getMemberRoleIds(serverId, username)
                };
            }
            @Override protected void done() {
                try {
                    Object[] res = get();
                    List<Map<String, Object>> roles   = (List<Map<String, Object>>) res[0];
                    List<String>             current  = (List<String>) res[1];
                    populateRoles(roles, current);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AssignRoleDialog.this,
                            "Error loading roles: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void populateRoles(List<Map<String, Object>> roles, List<String> currentIds) {
        rolesPanel.removeAll();
        checkBoxes.clear();
        roleIds.clear();

        if (roles.isEmpty()) {
            JLabel empty = new JLabel("No roles available in this server.");
            empty.setForeground(AppColors.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));
            rolesPanel.add(empty);
            rolesPanel.revalidate();
            return;
        }

        for (Map<String, Object> r : roles) {
            String id    = String.valueOf(r.get("id"));
            String name  = resolveField(r, "roleName", "name");
            String colorHex = resolveField(r, "color", "color");
            boolean isDefault  = Boolean.TRUE.equals(r.get("isDefault"))
                    || Boolean.TRUE.equals(r.get("default"));
            boolean preChecked = currentIds.contains(id);

            Color roleColor;
            try { roleColor = (colorHex.startsWith("#")) ? Color.decode(colorHex) : Color.GRAY; }
            catch (Exception e) { roleColor = Color.GRAY; }

            rolesPanel.add(buildRoleRow(id, name, roleColor, isDefault, preChecked));
            rolesPanel.add(Box.createVerticalStrut(2));
        }
        rolesPanel.revalidate();
        rolesPanel.repaint();
    }

    private JPanel buildRoleRow(String id, String name, Color roleColor,
                                 boolean isDefault, boolean preChecked) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(AppColors.BG_PRIMARY);
        row.setOpaque(true);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        // Color dot — painted circle
        final Color fc = roleColor;
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fc);
                g2.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                // Subtle border
                g2.setColor(fc.darker());
                g2.drawOval(1, 1, getWidth() - 2, getHeight() - 2);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(16, 16));
        dot.setOpaque(false);
        JPanel dotWrap = new JPanel(new GridBagLayout());
        dotWrap.setOpaque(false);
        dotWrap.setPreferredSize(new Dimension(24, 44));
        dotWrap.add(dot);

        // Name col
        JPanel nameCol = new JPanel();
        nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
        nameCol.setOpaque(false);
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(AppFonts.BODY_BOLD);
        nameLbl.setForeground(AppColors.TEXT_NORMAL);
        nameCol.add(nameLbl);
        if (isDefault) {
            JLabel defTag = new JLabel("Default");
            defTag.setFont(AppFonts.TINY);
            defTag.setForeground(AppColors.TEXT_MUTED);
            nameCol.add(defTag);
        }

        // Checkbox
        JCheckBox cb = new JCheckBox();
        cb.setOpaque(false);
        cb.setSelected(preChecked);
        checkBoxes.add(cb);
        roleIds.add(id);

        // Hover + click
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cb.setSelected(!cb.isSelected()); }
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(AppColors.BG_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { row.setBackground(AppColors.BG_PRIMARY); }
        });

        row.add(dotWrap, BorderLayout.WEST);
        row.add(nameCol, BorderLayout.CENTER);
        row.add(cb, BorderLayout.EAST);
        return row;
    }

    // ── Save ──────────────────────────────────────────────────────────────

    private void saveRoles() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) selected.add(roleIds.get(i));
        }

        saveBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                roleApi.assignRoles(serverId, username, selected);
                return null;
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                try {
                    get();
                    dispose();
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AssignRoleDialog.this,
                            "Error assigning roles: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Button factories ──────────────────────────────────────────────────

    private static JButton makeTextButton(String text) {
        JButton b = new JButton(text);
        b.setFont(AppFonts.BODY_BOLD);
        b.setForeground(AppColors.TEXT_NORMAL);
        b.setBackground(AppColors.BG_TERTIARY);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.SEPARATOR, 1, true),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setContentAreaFilled(true);
        b.setOpaque(true);
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
        b.setBorder(BorderFactory.createEmptyBorder(7, 20, 7, 20));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(AppColors.BRAND_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(AppColors.BRAND_PRIMARY); }
        });
        return b;
    }

    // ── Utils ─────────────────────────────────────────────────────────────

    private static String resolveField(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null && !"null".equals(v.toString()) && !v.toString().isBlank()) return v.toString();
        }
        return "?";
    }
}
