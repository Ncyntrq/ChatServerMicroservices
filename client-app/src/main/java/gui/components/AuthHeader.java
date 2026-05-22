package gui.components;

import javax.swing.*;
import java.awt.*;

public class AuthHeader extends JPanel {
    public AuthHeader(String titleText, String subtitleText) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.decode("#313338"));

        // Logo placeholder
        JLabel logoLabel = new JLabel(" 🚀 ", SwingConstants.CENTER) {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        logoLabel.setOpaque(false);
        logoLabel.setBackground(Color.decode("#5865F2"));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        logoLabel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. Title
        JLabel title = new JLabel(titleText);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 3. Subtitle
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setForeground(Color.decode("#B5BAC1"));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Assemble
        add(logoLabel);
        add(Box.createVerticalStrut(15));
        add(title);
        add(Box.createVerticalStrut(8));
        add(subtitle);
    }
}
