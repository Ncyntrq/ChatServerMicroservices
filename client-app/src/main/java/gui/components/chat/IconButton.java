package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class IconButton extends JButton {
    private boolean isHovered = false;

    public IconButton(String iconText, ActionListener onClick) {
        super(iconText);

        setForeground(AppColors.TEXT_MUTED);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(AppFonts.EMOJI);
        setPreferredSize(new Dimension(34, 34));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                setForeground(AppColors.TEXT_WHITE);
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                setForeground(AppColors.TEXT_MUTED);
                repaint();
            }
        });

        if (onClick != null) {
            addActionListener(onClick);
        }
    }

    public IconButton(String iconText) {
        this(iconText, null);
    }

    /** Constructor nhận Icon vẽ tay — không phụ thuộc font/emoji. */
    public IconButton(Icon icon, ActionListener onClick) {
        this("", onClick);
        setIcon(icon);
        setFont(AppFonts.BODY);       // không cần EMOJI font nữa
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
    }

    public IconButton(Icon icon) {
        this(icon, null);
    }

    /** Biến thể nhỏ gọn (vd toolbar tin nhắn): font emoji nhỏ + kích thước tùy chỉnh. */
    public IconButton(String iconText, ActionListener onClick, int size) {
        this(iconText, onClick);
        setFont(AppFonts.EMOJI_SM);
        setPreferredSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isHovered) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 15));
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
