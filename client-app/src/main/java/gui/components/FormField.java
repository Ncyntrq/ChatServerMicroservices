package gui.components;

import javax.swing.*;
import java.awt.*;

public class FormField extends JPanel {
    private final JTextField textField;

    public FormField(String labelText, String placeholder, boolean isPassword) {
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.decode("#313338"));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // label
        String htmlLabel = "<html><font color='#B5BAC1' face='sans-serif' size='3'><b>"
                + labelText.toUpperCase()
                + "</b></font> <font color='#F23F42'>*</font></html>";

        JLabel label = new JLabel(htmlLabel);

        // textField
        if (isPassword) {
            textField = new JPasswordField(20);
        } else {
            textField = new JTextField(20);
        }

        textField.putClientProperty("JTextField.placeholderText", placeholder);
        textField.setBackground(Color.decode("#1E1F22"));
        textField.setForeground(Color.decode("#DBDEE1"));
        textField.setCaretColor(Color.WHITE);
        textField.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        textField.setMargin(new Insets(12, 20, 12, 20));
        textField.putClientProperty("JComponent.arc", 50);

        add(label, BorderLayout.NORTH);
        add(textField, BorderLayout.CENTER);
    }

    public String getText() {
        return textField.getText();
    }
}
