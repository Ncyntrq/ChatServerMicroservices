package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;

public class SidebarCategoryHeader extends JPanel {
    public SidebarCategoryHeader(String title) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));
        setOpaque(false);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel label = new JLabel(title.toUpperCase());
        label.setForeground(AppColors.TEXT_MUTED);
        label.setFont(AppFonts.CAPTION_BOLD);

        add(label);
    }
}
