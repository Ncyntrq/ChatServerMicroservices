package gui.components.feedback;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Thông báo nổi (toast) góc dưới-phải cửa sổ, tự biến mất sau ~3s.
 * Thay cho việc chèn "system message" tạm thời vào luồng chat.
 * Hiển thị trên {@code JLayeredPane} nên không chiếm chỗ layout.
 */
public final class Toast {

    public enum Level { INFO, SUCCESS, WARN, ERROR }

    private static final int MARGIN = 20;
    private static final int GAP = 10;
    private static final int DURATION_MS = 3200;
    private static final int MAX_WIDTH = 320;

    // Các toast đang hiển thị (ứng dụng dùng 1 frame chính nên 1 danh sách tĩnh là đủ).
    private static final List<JComponent> active = new ArrayList<>();

    private Toast() {}

    public static void info(JFrame frame, String message)    { show(frame, message, Level.INFO); }
    public static void success(JFrame frame, String message) { show(frame, message, Level.SUCCESS); }
    public static void warn(JFrame frame, String message)    { show(frame, message, Level.WARN); }
    public static void error(JFrame frame, String message)   { show(frame, message, Level.ERROR); }

    public static void show(JFrame frame, String message, Level level) {
        if (frame == null || message == null) return;
        SwingUtilities.invokeLater(() -> {
            JLayeredPane lp = frame.getLayeredPane();
            JComponent toast = build(message, level);
            lp.add(toast, JLayeredPane.POPUP_LAYER);
            active.add(toast);
            reposition(lp);

            Timer timer = new Timer(DURATION_MS, e -> dismiss(lp, toast));
            timer.setRepeats(false);
            timer.start();
        });
    }

    private static void dismiss(JLayeredPane lp, JComponent toast) {
        active.remove(toast);
        lp.remove(toast);
        lp.repaint();
        reposition(lp);
    }

    /** Xếp các toast từ góc dưới-phải lên trên; toast mới nhất nằm sát góc. */
    private static void reposition(JLayeredPane lp) {
        int y = lp.getHeight() - MARGIN;
        for (int i = active.size() - 1; i >= 0; i--) {
            JComponent c = active.get(i);
            Dimension d = c.getPreferredSize();
            y -= d.height;
            c.setBounds(lp.getWidth() - d.width - MARGIN, y, d.width, d.height);
            y -= GAP;
        }
        lp.revalidate();
        lp.repaint();
    }

    private static JComponent build(String message, Level level) {
        final Color accent = accentColor(level);

        JPanel panel = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D bg = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(AppColors.BG_FLOATING);
                g2.fill(bg);
                // Thanh màu nhấn bên trái theo mức độ
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Float(0, 0, 5, getHeight(), 6, 6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel icon = new JLabel(iconFor(level));
        icon.setFont(AppFonts.EMOJI_SM);
        panel.add(icon, BorderLayout.WEST);

        JLabel text = new JLabel("<html><body style='width:230px'>" + escape(message) + "</body></html>");
        text.setFont(AppFonts.BODY_SM);
        text.setForeground(AppColors.TEXT_NORMAL);
        panel.add(text, BorderLayout.CENTER);

        Dimension pref = panel.getPreferredSize();
        panel.setPreferredSize(new Dimension(Math.min(MAX_WIDTH, pref.width), pref.height));
        panel.setSize(panel.getPreferredSize());
        return panel;
    }

    private static Color accentColor(Level level) {
        return switch (level) {
            case SUCCESS -> AppColors.SUCCESS;
            case WARN -> AppColors.WARNING;
            case ERROR -> AppColors.DANGER;
            default -> AppColors.BRAND_PRIMARY;
        };
    }

    private static String iconFor(Level level) {
        return switch (level) {
            case SUCCESS -> "✅";
            case WARN -> "⚠";
            case ERROR -> "⛔";
            default -> "ℹ";
        };
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
