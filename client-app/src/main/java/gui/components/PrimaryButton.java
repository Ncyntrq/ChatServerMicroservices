package gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class PrimaryButton extends JPanel {
    private final JButton button;

    public PrimaryButton(String text, ActionListener onClick) {
        setLayout(new BorderLayout());
        setBackground(Color.decode("#313338"));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        setAlignmentX(Component.CENTER_ALIGNMENT);

        button = new JButton(text);
        button.setBackground(Color.decode("#5865F2"));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setMargin(new Insets(12, 20, 12, 20));
        button.putClientProperty("JComponent.arc", 50);

        if (onClick != null) {
            button.addActionListener(onClick);
        }

        add(button, BorderLayout.CENTER);
    }
}
