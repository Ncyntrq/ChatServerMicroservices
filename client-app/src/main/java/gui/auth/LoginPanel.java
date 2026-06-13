package gui.auth;

import gui.components.AuthHeader;
import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import gui.ChatClientGUI;
import network.ApiException;
import network.AuthApiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginPanel extends JPanel {

    private final AuthFrame parent;

    public LoginPanel(AuthFrame parent) {
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        setBackground(AppColors.BG_PRIMARY);

        AuthHeader header = new AuthHeader("Welcome back!", "We're excited to see you again");
        FormField usernameField = new FormField("Username", "Enter your username or email", false);
        FormField passwordField = new FormField("Password", "Enter your password", true);

        JLabel forgotPass = new JLabel("Forgot your password?");
        forgotPass.setForeground(AppColors.TEXT_LINK);
        forgotPass.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linkPanel.setBackground(AppColors.BG_PRIMARY);
        linkPanel.add(forgotPass);

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(AppColors.DANGER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusPanel.setBackground(AppColors.BG_PRIMARY);
        statusPanel.add(statusLabel);

        AuthApiClient authClient = new AuthApiClient();
        PrimaryButton loginButton = new PrimaryButton("Log In", e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Please enter username and password");
                return;
            }
            statusLabel.setText("Logging in...");
            statusLabel.setForeground(AppColors.TEXT_MUTED);

            // Gọi BE trong background thread để không khoá EDT
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    return authClient.login(user, pass).getUsername();
                }

                @Override
                protected void done() {
                    try {
                        String username = get();
                        parent.dispose();

                        ChatClientGUI mainGui = new ChatClientGUI(username);
                        mainGui.setVisible(true);
                        mainGui.startSession();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String msg = cause instanceof ApiException
                                ? cause.getMessage()
                                : "Login failed: " + cause.getMessage();
                        statusLabel.setForeground(AppColors.DANGER);
                        statusLabel.setText(msg);
                    }
                }
            };
            worker.execute();
        });

        // Tiện lợi: nhấn Enter ở username/password → đăng nhập
        usernameField.onEnter(loginButton::doClick);
        passwordField.onEnter(loginButton::doClick);
        // Autofocus vào username khi mở
        SwingUtilities.invokeLater(() -> usernameField.getField().requestFocusInWindow());

        JLabel registerLink = new JLabel("<html><font color='#8B92A0'>Need an account?</font> <font color='#4DA6FF'>Register</font></html>");
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footerPanel.setBackground(AppColors.BG_PRIMARY);
        footerPanel.add(registerLink);

        add(header);
        add(Box.createVerticalStrut(30));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(usernameField);
        add(Box.createVerticalStrut(15));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(passwordField);
        add(Box.createVerticalStrut(5));
        linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(linkPanel);
        add(Box.createVerticalStrut(20));
        add(loginButton);
        add(Box.createVerticalStrut(8));
        statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(statusPanel);
        add(Box.createVerticalStrut(10));
        footerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(footerPanel);

        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (parent != null) parent.showRegisterTab();
            }
        });
    }
}
