package gui.auth;

import gui.components.AuthHeader;
import gui.components.FormField;
import gui.components.PrimaryButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RegisterPanel extends JPanel {
    public RegisterPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        setBackground(Color.decode("#313338"));

        // 1. Header
        AuthHeader header = new AuthHeader("Create an account", "A solid platform for real-time messaging");

        // 2. Fields
        FormField usernameField = new FormField("USERNAME", "Enter your new username", false);
        FormField passwordField = new FormField("PASSWORD", "Enter your new password", true);

        // 3. Register Button
        PrimaryButton registerButton = new PrimaryButton("Register", e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();
            System.out.println("Attempting to register: " + user);
            // TODO: Call ApiClient.register(user, pass)
        });

        // 4. Login Link
        JLabel loginLink = new JLabel("<html><font color='#00A8FC'>Already have an account?</font></html>");
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linkPanel.setBackground(Color.decode("#313338"));
        linkPanel.add(loginLink);

        // 5. Terms of Service Disclaimer
        JLabel termsText = new JLabel("By registering, you agree to our Terms of Service and Privacy Policy.");
        termsText.setForeground(Color.decode("#80848E")); // Very muted text
        termsText.setFont(new Font("SansSerif", Font.PLAIN, 10));
        JPanel termsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        termsPanel.setBackground(Color.decode("#313338"));
        termsPanel.add(termsText);

        // --- Assemble ---
        add(header);
        add(Box.createVerticalStrut(30));

        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(usernameField);
        add(Box.createVerticalStrut(15));

        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(passwordField);
        add(Box.createVerticalStrut(25));

        add(registerButton);
        add(Box.createVerticalStrut(15));

        linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(linkPanel);

        add(Box.createVerticalStrut(30));
        termsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(termsPanel);

        // --- Mock Interactions ---
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Switching back to Login Tab...");
                // We'll hook this up to the JTabbedPane next
            }
        });
    }
}
