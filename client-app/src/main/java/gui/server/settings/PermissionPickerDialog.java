package gui.server.settings;

import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.PermissionCache;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dialog chọn quyền (permissions) cho một role.
 *
 * Cách dùng:
 *   int newMask = PermissionPickerDialog.show(ownerWindow, currentBitmask);
 *   if (newMask >= 0) { // người dùng đã xác nhận }
 */
public class PermissionPickerDialog extends JDialog {

    // ── Định nghĩa quyền ─────────────────────────────────────────────────
    private record PermDef(int bit, String name, String desc) {}

    private static final PermDef[] SERVER_PERMS = {
        new PermDef(PermissionCache.ADMIN,          "Quản trị viên",       "Bỏ qua mọi quyền hạn. Toàn quyền trên server - cẩn thận khi cấp."),
        new PermDef(PermissionCache.MANAGE_SERVER,  "Quản lý Máy chủ",     "Thay đổi tên, icon, cài đặt cơ bản của server."),
        new PermDef(PermissionCache.MANAGE_ROLES,   "Quản lý Vai trò",     "Tạo, sửa, xóa role (không thể sửa role cao hơn mình)."),
        new PermDef(PermissionCache.MANAGE_CHANNEL, "Quản lý Kênh",        "Tạo, sửa, xóa hoặc ẩn các kênh chat."),
        new PermDef(PermissionCache.KICK_MEMBER,    "Đá thành viên",       "Kick thành viên ra khỏi server (họ có thể tham gia lại)."),
        new PermDef(PermissionCache.BAN_MEMBER,     "Cấm thành viên",      "Chặn vĩnh viễn thành viên khỏi server."),
    };
    private static final PermDef[] MEMBER_PERMS = {
        new PermDef(PermissionCache.CREATE_INVITE,    "Tạo Lời mời",         "Tạo link mời người khác vào server."),
        new PermDef(PermissionCache.MANAGE_NICKNAMES, "Quản lý Biệt danh",   "Đổi tên hiển thị (nickname) của thành viên khác."),
    };
    private static final PermDef[] CHANNEL_PERMS = {
        new PermDef(PermissionCache.READ_MESSAGES,   "Đọc Kênh",             "Xem và đọc tin nhắn trong kênh."),
        new PermDef(PermissionCache.MANAGE_MESSAGES, "Quản lý Tin nhắn",     "Xóa tin nhắn người khác, ghim tin nhắn."),
    };

    // ── State ─────────────────────────────────────────────────────────────
    private final Map<Integer, JCheckBox> checkBoxMap = new LinkedHashMap<>();
    private int resultBitmask = 0;
    private boolean confirmed = false;

    // ── Static factory ────────────────────────────────────────────────────

    /** Mở dialog modal. Trả về bitmask mới (>= 0) hoặc -1 nếu hủy. */
    public static int show(Window owner, int currentBitmask) {
        PermissionPickerDialog dlg = new PermissionPickerDialog(owner, currentBitmask);
        dlg.setVisible(true);
        return dlg.confirmed ? dlg.resultBitmask : -1;
    }

    // ── Constructor ───────────────────────────────────────────────────────

    private PermissionPickerDialog(Window owner, int currentBitmask) {
        super(owner, "Select Permissions", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(580, 540);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppColors.BG_PRIMARY);

        // ── Header ──
        JPanel header = buildHeader();
        root.add(header, BorderLayout.NORTH);

        // ── Scroll area ──
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppColors.BG_PRIMARY);
        content.setBorder(BorderFactory.createEmptyBorder(4, 20, 12, 20));

        addSection(content, "GENERAL SERVER PERMISSIONS", Color.decode("#E74C3C"), SERVER_PERMS, currentBitmask);
        content.add(Box.createVerticalStrut(4));
        addSection(content, "MEMBERSHIP PERMISSIONS", Color.decode("#3498DB"), MEMBER_PERMS, currentBitmask);
        content.add(Box.createVerticalStrut(4));
        addSection(content, "TEXT CHANNEL PERMISSIONS", Color.decode("#2ECC71"), CHANNEL_PERMS, currentBitmask);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(AppColors.BG_PRIMARY);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        // ── Footer with APPLY button ──
        JPanel footer = buildFooter();
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── UI builders ───────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(AppColors.BG_SECONDARY);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        // Gear icon (custom painted)
        JComponent gearIcon = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppColors.BRAND_PRIMARY);
                // Outer circle
                g2.fillOval(4, 4, getWidth() - 8, getHeight() - 8);
                // Inner cutout
                g2.setColor(AppColors.BG_SECONDARY);
                int s = getWidth() / 3;
                g2.fillOval((getWidth() - s) / 2, (getHeight() - s) / 2, s, s);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(28, 28); }
        };

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("Permissions");
        title.setFont(AppFonts.BODY_BOLD);
        title.setForeground(AppColors.TEXT_WHITE);
        JLabel sub = new JLabel("Check the boxes to assign permissions to this role");
        sub.setFont(AppFonts.CAPTION);
        sub.setForeground(AppColors.TEXT_MUTED);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(sub);

        p.add(gearIcon, BorderLayout.WEST);
        p.add(titleBlock, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(AppColors.BG_SECONDARY);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        // Left: info about current selection
        JLabel info = new JLabel("Click \"Save Changes\" to apply your modifications");
        info.setFont(AppFonts.CAPTION);
        info.setForeground(AppColors.TEXT_MUTED);

        // Right: buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);

        JButton cancelBtn = makeTextButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton applyBtn = makeApplyButton("Save Changes");
        applyBtn.addActionListener(e -> {
            resultBitmask = computeBitmask();
            confirmed = true;
            dispose();
        });

        btns.add(cancelBtn);
        btns.add(applyBtn);

        p.add(info, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private void addSection(JPanel parent, String title, Color accentColor,
                             PermDef[] perms, int currentMask) {
        // Section header with colored left bar
        JPanel headerRow = new JPanel(new BorderLayout(8, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        headerRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 6, 0));

        JComponent bar = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(accentColor);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(4, 16); }
        };

        JLabel lbl = new JLabel(title);
        lbl.setFont(AppFonts.CAPTION_BOLD);
        lbl.setForeground(accentColor);
        headerRow.add(bar, BorderLayout.WEST);
        headerRow.add(lbl, BorderLayout.CENTER);
        parent.add(headerRow);

        // Permission rows
        for (PermDef p : perms) {
            parent.add(buildPermRow(p, currentMask));
            parent.add(Box.createVerticalStrut(2));
        }
    }

    private JPanel buildPermRow(PermDef p, int currentMask) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(AppColors.BG_PRIMARY);
        row.setOpaque(true);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.SEPARATOR, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        // Checkbox
        JCheckBox cb = new JCheckBox();
        cb.setOpaque(false);
        cb.setSelected((currentMask & p.bit()) != 0);
        cb.setPreferredSize(new Dimension(20, 20));
        checkBoxMap.put(p.bit(), cb);

        // Text
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        JLabel nameLbl = new JLabel(p.name());
        nameLbl.setFont(AppFonts.BODY_BOLD);
        nameLbl.setForeground(AppColors.TEXT_HEADER);
        JLabel descLbl = new JLabel(p.desc());
        descLbl.setFont(AppFonts.CAPTION);
        descLbl.setForeground(AppColors.TEXT_MUTED);
        textPanel.add(nameLbl);
        textPanel.add(descLbl);

        // Click anywhere in row to toggle
        row.addMouseListener(new MouseAdapter() {
            Color savedBg = row.getBackground();
            @Override public void mouseClicked(MouseEvent e) { cb.setSelected(!cb.isSelected()); }
            @Override public void mouseEntered(MouseEvent e) {
                savedBg = row.getBackground();
                row.setBackground(AppColors.BG_HOVER);
                row.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                row.setBackground(savedBg);
                row.repaint();
            }
        });

        row.add(cb, BorderLayout.WEST);
        row.add(textPanel, BorderLayout.CENTER);
        return row;
    }

    // ── Button helpers (plain JButton, không dùng PrimaryButton) ─────────

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
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        return b;
    }

    private static JButton makeApplyButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        b.setFont(AppFonts.BODY_BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(AppColors.BRAND_PRIMARY);
        b.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(AppColors.BRAND_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(AppColors.BRAND_PRIMARY); }
        });
        return b;
    }

    // ── Compute ───────────────────────────────────────────────────────────

    private int computeBitmask() {
        int mask = 0;
        for (Map.Entry<Integer, JCheckBox> e : checkBoxMap.entrySet()) {
            if (e.getValue().isSelected()) mask |= e.getKey();
        }
        return mask;
    }
}
