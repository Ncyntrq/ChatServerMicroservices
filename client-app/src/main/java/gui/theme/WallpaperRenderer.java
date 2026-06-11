package gui.theme;

import javax.imageio.ImageIO;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Nạp (cache) + vẽ hoạ tiết nền chat: gradient theo theme + pattern phủ kín.
 * Pattern được vẽ ở alpha thấp để giữ chữ dễ đọc (không cần bong bóng tin nhắn).
 */
public final class WallpaperRenderer {

    /** Độ mờ của lớp hoạ tiết — đủ trang trí nhưng không lấn chữ. */
    private static final float PATTERN_ALPHA = 0.30f;
    /** Tỉ lệ thu nhỏ hoạ tiết rồi lát (tile) ⇒ motif nhỏ hơn, lặp dày hơn. */
    private static final double PATTERN_SCALE = 0.5;

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    // Cache 1 ảnh đã ghép (gradient + pattern) đúng kích thước vùng vẽ — tránh scale lại
    // mỗi lần repaint khi cuộn (viewport trong suốt ⇒ repaint nền liên tục).
    private static BufferedImage composed;
    private static String composedKey;
    // Bản đã làm mờ (frosted) dùng cho box sau tin nhắn.
    private static BufferedImage blurred;
    private static String blurredKey;

    private WallpaperRenderer() {}

    private static BufferedImage load(String path) {
        return CACHE.computeIfAbsent(path, p -> {
            try (InputStream in = WallpaperRenderer.class.getResourceAsStream(p)) {
                return in == null ? null : ImageIO.read(in);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /** Nạp ảnh nền tùy chỉnh từ file ngoài (cache theo đường dẫn). Null nếu rỗng/lỗi/đã xoá. */
    private static BufferedImage loadFile(String path) {
        if (path == null || path.isEmpty()) return null;
        return CACHE.computeIfAbsent("file:" + path, p -> {
            try {
                java.io.File f = new java.io.File(path);
                return f.isFile() ? ImageIO.read(f) : null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    /** Vẽ gradient (theo theme) + pattern (theo id + theme) phủ kín vùng w×h (dùng cache). */
    public static void paint(Graphics2D g, int w, int h, Theme theme, String patternId) {
        if (w <= 0 || h <= 0) return;
        g.drawImage(composedFor(w, h, theme, patternId), 0, 0, null);
    }

    /** Ảnh nền đã ghép (cache theo size/theme/pattern). */
    public static BufferedImage composedFor(int w, int h, Theme theme, String patternId) {
        String key = w + "x" + h + "|" + theme + "|" + patternId;
        if (composed == null || !key.equals(composedKey)) {
            composed = compose(w, h, theme, patternId);
            composedKey = key;
        }
        return composed;
    }

    /** Bản nền đã làm mờ (frosted) — dùng cho box sau tin nhắn. */
    public static BufferedImage blurredFor(int w, int h, Theme theme, String patternId) {
        if (w <= 0 || h <= 0) return null;
        String key = w + "x" + h + "|" + theme + "|" + patternId;
        if (blurred == null || !key.equals(blurredKey)) {
            blurred = blur(composedFor(w, h, theme, patternId));
            blurredKey = key;
        }
        return blurred;
    }

    /**
     * Vẽ hiệu ứng frosted cho {@code shape} (toạ độ cục bộ của {@code comp}):
     * lấy nền wallpaper đã blur, căn theo vị trí comp trong JScrollPane, + lớp tint mờ.
     * Dùng chung cho box tin nhắn và toolbar Sửa/Ghim/Xoá.
     */
    public static void paintFrosted(Graphics2D g2, Component comp, Shape shape, Color tint) {
        Container sp = SwingUtilities.getAncestorOfClass(JScrollPane.class, comp);
        Shape oldClip = g2.getClip();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.clip(shape);
        if (sp != null) {
            BufferedImage blur = blurredFor(sp.getWidth(), sp.getHeight(),
                    ThemeManager.get().current(), WallpaperManager.get().activePatternId());
            if (blur != null) {
                Point off = SwingUtilities.convertPoint(comp, 0, 0, sp);
                g2.drawImage(blur, -off.x, -off.y, null);
            }
        }
        if (tint != null) {
            g2.setColor(tint);
            g2.fill(shape);
        }
        g2.setClip(oldClip);
    }

    /** Làm mờ nhanh bằng thu nhỏ rồi phóng to (bilinear) — đủ mượt cho frosted glass. */
    private static BufferedImage blur(BufferedImage src) {
        int dw = Math.max(1, src.getWidth() / 6);
        int dh = Math.max(1, src.getHeight() / 6);
        BufferedImage small = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, dw, dh, null);
        g.dispose();

        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(small, 0, 0, src.getWidth(), src.getHeight(), null);
        g2.dispose();
        return out;
    }

    private static BufferedImage compose(int w, int h, Theme theme, String patternId) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();

        // Chế độ tùy chỉnh của user (ưu tiên hơn gradient/pattern dựng sẵn).
        WallpaperManager wm = WallpaperManager.get();
        if (WallpaperManager.CUSTOM_COLOR.equals(wm.selection())) {
            g.setColor(new Color(wm.customColor(), true));
            g.fillRect(0, 0, w, h);
            g.dispose();
            return out;
        }
        if (WallpaperManager.CUSTOM_IMAGE.equals(wm.selection())) {
            BufferedImage custom = loadFile(wm.customImagePath());
            if (custom != null) {
                drawCover(g, custom, w, h, 1f); // phủ kín, giữ tỉ lệ
            } else { // file lỗi/đã xoá → nền phẳng an toàn
                g.setColor(AppColors.BG_PRIMARY);
                g.fillRect(0, 0, w, h);
            }
            g.dispose();
            return out;
        }

        String suffix = theme == Theme.LIGHT ? "light" : "dark";

        BufferedImage gradient = load("/wallpapers/gradient-" + suffix + ".png");
        if (gradient != null) {
            drawCover(g, gradient, w, h, 1f);
        } else {
            g.setColor(AppColors.BG_PRIMARY);
            g.fillRect(0, 0, w, h);
        }

        if (patternId != null) {
            BufferedImage pattern = load("/wallpapers/pattern-" + patternId + "-" + suffix + ".png");
            if (pattern != null) tilePattern(g, pattern, w, h);
        }
        g.dispose();
        return out;
    }

    /** Lát hoạ tiết đã thu nhỏ phủ kín vùng (motif nhỏ + lặp) ở alpha thấp. */
    private static void tilePattern(Graphics2D g, BufferedImage pattern, int w, int h) {
        int tw = Math.max(1, (int) Math.round(pattern.getWidth() * PATTERN_SCALE));
        int th = Math.max(1, (int) Math.round(pattern.getHeight() * PATTERN_SCALE));
        BufferedImage tile = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = tile.createGraphics();
        tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        tg.drawImage(pattern, 0, 0, tw, th, null);
        tg.dispose();

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, PATTERN_ALPHA));
        g.setPaint(new TexturePaint(tile, new Rectangle(0, 0, tw, th)));
        g.fillRect(0, 0, w, h);
        g.setComposite(old);
    }

    /** Vẽ ảnh phủ kín (scale giữ tỉ lệ, center-crop) với độ mờ cho trước. */
    private static void drawCover(Graphics2D g, BufferedImage img, int w, int h, float alpha) {
        double scale = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight());
        int dw = (int) Math.ceil(img.getWidth() * scale);
        int dh = (int) Math.ceil(img.getHeight() * scale);
        int x = (w - dw) / 2;
        int y = (h - dh) / 2;

        Composite old = g.getComposite();
        if (alpha < 1f) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, x, y, dw, dh, null);
        g.setComposite(old);
    }
}
