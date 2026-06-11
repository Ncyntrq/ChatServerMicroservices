package gui.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Tiện ích xử lý ảnh. Scale chất lượng cao (bicubic) thay cho
 * {@code Image.getScaledInstance(SCALE_SMOOTH)} (bilinear) để giữ nét khi xem ảnh phóng to.
 */
public final class ImageUtils {

    private ImageUtils() {}

    /**
     * Scale {@code src} về tối đa {@code maxW}×{@code maxH}, giữ tỉ lệ, không phóng to vượt gốc.
     * Dùng Graphics2D + bicubic + render quality ⇒ ảnh nét hơn rõ rệt so với SCALE_SMOOTH.
     */
    public static BufferedImage highQualityScale(BufferedImage src, int maxW, int maxH) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        if (w <= 0 || h <= 0) return src;

        double scale = Math.min(1.0, Math.min(maxW / (double) w, maxH / (double) h));
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        if (nw == w && nh == h) return src; // không cần scale

        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }
}
