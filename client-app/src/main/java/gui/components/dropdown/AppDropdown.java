package gui.components.dropdown;

import gui.theme.AppColors;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class AppDropdown extends JPopupMenu {
    public AppDropdown() {
        setBackground(AppColors.BG_FLOATING);
        setOpaque(true);
        
        Border line = new LineBorder(AppColors.SEPARATOR, 1);
        Border margin = new EmptyBorder(4, 4, 4, 4);
        setBorder(new CompoundBorder(line, margin));
    }

    public void addSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(AppColors.SEPARATOR);
        sep.setBackground(AppColors.BG_FLOATING);
        sep.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        add(sep);
    }
}
