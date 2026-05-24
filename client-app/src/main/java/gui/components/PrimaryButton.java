package gui.components;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PrimaryButton extends JPanel {
    private final JButton button;

    public PrimaryButton(String text, ActionListener onClick) {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_PRIMARY);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        setAlignmentX(Component.CENTER_ALIGNMENT);

        button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Rounded gradient fill
                GradientPaint gp = new GradientPaint(0, 0, getBackground(),
                        getWidth(), getHeight(), getBackground().darker());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Text
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };
        button.setBackground(AppColors.BRAND_PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFont(AppFonts.BODY_BOLD);
        button.setPreferredSize(new Dimension(0, 44));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(AppColors.BRAND_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(AppColors.BRAND_PRIMARY);
            }
        });

        if (onClick != null) {
            button.addActionListener(onClick);
        }

        add(button, BorderLayout.CENTER);
    }
}
