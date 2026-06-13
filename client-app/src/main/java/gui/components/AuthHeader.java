package gui.components;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;

public class AuthHeader extends JPanel {
    public AuthHeader(String titleText, String subtitleText) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AppColors.BG_PRIMARY);

        // Logo — circular gradient icon
        JPanel logoCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                try {
                    java.net.URL url = getClass().getResource("/logo/logo.png");
                    if (url != null) {
                        Image img = javax.imageio.ImageIO.read(url).getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        g2.drawImage(img, x, y, null);
                        g2.dispose();
                        return;
                    }
                } catch (Exception e) {}

                // Gradient background
                GradientPaint gp = new GradientPaint(x, y, AppColors.BRAND_PRIMARY,
                        x + size, y + size, Color.decode("#EB459E"));
                g2.setPaint(gp);
                g2.fillOval(x, y, size, size);

                java.awt.image.BufferedImage emoji =
                        gui.components.chat.EmojiHelper.cachedImageForChar("💬", 30);
                if (emoji != null) {
                    g2.drawImage(emoji, (getWidth() - emoji.getWidth()) / 2,
                            (getHeight() - emoji.getHeight()) / 2, null);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 28));
                    FontMetrics fm = g2.getFontMetrics();
                    String icon = "💬";
                    int tx = (getWidth() - fm.stringWidth(icon)) / 2;
                    int ty = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(icon, tx, ty);
                }

                g2.dispose();
            }
        };
        logoCircle.setPreferredSize(new Dimension(64, 64));
        logoCircle.setMaximumSize(new Dimension(64, 64));
        logoCircle.setOpaque(false);
        logoCircle.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Nạp nền ảnh emoji rồi vẽ lại (lần đầu chưa có cache sẽ fallback drawString)
        gui.components.chat.EmojiHelper.prefetchChar("💬", 30, logoCircle::repaint);

        // Title
        JLabel title = new JLabel(titleText);
        title.setForeground(AppColors.TEXT_HEADER);
        title.setFont(AppFonts.HEADING_LG);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Subtitle
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setForeground(AppColors.TEXT_MUTED);
        subtitle.setFont(AppFonts.BODY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Assemble
        add(logoCircle);
        add(Box.createVerticalStrut(16));
        add(title);
        add(Box.createVerticalStrut(8));
        add(subtitle);
    }
}
