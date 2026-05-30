package gui.auth;

import javax.swing.*;
import java.awt.Window;
import java.awt.geom.RoundRectangle2D;

public class AuthDialog extends JDialog {

    private final JTabbedPane tabbedPane;

    public AuthDialog() {
        this((Window) null);
    }

    public AuthDialog(Window owner) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setTitle("Authentication");
        setModal(true);
        setSize(480, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Login", new LoginPanel(this));
        tabbedPane.addTab("Register", new RegisterPanel(this));

        add(tabbedPane);
    }

    /** Chuyển sang tab Login (index 0). */
    public void showLoginTab() {
        tabbedPane.setSelectedIndex(0);
    }

    /** Chuyển sang tab Register (index 1). */
    public void showRegisterTab() {
        tabbedPane.setSelectedIndex(1);
    }
}
