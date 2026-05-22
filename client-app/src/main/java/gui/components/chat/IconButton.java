package gui.components.chat;

import gui.theme.AppColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class IconButton extends JButton {
    public IconButton(String iconText, ActionListener onClick) {
        super(iconText);

        // Base styling for a transparent, borderless icon
        setForeground(AppColors.TEXT_MUTED);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Use a font that guarantees Emoji/Symbol support
        setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        // --- HOVER EFFECT ---
        // Make the icon light up to white when moused over, just like Discord
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setForeground(AppColors.TEXT_WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setForeground(AppColors.TEXT_MUTED);
            }
        });

        // Attach click event if provided
        if (onClick != null) {
            addActionListener(onClick);
        }
    }

    // Overload constructor for when you don't need a click event right away
    public IconButton(String iconText) {
        this(iconText, null);
    }
}
