package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChatInputContainer extends JPanel {
    private final JTextField inputField;
    private final JButton sendButton;

    public ChatInputContainer() {
        setLayout(new BorderLayout(8, 0));
        setBackground(AppColors.BG_TERTIARY);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Make the whole container have rounded corners
        putClientProperty("JComponent.arc", 12);

        // --- 1. Left icon (Plus/Attach) ---
        IconButton plusButton = new IconButton("+", e -> {
            System.out.println("Plus button clicked! Open attachment menu.");
        });

        // --- 2. Main text field ---
        inputField = new JTextField();
        inputField.putClientProperty("JTextField.placeholderText", "Nhập tin nhắn...");
        inputField.setBackground(AppColors.BG_TERTIARY);
        inputField.setForeground(AppColors.TEXT_NORMAL);
        inputField.setCaretColor(AppColors.TEXT_WHITE);
        inputField.setFont(AppFonts.BODY);
        inputField.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inputField.setSelectionColor(AppColors.BRAND_PRIMARY);

        // --- 3. Right panel: emoji + send ---
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightPanel.setOpaque(false);

        rightPanel.add(new IconButton("🎁", e -> System.out.println("Gift menu...")));
        rightPanel.add(new IconButton("😀", e -> System.out.println("Emoji picker...")));

        // Send button — pill shape
        sendButton = new JButton("Gửi") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        sendButton.setBackground(AppColors.BRAND_PRIMARY);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setBorderPainted(false);
        sendButton.setFont(AppFonts.CAPTION_BOLD);
        sendButton.setPreferredSize(new Dimension(56, 32));
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(AppColors.BRAND_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(AppColors.BRAND_PRIMARY);
            }
        });

        rightPanel.add(sendButton);

        // --- Assemble ---
        add(plusButton, BorderLayout.WEST);
        add(inputField, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(AppColors.BG_TERTIARY);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
        super.paintComponent(g);
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
