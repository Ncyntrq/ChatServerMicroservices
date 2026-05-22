package gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.net.URL;
import javax.imageio.ImageIO;

public class AvatarBadge extends JPanel {
    private final String initial;
    private Image avatarImage;

    public AvatarBadge(String initial) {
        this(initial, null);
    }

    public AvatarBadge(String initial, Image avatarImage) {
        this.initial = initial;
        this.avatarImage = avatarImage;

        setPreferredSize(new Dimension(40, 40));
        setMinimumSize(new Dimension(40, 40));
        setMaximumSize(new Dimension(40, 40));
        setOpaque(false);
    }

    public void setAvatarImage(Image avatarImage) {
        this.avatarImage = avatarImage;
        repaint();
    }

    public void loadAvatarFromUrl(String urlString) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                Image downloadedImage = ImageIO.read(url);
                if (downloadedImage != null) {
                    SwingUtilities.invokeLater(() -> setAvatarImage(downloadedImage));
                }
            } catch (Exception e) {
                System.err.println("Failed to load avatar from URL: " + urlString);
            }
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (avatarImage != null) {
            Shape circle = new Ellipse2D.Double(0, 0, getWidth(), getHeight());
            g2.setClip(circle);
            g2.drawImage(avatarImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g2.setColor(Color.decode("#5865F2"));
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            FontMetrics fm = g2.getFontMetrics();

            // Center text
            int x = (getWidth() - fm.stringWidth(initial)) / 2;
            int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(initial, x, y);
        }

        g2.dispose();
    }
}
