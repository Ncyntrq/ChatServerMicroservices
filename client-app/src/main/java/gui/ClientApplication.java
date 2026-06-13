package gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import gui.auth.AuthFrame;
import gui.theme.AppColors;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import network.SessionManager;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.Map;

public class ClientApplication {
    public static void main(String[] args) {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new java.awt.Font(FlatRobotoFont.FAMILY, java.awt.Font.PLAIN, 14));
        applyLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            // Có session hợp lệ → vào thẳng chat; chưa có → hiển thị landing.
            if (SessionManager.get().isAuthenticated()) {
                ChatClientGUI gui = new ChatClientGUI(SessionManager.get().getUsername());
                gui.setVisible(true);
                gui.startSession();
            } else {
                new AuthFrame().setVisible(true);
            }
        });
    }

    /**
     * Áp Look&Feel theo theme đang chọn + nạp lại bảng màu & fonts.
     * Gọi lúc khởi động và mỗi khi đổi theme.
     */
    public static void applyLookAndFeel() {
        try {
            Theme theme = ThemeManager.get().current();
            UIManager.setLookAndFeel(theme == Theme.LIGHT ? new FlatLightLaf() : new FlatDarkLaf());
            AppColors.apply(theme);
            // Áp mức zoom đã lưu (nếu có) lên fonts + defaultFont của FlatLaf.
            gui.theme.AppFonts.applyGlobalScale();
        } catch (Exception e) {
            System.err.println("Couldn't load Laf: " + e);
        }
    }

    /**
     * Đổi theme NGAY tại chỗ (không rebuild cửa sổ, không reconnect WS / reload dữ
     * liệu):
     * - đổi Look&Feel FlatLaf (cập nhật component chuẩn qua updateUI);
     * - remap các màu nền/chữ trung tính đã set lúc dựng (qua
     * {@link AppColors#buildRemap});
     * - component tự vẽ (tin nhắn, wallpaper, toolbar) đọc màu/theme mỗi lần
     * repaint nên tự cập nhật.
     * Có hiệu ứng chuyển mượt của FlatLaf (crossfade).
     */
    public static void applyThemeLive(Theme newTheme) {
        // AppColors sau gọi này = newTheme (buildRemap apply(from) rồi apply(to)).
        Map<Color, Color> remap = AppColors.buildRemap(newTheme.toggled(), newTheme);

        try {
            UIManager.setLookAndFeel(newTheme == Theme.LIGHT ? new FlatLightLaf() : new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Couldn't switch Laf: " + e);
        }
        for (Window w : Window.getWindows())
            remapColors(w, remap);
        FlatLaf.updateUI();
    }

    /**
     * Đệ quy đổi background/foreground theo bản đồ remap (chỉ các màu khớp đúng).
     */
    private static void remapColors(Component c, Map<Color, Color> m) {
        Color bg = c.getBackground();
        if (bg != null) {
            Color nb = m.get(bg);
            if (nb != null)
                c.setBackground(nb);
        }
        Color fg = c.getForeground();
        if (fg != null) {
            Color nf = m.get(fg);
            if (nf != null)
                c.setForeground(nf);
        }
        if (c instanceof Container ct) {
            for (Component child : ct.getComponents())
                remapColors(child, m);
        }
    }
}
