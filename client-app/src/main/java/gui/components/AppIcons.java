package gui.components;

import gui.theme.AppColors;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Thư viện icon vẽ tay bằng Java2D — không phụ thuộc vào font/emoji,
 * hiển thị đẹp trên mọi OS (Windows / macOS / Linux).
 *
 * Mỗi phương thức static trả về một {@link Icon} có thể gán cho bất kỳ
 * JButton / JLabel / JMenuItem / JRadioButton nào.
 */
public final class AppIcons {

    private AppIcons() {}

    // ──────────────────────────────────────────────────────────────────
    // Factory: kích thước mặc định
    // ──────────────────────────────────────────────────────────────────

    public static Icon sun(int size)        { return new SunIcon(size); }
    public static Icon moon(int size)       { return new MoonIcon(size); }
    public static Icon gear(int size)       { return new GearIcon(size); }
    public static Icon check(int size)      { return new CheckIcon(size); }
    public static Icon plus(int size)       { return new PlusIcon(size); }
    public static Icon pin(int size)        { return new PinIcon(size); }
    public static Icon search(int size)     { return new SearchIcon(size); }
    public static Icon users(int size)      { return new UsersIcon(size); }
    public static Icon ellipsis(int size)   { return new EllipsisIcon(size); }
    public static Icon download(int size)   { return new DownloadIcon(size); }
    public static Icon gift(int size)       { return new GiftIcon(size); }
    public static Icon smile(int size)      { return new SmileIcon(size); }

    /** Mặt trời (Light mode) — vòng tròn vàng sáng + 8 tia. */
    public static class SunIcon implements Icon {
        private final int size;
        SunIcon(int size) { this.size = size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : new Color(0xF0B233);
            // 8 tia ngoài
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            float outer = size / 2f;
            float inner = outer * 0.42f;
            float rayW  = Math.max(1.5f, outer * 0.12f);
            float rayIn = outer * 0.58f;
            float rayOut= outer * 0.90f;

            g2.setColor(col);
            g2.setStroke(new BasicStroke(rayW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 8; i++) {
                double ang = Math.toRadians(i * 45);
                float sx = cx + (float) Math.sin(ang) * rayIn;
                float sy = cy - (float) Math.cos(ang) * rayIn;
                float ex = cx + (float) Math.sin(ang) * rayOut;
                float ey = cy - (float) Math.cos(ang) * rayOut;
                g2.draw(new Line2D.Float(sx, sy, ex, ey));
            }
            // Nhân tròn
            g2.fill(new Ellipse2D.Float(cx - inner, cy - inner, inner * 2, inner * 2));
            g2.dispose();
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Mặt trăng khuyết (Dark mode) — tròn trắng bị cắt tạo vết khuyết thanh lịch. */
    public static class MoonIcon implements Icon {
        private final int size;
        MoonIcon(int size) { this.size = size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : new Color(0xC8A8E9);
            float r   = size / 2f;
            float cx  = x + r;
            float cy  = y + r;

            // Tạo vùng clip: hình tròn lớn trừ hình tròn lệch (tạo trăng khuyết)
            Area moon = new Area(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            float cr  = r * 0.72f;
            Area cut  = new Area(new Ellipse2D.Float(cx + r * 0.12f, cy - r * 0.88f, cr * 2, cr * 2));
            moon.subtract(cut);

            g2.setColor(col);
            g2.fill(moon);
            g2.dispose();
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Bánh răng cài đặt (⚙) — vẽ tay 8 răng + tròn giữa rỗng. */
    public static class GearIcon implements Icon {
        private final int size;
        GearIcon(int size) { this.size = size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.TEXT_MUTED;
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            float outerR = size / 2f * 0.90f;
            float innerR = outerR * 0.65f;
            float holeR  = outerR * 0.35f;
            int teeth = 8;
            float toothW = (float) Math.toRadians(360.0 / teeth * 0.40);

            // Gear shape via GeneralPath
            GeneralPath gear = new GeneralPath();
            for (int i = 0; i < teeth; i++) {
                double base  = Math.toRadians(i * 360.0 / teeth) - Math.PI / 2;
                double tip   = base + Math.toRadians(360.0 / teeth * 0.5);
                // inner left
                double a0 = base - toothW / 2;
                double a1 = base + toothW / 2;
                // outer (tooth tip)
                double a2 = tip - toothW / 2;
                double a3 = tip + toothW / 2;

                if (i == 0) {
                    gear.moveTo(cx + innerR * Math.cos(a0), cy + innerR * Math.sin(a0));
                } else {
                    gear.lineTo(cx + innerR * Math.cos(a0), cy + innerR * Math.sin(a0));
                }
                gear.lineTo(cx + outerR * Math.cos(a1), cy + outerR * Math.sin(a1));
                gear.lineTo(cx + outerR * Math.cos(a2), cy + outerR * Math.sin(a2));
                gear.lineTo(cx + innerR * Math.cos(a3), cy + innerR * Math.sin(a3));
            }
            gear.closePath();

            // Punch hole in center using Area subtraction
            Area gearArea = new Area(gear);
            gearArea.subtract(new Area(new Ellipse2D.Float(cx - holeR, cy - holeR, holeR * 2, holeR * 2)));

            g2.setColor(col);
            g2.fill(gearArea);
            g2.dispose();
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Dấu tick (✓) — cho popup chọn trạng thái. */
    public static class CheckIcon implements Icon {
        private final int size;
        CheckIcon(int size) { this.size = size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.SUCCESS;
            float sw = Math.max(1.5f, size * 0.15f);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Tick shape: từ trái-dưới lên giữa rồi phải-trên
            float lx = x + size * 0.15f;
            float ly = y + size * 0.52f;
            float mx = x + size * 0.40f;
            float my = y + size * 0.78f;
            float rx = x + size * 0.85f;
            float ry = y + size * 0.22f;
            GeneralPath tick = new GeneralPath();
            tick.moveTo(lx, ly);
            tick.lineTo(mx, my);
            tick.lineTo(rx, ry);
            g2.draw(tick);
            g2.dispose();
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // ──────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────

    private static Graphics2D prep(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    // ──────────────────────────────────────────────────────────────────
    // Additional icons
    // ──────────────────────────────────────────────────────────────────

    /** Dấu cộng (➕) — 2 đường thẳng vuông góc. */
    public static class PlusIcon implements Icon {
        private final int size;
        PlusIcon(int size) { this.size = size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.TEXT_MUTED;
            float sw = Math.max(1.5f, size * 0.14f);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = x + size / 2f, cy = y + size / 2f, arm = size * 0.38f;
            g2.draw(new Line2D.Float(cx - arm, cy, cx + arm, cy));
            g2.draw(new Line2D.Float(cx, cy - arm, cx, cy + arm));
            g2.dispose();
        }
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Pin / ghim (📌) — hình kim ghim đơn giản. */
    public static class PinIcon implements Icon {
        private final int size;
        PinIcon(int size) { this.size = size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.TEXT_MUTED;
            g2.setColor(col);
            float sw = Math.max(1.5f, size * 0.13f);
            float cx = x + size / 2f;
            float topY = y + size * 0.10f;
            float midY = y + size * 0.55f;
            float botY = y + size * 0.95f;
            float r    = size * 0.30f;
            // Đầu tròn
            g2.fill(new Ellipse2D.Float(cx - r, topY, r * 2, r * 2));
            // Thân
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(cx, topY + r * 2, cx, botY));
            // Cánh chéo nhỏ
            float wingW = r * 0.85f;
            g2.draw(new Line2D.Float(cx - wingW, midY, cx + wingW, midY));
            g2.dispose();
        }
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Kính lúp tìm kiếm (🔍). */
    public static class SearchIcon implements Icon {
        private final int size;
        SearchIcon(int size) { this.size = size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.TEXT_MUTED;
            float sw = Math.max(1.5f, size * 0.13f);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float r  = size * 0.31f;
            float ocx = x + r + size * 0.10f;
            float ocy = y + r + size * 0.08f;
            g2.draw(new Ellipse2D.Float(ocx - r, ocy - r, r * 2, r * 2));
            float handleX1 = ocx + r * 0.68f;
            float handleY1 = ocy + r * 0.68f;
            float handleX2 = x + size * 0.92f;
            float handleY2 = y + size * 0.92f;
            g2.draw(new Line2D.Float(handleX1, handleY1, handleX2, handleY2));
            g2.dispose();
        }
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** 2 người (👥) — biểu tượng danh sách thành viên. */
    public static class UsersIcon implements Icon {
        private final int size;
        UsersIcon(int size) { this.size = size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.TEXT_MUTED;
            g2.setColor(col);
            // Người trước (nhỏ, lệch phải-trên)
            float hr = size * 0.18f, br = size * 0.22f;
            float bx2 = x + size * 0.70f, hy2 = y + size * 0.20f, by2 = y + size * 0.62f;
            g2.fill(new Ellipse2D.Float(bx2 - hr, hy2 - hr, hr * 2, hr * 2));
            g2.fill(new Arc2D.Float(bx2 - br, by2 - br * 0.2f, br * 2, br * 2, 0, 180, Arc2D.PIE));
            // Người chính (lớn hơn, bên trái)
            float hr1 = size * 0.22f, br1 = size * 0.27f;
            float bx1 = x + size * 0.38f, hy1 = y + size * 0.22f, by1 = y + size * 0.65f;
            g2.fill(new Ellipse2D.Float(bx1 - hr1, hy1 - hr1, hr1 * 2, hr1 * 2));
            g2.fill(new Arc2D.Float(bx1 - br1, by1 - br1 * 0.2f, br1 * 2, br1 * 2, 0, 180, Arc2D.PIE));
            g2.dispose();
        }
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Ba chấm (⋯) — nút tùy chọn thêm. */
    public static class EllipsisIcon implements Icon {
        private final int size;
        EllipsisIcon(int size) { this.size = size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.TEXT_MUTED;
            g2.setColor(col);
            float r = Math.max(1.5f, size * 0.11f);
            float cy = y + size / 2f;
            float[] xs = { x + size * 0.18f, x + size * 0.50f, x + size * 0.82f };
            for (float dotX : xs) {
                g2.fill(new Ellipse2D.Float(dotX - r, cy - r, r * 2, r * 2));
            }
            g2.dispose();
        }
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Mũi tên tải xuống (⬇) — đường thẳng + mũi tên V. */
    public static class DownloadIcon implements Icon {
        private final int size;
        DownloadIcon(int size) { this.size = size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.TEXT_MUTED;
            float sw = Math.max(1.5f, size * 0.14f);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = x + size / 2f;
            // Đường dọc
            g2.draw(new Line2D.Float(cx, y + size * 0.10f, cx, y + size * 0.72f));
            // Mũi tên
            float aw = size * 0.30f, ay = y + size * 0.72f;
            g2.draw(new Line2D.Float(cx - aw, ay - aw * 0.7f, cx, ay));
            g2.draw(new Line2D.Float(cx + aw, ay - aw * 0.7f, cx, ay));
            // Đường đáy
            g2.draw(new Line2D.Float(x + size * 0.15f, y + size * 0.88f,
                    x + size * 0.85f, y + size * 0.88f));
            g2.dispose();
        }
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Hộp quà (🎁) — hình chữ nhật + nơ đơn giản, vẽ Java2D. */
    public static class GiftIcon implements Icon {
        private final int size;
        GiftIcon(int size) { this.size = size; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.TEXT_MUTED;
            g2.setColor(col);
            float sw  = Math.max(1.5f, size * 0.12f);
            float cx  = x + size / 2f;
            // ─ Nắp hộp ────────────────────────────────────────────
            float lidT = y + size * 0.32f;   // top of lid
            float lidH = size * 0.13f;
            float lidB = lidT + lidH;         // bottom of lid = top of body
            float bx   = x + size * 0.12f;
            float bw   = size * 0.76f;
            float bh   = size * 0.44f;
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Nắp
            g2.draw(new Rectangle2D.Float(bx, lidT, bw, lidH));
            // Thân
            g2.draw(new Rectangle2D.Float(bx, lidB, bw, bh));
            // Dải giữa dọc
            g2.draw(new Line2D.Float(cx, lidT, cx, lidB + bh));
            // ─ Nơ: 2 vòng elip + nút giữa ────────────────────────
            float bowY = y + size * 0.14f;
            float bowRx = size * 0.16f, bowRy = size * 0.11f;
            // Cánh trái
            g2.draw(new Ellipse2D.Float(cx - bowRx * 1.9f, bowY, bowRx * 2, bowRy * 2));
            // Cánh phải
            g2.draw(new Ellipse2D.Float(cx - bowRx * 0.1f, bowY, bowRx * 2, bowRy * 2));
            // Nút nơ
            float knot = Math.max(2f, size * 0.07f);
            g2.fill(new Ellipse2D.Float(cx - knot, bowY + bowRy - knot, knot * 2, knot * 2));
            g2.dispose();
        }
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }

    /** Mặt cười (😀) — vòng tròn + 2 mắt chấm + miệng arc, vẽ Java2D. */
    public static class SmileIcon implements Icon {
        private final int size;
        SmileIcon(int size) { this.size = size; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = prep(g);
            Color col = c != null ? c.getForeground() : AppColors.TEXT_MUTED;
            g2.setColor(col);
            float sw  = Math.max(1.5f, size * 0.11f);
            float r   = size / 2f * 0.90f;
            float cx  = x + size / 2f;
            float cy  = y + size / 2f;
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Vòng tròn mặt
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            // Mắt trái & phải (chấm đặc)
            float eyeR = Math.max(1.5f, r * 0.13f);
            float eyeY = cy - r * 0.28f;
            g2.fill(new Ellipse2D.Float(cx - r * 0.36f - eyeR, eyeY - eyeR, eyeR * 2, eyeR * 2));
            g2.fill(new Ellipse2D.Float(cx + r * 0.36f - eyeR, eyeY - eyeR, eyeR * 2, eyeR * 2));
            // Miệng cong (nửa elip dưới)
            float mw = r * 0.95f, mh = r * 0.52f;
            g2.draw(new Arc2D.Float(cx - mw / 2f, cy + r * 0.06f, mw, mh, 0, -180, Arc2D.OPEN));
            g2.dispose();
        }
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
    }
}
