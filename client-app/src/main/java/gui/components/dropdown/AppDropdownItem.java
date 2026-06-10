package gui.components.dropdown;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;

public class AppDropdownItem extends JMenuItem {
    private Color defaultBg = AppColors.BG_FLOATING;
    private Color hoverBg = AppColors.BG_HOVER;
    private Color defaultFg = AppColors.TEXT_NORMAL;
    private Color hoverFg = Color.WHITE;

    public AppDropdownItem(String text) {
        super(text);
        init();
    }

    public AppDropdownItem(String text, Icon icon) {
        super(text, icon);
        init();
    }

    public AppDropdownItem(String text, ActionListener action) {
        super(text);
        init();
        addActionListener(action);
    }

    public AppDropdownItem(String text, Color fg, ActionListener action) {
        super(text);
        this.defaultFg = fg;
        this.hoverFg = fg;
        init();
        addActionListener(action);
        setForeground(fg);
    }

    public AppDropdownItem(String text, Color fg, Color hoverBg, ActionListener action) {
        super(text);
        this.defaultFg = fg;
        this.hoverFg = fg;
        this.hoverBg = hoverBg;
        init();
        addActionListener(action);
        setForeground(fg);
    }

    private void init() {
        setFont(AppFonts.BODY_SM);
        setForeground(defaultFg);
        setBackground(defaultBg);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Use custom UI to override native drawing completely so we have full control
        setUI(new javax.swing.plaf.basic.BasicMenuItemUI() {
            @Override
            protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (menuItem.isArmed() || menuItem.isSelected()) {
                    g2.setColor(hoverBg);
                    menuItem.setForeground(hoverFg);
                } else {
                    g2.setColor(defaultBg);
                    menuItem.setForeground(defaultFg);
                }
                
                // Draw edge-to-edge flat background
                g2.fillRect(0, 0, menuItem.getWidth(), menuItem.getHeight());
                g2.dispose();
            }
        });
    }
}
