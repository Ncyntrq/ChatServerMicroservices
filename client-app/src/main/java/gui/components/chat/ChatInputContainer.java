package gui.components.chat;

import gui.theme.AppColors;
import javax.swing.*;
import java.awt.*;

public class ChatInputContainer extends JPanel {
    private final JTextField inputField;
    private final JButton sendButton;

    public ChatInputContainer() {
        setLayout(new BorderLayout(10, 0));
        setBackground(AppColors.BG_PRIMARY);

        // --- 1. THE LEFT ICON (Plus Button) ---
        // Using our new standalone component!
        IconButton plusButton = new IconButton("+", e -> {
            System.out.println("Plus button clicked! Open attachment menu.");
        });

        // --- 2. THE MAIN TEXT FIELD ---
        inputField = new JTextField();
        inputField.putClientProperty("JTextField.placeholderText", "Nhập tin nhắn...");
        inputField.putClientProperty("JComponent.arc", 15);
        inputField.setBackground(AppColors.BG_TERTIARY);
        inputField.setForeground(AppColors.TEXT_NORMAL);
        inputField.setCaretColor(AppColors.TEXT_WHITE);
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // --- 3. THE RIGHT WRAPPER (Icons + Send Button) ---
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPanel.setOpaque(false);

        // Add dummy attachment icons using the new component
        rightPanel.add(new IconButton("🎁", e -> System.out.println("Gift menu...")));
        rightPanel.add(new IconButton("😀", e -> System.out.println("Emoji picker...")));

        // The Primary Send Button
        sendButton = new JButton("Gửi ➢");
        sendButton.setBackground(AppColors.BRAND_PRIMARY);
        sendButton.setForeground(AppColors.TEXT_WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        sendButton.putClientProperty("JComponent.arc", 15);
        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        rightPanel.add(sendButton);

        // --- 4. ASSEMBLE ---
        add(plusButton, BorderLayout.WEST);
        add(inputField, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    public String getMessageText() {
        return inputField.getText();
    }

    public void clearInput() {
        inputField.setText("");
    }

    public JButton getSendButton() {
        return sendButton;
    }

    public JTextField getInputField() {
        return inputField;
    }
}
