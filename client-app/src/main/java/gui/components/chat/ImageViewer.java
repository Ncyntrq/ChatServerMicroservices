package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.FileApiClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Xem ảnh gốc trong dialog (lightbox). Dùng chung cho card ảnh trong tin nhắn và sidebar gallery.
 * Tải qua {@link FileApiClient} (JWT header) + scale bicubic chất lượng cao.
 */
public final class ImageViewer {

    private ImageViewer() {}

    /** Tải bytes ảnh từ gateway rồi scale chất lượng cao về tối đa {@code maxW}×{@code maxH}. */
    public static ImageIcon loadScaled(String fullUrl, int maxW, int maxH) {
        if (fullUrl == null) return null;
        try {
            byte[] bytes = new FileApiClient().download(fullUrl); // JWT qua header, chặn host lạ
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
            if (src == null) return null;
            return new ImageIcon(gui.utils.ImageUtils.highQualityScale(src, maxW, maxH));
        } catch (Exception e) {
            return null;
        }
    }

    /** Mở dialog xem ảnh gốc (~85% màn hình), tải nền bằng SwingWorker. */
    public static void open(Component anchor, String url, String name) {
        Window owner = anchor != null ? SwingUtilities.getWindowAncestor(anchor) : null;
        JDialog dlg = new JDialog(owner, name != null ? name : "Ảnh", Dialog.ModalityType.MODELESS);
        JLabel label = new JLabel("Đang tải…", SwingConstants.CENTER);
        label.setForeground(AppColors.TEXT_MUTED);
        label.setFont(AppFonts.BODY_SM);
        dlg.setContentPane(new JScrollPane(label));

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxW = (int) (screen.width * 0.85), maxH = (int) (screen.height * 0.85);
        dlg.setSize(Math.min(900, maxW), Math.min(720, maxH));
        dlg.setLocationRelativeTo(owner);

        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() { return loadScaled(url, maxW, maxH); }
            @Override protected void done() {
                try {
                    ImageIcon ic = get();
                    if (ic != null) { label.setText(null); label.setIcon(ic); }
                    else label.setText("[Không tải được ảnh]");
                } catch (Exception e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    label.setText("[Lỗi tải ảnh]");
                }
            }
        }.execute();
        dlg.setVisible(true);
    }
}
