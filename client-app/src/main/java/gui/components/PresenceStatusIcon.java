package gui.components;

import gui.theme.AppColors;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Icon trạng thái online vẽ tay giống Discord.
 * Implements Icon để có thể dùng trực tiếp trong JMenuItem / JLabel.
 *
 *  - ONLINE          : tròn xanh đặc
 *  - IDLE            : trăng khuyết vàng (inactive)
 *  - AWAY            : đồng hồ màu cam (chủ động vắng mặt)
 *  - DO_NOT_DISTURB  : tròn đỏ gạch ngang
 *  - INVISIBLE/OFFLINE: tròn rỗng xám
 */
public class PresenceStatusIcon extends JComponent implements Icon {

    /** Màu riêng cho trạng thái AWAY — cam, khác với vàng IDLE. */
    public static final Color AWAY_COLOR = new Color(0xE6, 0x7E, 0x22);

    public enum Status {
        ONLINE, IDLE, AWAY, DO_NOT_DISTURB, INVISIBLE, OFFLINE;

        public static Status from(String s) {
            if (s == null) return OFFLINE;
            return switch (s.toUpperCase()) {
                case "ONLINE"          -> ONLINE;
                case "IDLE"            -> IDLE;
                case "AWAY"            -> AWAY;
                case "DO_NOT_DISTURB"  -> DO_NOT_DISTURB;
                case "INVISIBLE"       -> INVISIBLE;
                default                -> OFFLINE;
            };
        }

        public String toVietnamese() {
            return switch (this) {
                case ONLINE         -> "Đang hoạt động";
                case IDLE           -> "Không hoạt động";
                case AWAY           -> "Vắng mặt";
                case DO_NOT_DISTURB -> "Không làm phiền";
                case INVISIBLE      -> "Ẩn mình";
                case OFFLINE        -> "Ngoại tuyến";
            };
        }

        public Color color() {
            return switch (this) {
                case ONLINE         -> AppColors.STATUS_ONLINE;
                case IDLE           -> AppColors.STATUS_IDLE;   // vàng
                case AWAY           -> AWAY_COLOR;              // cam — khác với IDLE
                case DO_NOT_DISTURB -> AppColors.STATUS_DND;
                case INVISIBLE, OFFLINE -> AppColors.STATUS_OFFLINE;
            };
        }
    }

    private Status status;
    private final int size;
    /** Màu nền phía sau icon (để cắt lỗ cutout khớp với background thực). */
    private Color bgColor;

    public PresenceStatusIcon(Status status, int size, Color bgColor) {
        this.status  = status;
        this.size    = size;
        this.bgColor = bgColor != null ? bgColor : AppColors.BG_SECONDARY;
        Dimension d  = new Dimension(size, size);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        setOpaque(false);
    }

    public PresenceStatusIcon(Status status, int size) {
        this(status, size, null);
    }

    public void setStatus(Status s)     { this.status = s; repaint(); }
    public void setBgColor(Color c)     { this.bgColor = c; repaint(); }
    public Status getStatus()           { return status; }

    // ──────────────────────────────────────────────────────────────────
    // JComponent painting
    // ──────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float r = Math.min(getWidth(), getHeight()) / 2f;
        paintStatus(g2, status, getWidth() / 2f, getHeight() / 2f, r, bgColor);
        g2.dispose();
    }

    // ──────────────────────────────────────────────────────────────────
    // javax.swing.Icon — dùng được trong JMenuItem / JLabel
    // ──────────────────────────────────────────────────────────────────

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color bg = (c != null) ? c.getBackground() : bgColor;
        if (bg == null) bg = AppColors.BG_SECONDARY;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float r = size / 2f;
        paintStatus(g2, status, x + r, y + r, r, bg);
        g2.dispose();
    }

    @Override public int getIconWidth()  { return size; }
    @Override public int getIconHeight() { return size; }

    // ──────────────────────────────────────────────────────────────────
    // Core drawing — static so UserListItem / UserFooterPanel can reuse
    // ──────────────────────────────────────────────────────────────────

    /**
     * Vẽ status dot tại (cx, cy) bán kính r vào Graphics2D đã được antialias.
     * Dùng được từ bất kỳ paint() nào.
     */
    public static void paintStatus(Graphics2D g, Status status, float cx, float cy, float r, Color bg) {
        if (bg == null) bg = AppColors.BG_SECONDARY;
        switch (status) {
            case ONLINE          -> paintOnline(g, cx, cy, r, status.color());
            case IDLE            -> paintMoon(g, cx, cy, r, status.color(), bg);
            case AWAY            -> paintClock(g, cx, cy, r, status.color());
            case DO_NOT_DISTURB  -> paintDnd(g, cx, cy, r, status.color());
            case INVISIBLE, OFFLINE -> paintHollow(g, cx, cy, r, status.color(), bg);
        }
    }

    /** Tròn xanh đặc. */
    private static void paintOnline(Graphics2D g, float cx, float cy, float r, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
    }

    /**
     * Trăng khuyết: tròn vàng, phủ tròn bg lệch trên-phải để tạo vết khuyết.
     * Chỉ dùng cho IDLE.
     */
    private static void paintMoon(Graphics2D g, float cx, float cy, float r, Color c, Color bg) {
        g.setColor(c);
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g.setColor(bg);
        float cr = r * 0.65f;
        g.fill(new Ellipse2D.Float(cx - r * 0.1f, cy - r * 0.85f, cr * 2, cr * 2));
    }

    /**
     * Đồng hồ: tròn cam đặc + viền trắng nhỏ (để thấy nền đồng hồ) + 2 kim.
     * Dùng cho AWAY.
     */
    private static void paintClock(Graphics2D g, float cx, float cy, float r, Color c) {
        // Nền đồng hồ
        g.setColor(c);
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

        // Vòng nền trắng nhỏ bên trong (mặt đồng hồ)
        float faceR = r * 0.72f;
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(cx - faceR, cy - faceR, faceR * 2, faceR * 2));

        // Kim giờ (ngắn, chỉ ~10 giờ)
        float sw = Math.max(1f, r * 0.2f);
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        float hourLen = faceR * 0.55f;
        double hourAngle = Math.toRadians(-60);           // 10 giờ
        g.draw(new Line2D.Float(cx, cy,
                cx + (float) (Math.sin(hourAngle) * hourLen),
                cy - (float) (Math.cos(hourAngle) * hourLen)));

        // Kim phút (dài hơn, chỉ ~3 giờ)
        float minLen = faceR * 0.75f;
        double minAngle = Math.toRadians(90);             // 3 giờ
        g.draw(new Line2D.Float(cx, cy,
                cx + (float) (Math.sin(minAngle) * minLen),
                cy - (float) (Math.cos(minAngle) * minLen)));
    }

    /** Tròn đỏ gạch ngang trắng. */
    private static void paintDnd(Graphics2D g, float cx, float cy, float r, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g.setColor(Color.WHITE);
        float barH = Math.max(1.5f, r * 0.30f);
        float barW = r * 1.40f;
        g.fill(new RoundRectangle2D.Float(cx - barW / 2, cy - barH / 2, barW, barH, barH, barH));
    }

    /** Tròn rỗng xám (lỗ cắt bằng màu nền). */
    private static void paintHollow(Graphics2D g, float cx, float cy, float r, Color c, Color bg) {
        g.setColor(c);
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        float inner = r * 0.52f;
        g.setColor(bg);
        g.fill(new Ellipse2D.Float(cx - inner, cy - inner, inner * 2, inner * 2));
    }
}
