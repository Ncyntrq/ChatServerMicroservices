package gui.auth;

import gui.components.AuthHeader;
import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.ApiException;
import network.AuthApiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RegisterPanel extends JPanel {

    private final AuthFrame parent;

    public RegisterPanel(AuthFrame parent) {
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        setBackground(AppColors.BG_PRIMARY);

        AuthHeader header = new AuthHeader("Create an account", "A solid platform for real-time messaging");
        FormField usernameField = new FormField("USERNAME", "Enter your new username", false);
        FormField passwordField = new FormField("PASSWORD", "Enter your new password", true);

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(AppColors.DANGER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusPanel.setBackground(AppColors.BG_PRIMARY);
        statusPanel.add(statusLabel);

        AuthApiClient authClient = new AuthApiClient();
        PrimaryButton registerButton = new PrimaryButton("Register", e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Please enter username and password");
                return;
            }
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Registering...");

            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    return authClient.register(user, pass);
                }

                @Override
                protected void done() {
                    try {
                        String msg = get();
                        statusLabel.setForeground(AppColors.SUCCESS);
                        statusLabel.setText("Registration successful! " + (msg == null ? "" : msg));
                        // Auto chuyển về tab Login sau 1s
                        Timer t = new Timer(800, ev -> {
                            if (parent != null) parent.showLoginTab();
                        });
                        t.setRepeats(false);
                        t.start();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String err = cause instanceof ApiException
                                ? cause.getMessage()
                                : "Registration failed: " + cause.getMessage();
                        statusLabel.setForeground(AppColors.DANGER);
                        statusLabel.setText(err);
                    }
                }
            };
            worker.execute();
        });

        // Tiện lợi: nhấn Enter ở username/password → đăng ký
        usernameField.onEnter(registerButton::doClick);
        passwordField.onEnter(registerButton::doClick);

        JLabel loginLink = new JLabel("<html><font color='#4DA6FF'>Already have an account?</font></html>");
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linkPanel.setBackground(AppColors.BG_PRIMARY);
        linkPanel.add(loginLink);

        JLabel termsText = new JLabel("By registering, you agree to our Terms of Service and Privacy Policy.");
        termsText.setForeground(AppColors.TEXT_MUTED);
        termsText.setFont(new Font("SansSerif", Font.PLAIN, 10));
        JPanel termsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        termsPanel.setBackground(AppColors.BG_PRIMARY);
        termsPanel.add(termsText);

        add(header);
        add(Box.createVerticalStrut(30));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(usernameField);
        add(Box.createVerticalStrut(15));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(passwordField);
        add(Box.createVerticalStrut(25));
        add(registerButton);
        add(Box.createVerticalStrut(8));
        statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(statusPanel);
        add(Box.createVerticalStrut(15));
        linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(linkPanel);
        add(Box.createVerticalStrut(20));
        termsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(termsPanel);

        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (parent != null) parent.showLoginTab();
            }
        });
    }
}
