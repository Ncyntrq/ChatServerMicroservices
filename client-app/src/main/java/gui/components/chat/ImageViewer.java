package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.FileApiClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Xem ảnh gốc trong dialog (lightbox). Dùng chung cho card ảnh trong tin nhắn và sidebar gallery.
 * Hỗ trợ:
 *  - Tự scale fit ảnh theo cửa sổ (giữ nguyên tỉ lệ)
 *  - Lăn chuột để zoom in/out (giữ điểm tâm dưới con trỏ)
 *  - Kéo resize cửa sổ → ảnh scale theo (ở mức zoom fit)
 *  - Kéo chuột để pan khi ảnh đã zoom
 */
public final class ImageViewer {

    private ImageViewer() {}

    /** Tải bytes ảnh từ gateway (không scale, lấy ảnh gốc). */
    public static ImageIcon loadScaled(String fullUrl, int maxW, int maxH) {
        if (fullUrl == null) return null;
        try {
            byte[] bytes = new FileApiClient().download(fullUrl);
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
            if (src == null) return null;
            return new ImageIcon(gui.utils.ImageUtils.highQualityScale(src, maxW, maxH));
        } catch (Exception e) {
            return null;
        }
    }

    /** Mở dialog xem ảnh với đầy đủ tính năng zoom/pan/resize. */
    public static void open(Component anchor, String url, String name) {
        Window owner = anchor != null ? SwingUtilities.getWindowAncestor(anchor) : null;
        JDialog dlg = new JDialog(owner, name != null ? name : "Ảnh", Dialog.ModalityType.MODELESS);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Panel loading placeholder
        JLabel loadingLabel = new JLabel("Đang tải ảnh…", SwingConstants.CENTER);
        loadingLabel.setForeground(AppColors.TEXT_MUTED);
        loadingLabel.setFont(AppFonts.BODY_SM);
        loadingLabel.setOpaque(true);
        loadingLabel.setBackground(AppColors.BG_PRIMARY);

        dlg.setContentPane(loadingLabel);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int dlgW = Math.min(900, (int) (screen.width  * 0.85));
        int dlgH = Math.min(720, (int) (screen.height * 0.85));
        dlg.setSize(dlgW, dlgH);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                byte[] bytes = new FileApiClient().download(url);
                if (bytes == null || bytes.length == 0) return null;
                return ImageIO.read(new ByteArrayInputStream(bytes));
            }

            @Override
            protected void done() {
                try {
                    BufferedImage img = get();
                    if (img != null) {
                        ImageCanvas canvas = new ImageCanvas(img);
                        dlg.setContentPane(canvas);
                        dlg.revalidate();
                        dlg.repaint();

                        // Gắn phím Escape để đóng
                        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
                        dlg.getRootPane().getActionMap().put("close", new AbstractAction() {
                            @Override public void actionPerformed(ActionEvent e) { dlg.dispose(); }
                        });
                    } else {
                        loadingLabel.setText("[Không tải được ảnh]");
                    }
                } catch (Exception e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    loadingLabel.setText("[Lỗi tải ảnh]");
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Canvas nội bộ: vẽ ảnh với zoom/pan đầy đủ tính năng
    // ------------------------------------------------------------------
    private static class ImageCanvas extends JPanel {
        private final BufferedImage img;

        /** Tỉ lệ zoom hiện tại (1.0 = 100%). */
        private double zoom = 1.0;
        /** Giới hạn zoom. */
        private static final double ZOOM_MIN = 0.1, ZOOM_MAX = 8.0, ZOOM_STEP = 0.12;

        /** Offset pan (trung tâm ảnh so với trung tâm canvas). */
        private double panX = 0, panY = 0;

        /** Điểm kéo để pan. */
        private Point dragStart;
        private double panXAtDrag, panYAtDrag;

        /** True = đang ở chế độ fit-to-window (chưa tự zoom bằng tay). */
        private boolean fitMode = true;

        ImageCanvas(BufferedImage img) {
            this.img = img;
            setBackground(AppColors.BG_PRIMARY);
            setOpaque(true);

            // Zoom bằng lăn chuột
            addMouseWheelListener(e -> {
                fitMode = false;
                double delta = -e.getPreciseWheelRotation() * ZOOM_STEP;
                double newZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom + delta));
                if (newZoom == zoom) return;

                // Zoom quanh điểm con trỏ chuột (giữ nguyên điểm ảnh dưới cursor)
                double mx = e.getX() - getWidth()  / 2.0 - panX;
                double my = e.getY() - getHeight() / 2.0 - panY;
                double ratio = newZoom / zoom;
                panX += mx - mx * ratio;
                panY += my - my * ratio;
                zoom = newZoom;
                repaint();
            });

            // Pan bằng kéo chuột
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        dragStart = e.getPoint();
                        panXAtDrag = panX;
                        panYAtDrag = panY;
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                }
                @Override public void mouseReleased(MouseEvent e) {
                    dragStart = null;
                    setCursor(Cursor.getDefaultCursor());
                }
                @Override public void mouseClicked(MouseEvent e) {
                    // Double-click để toggle fit/100%
                    if (e.getClickCount() == 2) {
                        if (fitMode) {
                            zoom = 1.0; panX = 0; panY = 0;
                            fitMode = false;
                        } else {
                            panX = 0; panY = 0;
                            fitMode = true;
                        }
                        repaint();
                    }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragStart != null && SwingUtilities.isLeftMouseButton(e)) {
                        fitMode = false;
                        panX = panXAtDrag + (e.getX() - dragStart.x);
                        panY = panYAtDrag + (e.getY() - dragStart.y);
                        repaint();
                    }
                }
            });

            // Resize cửa sổ → repaint (fit sẽ tính lại trong paintComponent)
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);

            int cw = getWidth(), ch = getHeight();
            int iw = img.getWidth(), ih = img.getHeight();

            double effectiveZoom;
            if (fitMode) {
                // Tính tỉ lệ fit-to-window (giữ aspect ratio, có padding nhỏ)
                double scaleX = (cw - 24.0) / iw;
                double scaleY = (ch - 24.0) / ih;
                effectiveZoom = Math.min(1.0, Math.min(scaleX, scaleY)); // không phóng to hơn 100% khi fit
                zoom = effectiveZoom; // cập nhật zoom để zoom wheel kế tiếp dùng đúng base
            } else {
                effectiveZoom = zoom;
            }

            int drawW = (int) (iw * effectiveZoom);
            int drawH = (int) (ih * effectiveZoom);

            // Tâm canvas + offset pan
            double ox = (cw - drawW) / 2.0 + (fitMode ? 0 : panX);
            double oy = (ch - drawH) / 2.0 + (fitMode ? 0 : panY);

            g2.drawImage(img, (int) ox, (int) oy, drawW, drawH, null);
            g2.dispose();

            // Hint zoom level ở góc dưới phải
            String hint = String.format("%.0f%%", effectiveZoom * 100);
            Graphics2D gh = (Graphics2D) g.create();
            gh.setFont(AppFonts.TINY != null ? AppFonts.TINY : new Font("SansSerif", Font.PLAIN, 10));
            gh.setColor(new Color(255, 255, 255, 160));
            FontMetrics fm = gh.getFontMetrics();
            int hw = fm.stringWidth(hint);
            gh.fillRoundRect(cw - hw - 18, ch - 22, hw + 12, 16, 6, 6);
            gh.setColor(new Color(50, 50, 50, 200));
            gh.drawString(hint, cw - hw - 12, ch - 10);
            gh.dispose();
        }
    }
}
