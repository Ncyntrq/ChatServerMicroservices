package gui.components.chat;

import gui.theme.AppColors;
import javax.swing.*;
import java.awt.*;

public class UnreadBadgePanel extends JPanel {
    private final JLabel badgeLabel;

    public UnreadBadgePanel() {
        super(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(20, 16));

        badgeLabel = new JLabel("");
        badgeLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(badgeLabel, BorderLayout.CENTER);
        setVisible(false);
    }

    public void setCount(int count) {
        if (count > 0) {
            badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
            setVisible(true);
        } else {
            setVisible(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(AppColors.DANGER);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }
}
