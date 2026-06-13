package gui.auth;

import gui.theme.AppColors;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class AuthFrame extends JFrame {

    private final JPanel cards;
    private final CardLayout cardLayout;

    public AuthFrame() {
        super("Authentication");
        setUndecorated(true);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

        // Header containing close button (for the Right Panel)
        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        header.setBackground(AppColors.BG_PRIMARY);
        
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        closeBtn.setForeground(AppColors.TEXT_MUTED);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> System.exit(0));
        
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

        // The Right Panel (Forms)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(AppColors.BG_PRIMARY);
        rightPanel.add(header, BorderLayout.NORTH);
        rightPanel.add(cards, BorderLayout.CENTER);

        // The Root Panel (Split Screen)
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(new AuthGraphicPanel());
        root.add(rightPanel);

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
