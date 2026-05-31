package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SidebarCategoryHeader extends JPanel {
    public SidebarCategoryHeader(String title) {
        this(title, null);
    }

    public SidebarCategoryHeader(String title, Runnable onClick) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel label = new JLabel(title.toUpperCase());
        label.setForeground(AppColors.TEXT_MUTED);
        label.setFont(AppFonts.CAPTION_BOLD);
        label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 0));
        add(label, BorderLayout.WEST);
        
        if (onClick != null) {
            JLabel plusLabel = new JLabel("+");
            plusLabel.setForeground(AppColors.TEXT_MUTED);
            plusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            plusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 12));
            add(plusLabel, BorderLayout.EAST);

            setCursor(new Cursor(Cursor.HAND_CURSOR));
            label.setCursor(new Cursor(Cursor.HAND_CURSOR));
            plusLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    label.setForeground(AppColors.TEXT_WHITE);
                    plusLabel.setForeground(AppColors.TEXT_WHITE);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    label.setForeground(AppColors.TEXT_MUTED);
                    plusLabel.setForeground(AppColors.TEXT_MUTED);
                }
            });
        }
    }
}
