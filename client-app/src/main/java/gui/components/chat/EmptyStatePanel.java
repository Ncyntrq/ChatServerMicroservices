package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

public class EmptyStatePanel extends JPanel {
    public EmptyStatePanel(String message) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        // 1. The Mascot
        MascotCanvas mascot = new MascotCanvas();
        mascot.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. The Welcome Text
        JLabel title = new JLabel("Welcome back!");
        title.setFont(AppFonts.HEADING_LG);
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(message);
        subtitle.setFont(AppFonts.BODY);
        subtitle.setForeground(AppColors.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Remove shortcuts as requested
        add(Box.createVerticalGlue());
        add(mascot);
        add(Box.createVerticalStrut(20));
        add(title);
        add(Box.createVerticalStrut(8));
        add(subtitle);
        add(Box.createVerticalGlue());
    }



    private static class MascotCanvas extends JComponent {
        private float tick = 0;
        private Timer timer;

        public MascotCanvas() {
            setPreferredSize(new Dimension(200, 200));
            setMaximumSize(new Dimension(200, 200));
            
            // 60 FPS animation loop
            timer = new Timer(16, e -> {
                tick += 0.05f;
                repaint();
            });
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            if (timer != null) timer.stop();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            if (timer != null) timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            // Float offset using sine wave
            float floatY = (float) Math.sin(tick) * 10f;

            // Draw shadow at bottom
            float shadowScale = 1.0f - ((floatY + 10f) / 40f); 
            int sw = (int) (80 * shadowScale);
            int sh = (int) (15 * shadowScale);
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillOval(cx - sw/2, cy + 60 - sh/2, sw, sh);

            // Draw Mascot Body (Chat Bubble)
            int bodyW = 100;
            int bodyH = 75;
            int bx = cx - bodyW/2;
            int by = cy - bodyH/2 + (int)floatY - 20;

            // Bubble tail
            GeneralPath tail = new GeneralPath();
            tail.moveTo(bx + 20, by + bodyH - 5);
            tail.lineTo(bx + 10, by + bodyH + 15);
            tail.lineTo(bx + 35, by + bodyH - 5);
            tail.closePath();
            
            // Body gradient (Brand colors)
            GradientPaint gp = new GradientPaint(
                bx, by, AppColors.BRAND_PRIMARY,
                bx + bodyW, by + bodyH, AppColors.BRAND_HOVER
            );
            g2.setPaint(gp);
            g2.fill(tail);
            g2.fillRoundRect(bx, by, bodyW, bodyH, 25, 25);

            // Eyes
            g2.setColor(Color.WHITE);
            // Blinking logic: blink quickly every few seconds
            boolean isBlinking = (Math.sin(tick * 0.4f) > 0.96f);
            int eyeY = by + 30;
            int leftEyeX = bx + 30;
            int rightEyeX = bx + 60;
            
            if (isBlinking) {
                // draw horizontal lines for blinking eyes
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(leftEyeX, eyeY + 5, leftEyeX + 10, eyeY + 5);
                g2.drawLine(rightEyeX, eyeY + 5, rightEyeX + 10, eyeY + 5);
            } else {
                g2.fillOval(leftEyeX, eyeY, 10, 12);
                g2.fillOval(rightEyeX, eyeY, 10, 12);
            }

            g2.dispose();
        }
    }
}
