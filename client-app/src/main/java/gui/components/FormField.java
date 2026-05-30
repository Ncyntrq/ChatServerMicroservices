package gui.components;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;

public class FormField extends JPanel {
    private final JTextField textField;

    public FormField(String labelText, String placeholder, boolean isPassword) {
        setLayout(new BorderLayout(0, 8));
        setBackground(AppColors.BG_PRIMARY);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        // Label with required asterisk
        JLabel label = new JLabel(labelText.toUpperCase());
        label.setFont(AppFonts.CAPTION_BOLD);
        label.setForeground(AppColors.TEXT_MUTED);

        JLabel asterisk = new JLabel(" *");
        asterisk.setFont(AppFonts.CAPTION_BOLD);
        asterisk.setForeground(AppColors.DANGER);

        JPanel labelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelRow.setOpaque(false);
        labelRow.add(label);
        labelRow.add(asterisk);

        // Input field
        if (isPassword) {
            textField = new JPasswordField(20);
        } else {
            textField = new JTextField(20);
        }

        textField.putClientProperty("JTextField.placeholderText", placeholder);
        textField.putClientProperty("JComponent.arc", 8);
        textField.setBackground(AppColors.BG_TERTIARY);
        textField.setForeground(AppColors.TEXT_NORMAL);
        textField.setCaretColor(AppColors.TEXT_WHITE);
        textField.setFont(AppFonts.BODY);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.SEPARATOR, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        textField.setSelectionColor(AppColors.BRAND_PRIMARY);

        add(labelRow, BorderLayout.NORTH);
        add(textField, BorderLayout.CENTER);
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }
}
