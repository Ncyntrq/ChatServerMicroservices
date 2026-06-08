package gui.auth;

import gui.theme.AppColors;
import javax.swing.*;
import java.awt.CardLayout;
import java.awt.Window;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.geom.RoundRectangle2D;

public class AuthDialog extends JDialog {

    private final JPanel cards;
    private final CardLayout cardLayout;

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

        // Header containing close button
        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        header.setBackground(AppColors.BG_PRIMARY);
        
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        closeBtn.setForeground(AppColors.TEXT_MUTED);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        
        closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeBtn.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeBtn.setForeground(AppColors.TEXT_MUTED);
            }
        });
        
        header.add(closeBtn);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.add(new LoginPanel(this), "Login");
        cards.add(new RegisterPanel(this), "Register");

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppColors.BG_PRIMARY);
        root.add(header, BorderLayout.NORTH);
        root.add(cards, BorderLayout.CENTER);

        setContentPane(root);
    }

    /** Chuyển sang panel Login. */
    public void showLoginTab() {
        cardLayout.show(cards, "Login");
    }

    /** Chuyển sang panel Register. */
    public void showRegisterTab() {
        cardLayout.show(cards, "Register");
    }
}
