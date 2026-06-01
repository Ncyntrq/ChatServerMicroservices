package gui.components;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.net.URL;
import javax.imageio.ImageIO;

public class AvatarBadge extends JPanel {
    private final String initial;
    private final Color bgColor;
    private Image avatarImage;
    private final int size;

    public AvatarBadge(String initial) {
        this(initial, 40);
    }

    public AvatarBadge(String initial, int size) {
        this(initial, size, null);
    }

    public AvatarBadge(String initial, int size, Image avatarImage) {
        this.initial = initial;
        this.size = size;
        this.avatarImage = avatarImage;
        // Unique color per initial letter for visual variety
        this.bgColor = AppColors.avatarColorFor(initial);

        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setOpaque(false);
    }

    /** Backwards-compatible constructor */
    public AvatarBadge(String initial, Image avatarImage) {
        this(initial, 40, avatarImage);
    }

    public void setAvatarImage(Image avatarImage) {
        this.avatarImage = avatarImage;
        repaint();
    }

    public void loadAvatarFromUrl(String urlString) {
        new Thread(() -> {
            try {
                // download() chỉ gửi JWT (header) khi URL trỏ về gateway tin cậy → chống lộ token tới host lạ
                byte[] bytes = new network.FileApiClient().download(urlString);
                Image downloadedImage = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                if (downloadedImage != null) {
                    SwingUtilities.invokeLater(() -> setAvatarImage(downloadedImage));
                }
            } catch (Exception e) {
                System.err.println("Failed to load avatar from URL: " + urlString + " (" + e.getMessage() + ")");
            }
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        if (avatarImage != null) {
            Shape circle = new Ellipse2D.Double(0, 0, w, h);
            g2.setClip(circle);
            g2.drawImage(avatarImage, 0, 0, w, h, this);
        } else {
            // Gradient fill for richer visual
            g2.setColor(bgColor);
            g2.fillOval(0, 0, w, h);

            // Subtle highlight arc at top-left
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillArc(0, 0, w, h, 45, 90);

            // Initial letter
            g2.setColor(Color.WHITE);
            Font f = size > 30 ? AppFonts.AVATAR_INITIAL : AppFonts.AVATAR_INITIAL_SM;
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();

            int x = (w - fm.stringWidth(initial)) / 2;
            int y = ((h - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(initial, x, y);
        }

        g2.dispose();
    }
}
