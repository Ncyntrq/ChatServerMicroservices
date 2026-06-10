package gui.auth;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;

public class AuthGraphicPanel extends JPanel {
    
    public AuthGraphicPanel() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(60, 40, 40, 40));

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logoIcon = new JLabel("💬");
        logoIcon.setFont(AppFonts.EMOJI.deriveFont(28f));
        logoIcon.setForeground(Color.WHITE);

        JLabel logoText = new JLabel("ChatServer");
        logoText.setFont(AppFonts.HEADING_LG.deriveFont(Font.BOLD, 32f));
        logoText.setForeground(Color.WHITE);

        logoRow.add(logoIcon);
        logoRow.add(logoText);

        JLabel desc = new JLabel("<html><div style='text-align: center; color: rgba(255,255,255,0.8); width: 250px;'>" 
                + "Connect with friends and communities.<br>Experience smooth, real-time messaging.</div></html>");
        desc.setFont(AppFonts.BODY);
        desc.setHorizontalAlignment(SwingConstants.CENTER);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);

        // QR Code Placeholder
        JPanel qrPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                // Draw a fake QR grid
                g2.setColor(new Color(0, 0, 0, 30));
                for(int i = 12; i < getWidth() - 12; i += 12) {
                    for(int j = 12; j < getHeight() - 12; j += 12) {
                        if (Math.random() > 0.3) {
                            g2.fillRoundRect(i, j, 8, 8, 2, 2);
                        }
                    }
                }
                
                // Draw corner markers
                g2.setColor(new Color(88, 101, 242)); // Muted brand color
                g2.setStroke(new BasicStroke(5));
                g2.drawRoundRect(14, 14, 28, 28, 6, 6);
                g2.drawRoundRect(getWidth() - 42, 14, 28, 28, 6, 6);
                g2.drawRoundRect(14, getHeight() - 42, 28, 28, 6, 6);
                
                g2.dispose();
            }
        };
        qrPanel.setPreferredSize(new Dimension(180, 180));
        qrPanel.setMaximumSize(new Dimension(180, 180));
        qrPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        qrPanel.setOpaque(false);

        JLabel qrTitle = new JLabel("Log in with QR Code");
        qrTitle.setFont(AppFonts.HEADING_MD);
        qrTitle.setForeground(Color.WHITE);
        qrTitle.setHorizontalAlignment(SwingConstants.CENTER);
        qrTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel qrDesc = new JLabel("<html><div style='text-align: center; color: rgba(255,255,255,0.7); width: 250px;'>" 
                + "Scan the code with the mobile app<br>to log in instantly.</div></html>");
        qrDesc.setFont(AppFonts.BODY_SM);
        qrDesc.setHorizontalAlignment(SwingConstants.CENTER);
        qrDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(Box.createVerticalGlue());
        add(logoRow);
        add(Box.createVerticalStrut(15));
        add(desc);
        add(Box.createVerticalStrut(60));
        add(qrPanel);
        add(Box.createVerticalStrut(24));
        add(qrTitle);
        add(Box.createVerticalStrut(8));
        add(qrDesc);
        add(Box.createVerticalGlue());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Soothing, softer gradient (Slate Blue to Deep Purple)
        GradientPaint gp = new GradientPaint(
                0, 0, new Color(74, 85, 162),
                getWidth(), getHeight(), new Color(25, 33, 78)
        );
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.dispose();
    }
}
