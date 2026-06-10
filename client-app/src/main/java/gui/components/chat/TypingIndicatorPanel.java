package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

public class TypingIndicatorPanel extends JPanel {
    private final Map<String, Long> typingUsers = new HashMap<>();
    private final Timer cleanupTimer;
    private final Timer animationTimer;
    private final JLabel textLabel;
    
    private float tick = 0;

    public TypingIndicatorPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setOpaque(false);
        setVisible(false);

        textLabel = new JLabel("");
        textLabel.setFont(AppFonts.BODY_BOLD);
        textLabel.setForeground(AppColors.TEXT_NORMAL);
        
        DotPanel dotPanel = new DotPanel();

        add(dotPanel);
        add(textLabel);

        // Timer to clear users who haven't typed in 3 seconds
        cleanupTimer = new Timer(1000, e -> {
            long now = System.currentTimeMillis();
            boolean changed = typingUsers.entrySet().removeIf(entry -> now - entry.getValue() > 3000);
            if (changed) {
                updateUIState();
            }
        });
        cleanupTimer.start();

        // Animation timer for dots
        animationTimer = new Timer(50, e -> {
            tick += 0.2f;
            dotPanel.repaint();
        });
    }

    public void addTypingUser(String username) {
        typingUsers.put(username, System.currentTimeMillis());
        updateUIState();
    }

    public void clear() {
        typingUsers.clear();
        updateUIState();
    }

    private void updateUIState() {
        if (typingUsers.isEmpty()) {
            setVisible(false);
            animationTimer.stop();
            return;
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        int maxNames = 3;
        for (String user : typingUsers.keySet()) {
            if (count > 0) sb.append(", ");
            if (count >= maxNames) {
                sb.append("and others");
                break;
            }
            sb.append(user);
            count++;
        }

        if (typingUsers.size() == 1) {
            sb.append(" is typing...");
        } else {
            sb.append(" are typing...");
        }

        textLabel.setText(sb.toString());
        setVisible(true);
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        revalidate();
        repaint();
    }

    private class DotPanel extends JPanel {
        public DotPanel() {
            setPreferredSize(new Dimension(30, 20));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int baseSize = 5;
            int gap = 8;
            int startX = 2;
            int startY = 10;

            for (int i = 0; i < 3; i++) {
                // Sine wave offset with phase delay for each dot
                float offset = (float) Math.sin(tick - (i * 0.5f)) * 3f;
                int size = baseSize;
                
                g2.setColor(AppColors.TEXT_MUTED);
                g2.fill(new RoundRectangle2D.Float(startX + (i * gap), startY + offset, size, size, size, size));
            }

            g2.dispose();
        }
    }
}
