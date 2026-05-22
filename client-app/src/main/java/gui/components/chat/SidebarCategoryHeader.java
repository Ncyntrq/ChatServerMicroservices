package gui.components.chat;

import javax.swing.*;
import java.awt.*;

public class SidebarCategoryHeader extends JPanel {
    public SidebarCategoryHeader(String title) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
        setOpaque(false);

        JLabel label = new JLabel(title.toUpperCase());
        label.setForeground(Color.decode("#80848E"));
        label.setFont(new Font("SansSerif", Font.BOLD, 11));

        add(label);
    }
}
