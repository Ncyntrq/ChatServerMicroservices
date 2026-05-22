package gui.auth;

import javax.swing.*;
import java.awt.geom.RoundRectangle2D;

public class AuthDialog extends JDialog {
    public AuthDialog() {
        setUndecorated(true);
        setTitle("Authentication");
        setModal(true);
        setSize(480, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Login", new LoginPanel());
        tabbedPane.addTab("Register", new RegisterPanel());

        add(tabbedPane);
    }
}
