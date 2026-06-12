package gui.components.chat;

import javax.swing.*;
import java.awt.*;

/**
 * Badge biểu tượng file: ô bo góc màu + 3 chữ viết tắt loại file (PDF/DOC/…).
 * Dùng chung cho card file trong tin nhắn và sidebar (tránh emoji ô vuông, đồng nhất mọi OS).
 */
public final class FileBadge {

    private FileBadge() {}

    private record FileType(String abbr, Color color) {}

    private static FileType typeOf(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".pdf"))                                            return new FileType("PDF", new Color(0xE74C3C));
        if (n.endsWith(".zip") || n.endsWith(".rar") || n.endsWith(".7z")) return new FileType("ZIP", new Color(0xF39C12));
        if (n.endsWith(".doc") || n.endsWith(".docx"))                     return new FileType("DOC", new Color(0x2980B9));
        if (n.endsWith(".xls") || n.endsWith(".xlsx") || n.endsWith(".csv")) return new FileType("XLS", new Color(0x27AE60));
        if (n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg")) return new FileType("AUD", new Color(0x9B59B6));
        if (n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi")) return new FileType("VID", new Color(0xE67E22));
        return new FileType("FILE", new Color(0x7F8C8D));
    }

    /** Badge vuông {@code px}×{@code px} bo góc, màu theo loại file, chữ trắng ở giữa. */
    public static JLabel make(String name, int px) {
        final FileType ft = typeOf(name);
        JLabel label = new JLabel(ft.abbr(), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ft.color());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, px / 4)));
        label.setPreferredSize(new Dimension(px, px));
        label.setMinimumSize(new Dimension(px, px));
        label.setMaximumSize(new Dimension(px, px));
        label.setOpaque(false);
        return label;
    }

    /** Định dạng kích thước file dễ đọc (B / KB / MB). Rỗng nếu ≤ 0. */
    public static String humanSize(long b) {
        if (b <= 0) return "";
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.1f MB", b / (1024.0 * 1024));
    }
}
