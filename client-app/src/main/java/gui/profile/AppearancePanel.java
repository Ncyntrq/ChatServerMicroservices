package gui.profile;

import gui.ChatClientGUI;
import gui.ClientApplication;
import gui.components.AppIcons;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import gui.theme.WallpaperManager;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tab "Giao diện": chọn theme Sáng/Tối. Đổi → lưu + dựng lại cửa sổ chat.
 */
public class AppearancePanel extends JPanel {

    public AppearancePanel(JDialog dialog) {
        setLayout(new GridBagLayout());
        setBackground(AppColors.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 16, 0);

        JLabel title = new JLabel("Chế độ hiển thị");
        title.setFont(AppFonts.HEADING_SM);
        title.setForeground(AppColors.TEXT_HEADER);
        add(title, gc);

        ButtonGroup group = new ButtonGroup();

        // Dùng AppIcons thay emoji — tương thích mọi OS
        JRadioButton darkBtn  = themeRadio("  Tối",   AppIcons.moon(16), Theme.DARK,  group, dialog);
        JRadioButton lightBtn = themeRadio("  Sáng",  AppIcons.sun(16),  Theme.LIGHT, group, dialog);

        Theme current = ThemeManager.get().current();
        darkBtn.setSelected(current == Theme.DARK);
        lightBtn.setSelected(current == Theme.LIGHT);

        gc.gridy = 1; gc.insets = new Insets(0, 0, 8, 0);
        add(darkBtn, gc);
        gc.gridy = 2; gc.insets = new Insets(0, 0, 24, 0);
        add(lightBtn, gc);

        // --- Hoạ tiết nền chat ---
        JLabel wpTitle = new JLabel("Hoạ tiết nền chat");
        wpTitle.setFont(AppFonts.HEADING_SM);
        wpTitle.setForeground(AppColors.TEXT_HEADER);
        gc.gridy = 3; gc.insets = new Insets(0, 0, 12, 0);
        add(wpTitle, gc);

        gc.gridy = 4; gc.insets = new Insets(0, 0, 0, 0);
        add(buildWallpaperSelector(dialog), gc);
    }

    /** Combo chọn nền chat: Ngẫu nhiên / Không nền / pattern / Ảnh tùy chỉnh / Màu nền. */
    private JComboBox<String> buildWallpaperSelector(JDialog dialog) {
        Map<String, String> options = new LinkedHashMap<>(); // label → value
        options.put("Ngẫu nhiên", WallpaperManager.RANDOM);
        options.put("Không nền",  WallpaperManager.NONE);
        options.put("Mèo",        "cats");
        options.put("Star Wars",  "starwars");
        options.put("Kẹo ngọt",   "sweets");
        options.put("Ảnh tùy chỉnh…", WallpaperManager.CUSTOM_IMAGE);
        options.put("Màu nền…",       WallpaperManager.CUSTOM_COLOR);

        JComboBox<String> combo = new JComboBox<>(options.keySet().toArray(new String[0]));
        combo.setFont(AppFonts.BODY);
        // Chọn sẵn theo cấu hình hiện tại (set TRƯỚC khi gắn listener ⇒ không bật picker khi mở dialog)
        String cur = WallpaperManager.get().selection();
        options.entrySet().stream()
                .filter(e -> e.getValue().equals(cur))
                .findFirst()
                .ifPresent(e -> combo.setSelectedItem(e.getKey()));

        combo.addActionListener(e -> {
            String value = options.get((String) combo.getSelectedItem());
            if (value == null) return;
            WallpaperManager wm = WallpaperManager.get();
            if (WallpaperManager.CUSTOM_IMAGE.equals(value)) {
                pickImage(dialog, wm);
            } else if (WallpaperManager.CUSTOM_COLOR.equals(value)) {
                pickColor(dialog, wm);
            } else {
                wm.set(value);
                refresh(dialog);
            }
        });
        return combo;
    }

    /** Mở hộp chọn file ảnh; nếu chọn → lưu + áp nền ngay. */
    private void pickImage(JDialog dialog, WallpaperManager wm) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chọn ảnh nền");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Ảnh (png, jpg, jpeg, gif, bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (fc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            wm.setCustomImage(fc.getSelectedFile().getAbsolutePath());
            refresh(dialog);
        }
    }

    /** Mở bảng chọn màu; nếu chọn → lưu + áp nền ngay. */
    private void pickColor(JDialog dialog, WallpaperManager wm) {
        Color init = new Color(wm.customColor(), true);
        Color picked = JColorChooser.showDialog(dialog, "Chọn màu nền", init);
        if (picked != null) {
            wm.setCustomColor(picked.getRGB());
            refresh(dialog);
        }
    }

    /** Vẽ lại nền chat của cửa sổ chính (nếu dialog mở từ ChatClientGUI). */
    private void refresh(JDialog dialog) {
        Window owner = dialog.getOwner();
        if (owner instanceof ChatClientGUI chat) chat.refreshChatBackground();
    }

    /**
     * JRadioButton với icon Java2D ở trái, text ở phải.
     * Icon tự đổi màu theo foreground của button (khi disabled / hover).
     */
    private JRadioButton themeRadio(String text, Icon icon, Theme theme, ButtonGroup group, JDialog dialog) {
        JRadioButton btn = new JRadioButton(text, icon) {
            // Đảm bảo icon luôn được sơn với màu foreground hiện tại
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        btn.setFont(AppFonts.BODY);
        btn.setForeground(AppColors.TEXT_NORMAL);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setIconTextGap(6);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        group.add(btn);
        btn.addActionListener(e -> applyTheme(theme, dialog));
        return btn;
    }

    private void applyTheme(Theme theme, JDialog dialog) {
        if (theme == ThemeManager.get().current()) return;
        ThemeManager.get().set(theme);
        ClientApplication.applyThemeLive(theme);
    }
}
