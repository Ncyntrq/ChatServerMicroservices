package gui.profile;

import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.SessionManager;
import network.UserProfileApiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Dialog cài đặt user dạng overlay toàn màn hình, sidebar trái:
 * My Account / Privacy & Safety / Status / Appearance (theme) / Log Out.
 */
public class UserSettingsDialog extends JDialog {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public UserSettingsDialog(Window owner, gui.components.PresenceStatusIcon.Status initialStatus, Runnable onProfileChanged) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setBackground(AppColors.BG_PRIMARY);
        
        // Track owner bounds to simulate a full-screen overlay
        if (owner != null) {
            setBounds(owner.getBounds());
            owner.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) { setBounds(owner.getBounds()); }
                @Override
                public void componentMoved(ComponentEvent e) { setBounds(owner.getBounds()); }
            });
        } else {
            setSize(1024, 768);
            setLocationRelativeTo(null);
        }

        String username = SessionManager.get().getUsername();
        UserProfileApiClient profileApi = new UserProfileApiClient();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppColors.BG_PRIMARY);

        // --- Left Sidebar ---
        JPanel sidebarWrapper = new JPanel(new BorderLayout());
        sidebarWrapper.setBackground(AppColors.BG_SECONDARY);
        sidebarWrapper.setPreferredSize(new Dimension(240, 0));

        JPanel sidebarList = new JPanel();
        sidebarList.setLayout(new BoxLayout(sidebarList, BoxLayout.Y_AXIS));
        sidebarList.setOpaque(false);
        sidebarList.setBorder(BorderFactory.createEmptyBorder(60, 20, 20, 20));

        JLabel headerUser = new JLabel("USER SETTINGS");
        headerUser.setFont(AppFonts.CAPTION_BOLD);
        headerUser.setForeground(AppColors.TEXT_MUTED);
        headerUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarList.add(headerUser);
        sidebarList.add(Box.createVerticalStrut(8));

        sidebarList.add(createSidebarBtn("My Account", "Profile"));
        sidebarList.add(Box.createVerticalStrut(4));
        sidebarList.add(createSidebarBtn("Privacy & Safety", "Security"));
        sidebarList.add(Box.createVerticalStrut(4));
        sidebarList.add(createSidebarBtn("Status", "Status"));
        sidebarList.add(Box.createVerticalStrut(4));
        sidebarList.add(createSidebarBtn("Appearance", "Appearance"));

        sidebarList.add(Box.createVerticalStrut(20));
        JSeparator sep = new JSeparator();
        sep.setForeground(AppColors.SEPARATOR);
        sep.setMaximumSize(new Dimension(200, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarList.add(sep);
        sidebarList.add(Box.createVerticalStrut(20));

        JButton logoutBtn = createSidebarBtn("Log Out", "Logout");
        logoutBtn.setForeground(AppColors.DANGER);
        sidebarList.add(logoutBtn);

        sidebarWrapper.add(sidebarList, BorderLayout.WEST);

        // --- Right Content Area ---
        cards.setBackground(AppColors.BG_PRIMARY);
        cards.setBorder(BorderFactory.createEmptyBorder(60, 40, 60, 40));
        
        ProfileEditPanel pep = new ProfileEditPanel(username, profileApi, onProfileChanged);
        
        cards.add(pep, "Profile");
        cards.add(new AccountSecurityPanel(), "Security");
        cards.add(new StatusPanel(profileApi, initialStatus), "Status");
        cards.add(new AppearancePanel(this), "Appearance");

        // --- ESC Close Button ---
        JPanel escWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 40, 60));
        escWrapper.setOpaque(false);
        escWrapper.setPreferredSize(new Dimension(160, 0));
        JButton escBtn = new JButton("<html><div style='text-align:center;'><div style='font-size:24px; border:2px solid #8B92A0; border-radius:50%; padding:2px 8px;'>✕</div><div style='font-size:11px; margin-top:4px;'>ESC</div></div></html>");
        escBtn.setFont(AppFonts.BODY_BOLD);
        escBtn.setForeground(AppColors.TEXT_MUTED);
        escBtn.setFocusPainted(false);
        escBtn.setContentAreaFilled(false);
        escBtn.setBorderPainted(false);
        escBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        escBtn.addActionListener(e -> dispose());
        escBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { escBtn.setForeground(Color.WHITE); }
            public void mouseExited(java.awt.event.MouseEvent evt) { escBtn.setForeground(AppColors.TEXT_MUTED); }
        });
        escWrapper.add(escBtn);

        // Assemble Right Side
        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(AppColors.BG_PRIMARY);
        rightSide.add(cards, BorderLayout.CENTER);
        rightSide.add(escWrapper, BorderLayout.EAST);

        root.add(sidebarWrapper, BorderLayout.WEST);
        root.add(rightSide, BorderLayout.CENTER);

        // Key bindings for ESC
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "closeSettings");
        root.getActionMap().put("closeSettings", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });

        setContentPane(root);
    }

    private JButton createSidebarBtn(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(AppFonts.BODY_BOLD);
        btn.setForeground(AppColors.TEXT_NORMAL);
        btn.setBackground(AppColors.BG_SECONDARY);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(200, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setContentAreaFilled(true);
                btn.setBackground(AppColors.BG_TERTIARY);
                btn.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setContentAreaFilled(false);
                if (!"Log Out".equals(text)) {
                    btn.setForeground(AppColors.TEXT_NORMAL);
                } else {
                    btn.setForeground(AppColors.DANGER);
                }
            }
        });

        btn.addActionListener(e -> {
            if ("Logout".equals(cardName)) {
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            try {
                                network.PresenceApiClient pApi = new network.PresenceApiClient();
                                pApi.updatePresenceStatus("ONLINE");
                                network.UserProfileApiClient uApi = new network.UserProfileApiClient();
                                uApi.updateStatus("");
                            } catch (Exception ignore) {}
                            return null;
                        }
                        @Override
                        protected void done() {
                            SessionManager.get().clear();
                            Window owner = SwingUtilities.getWindowAncestor(gui.profile.UserSettingsDialog.this);
                            if (owner instanceof gui.ChatClientGUI) {
                                try { ((gui.ChatClientGUI) owner).disconnect(); } catch (Exception ignore) {}
                            }
                            if (owner != null) {
                                owner.dispose();
                            }
                            new gui.auth.AuthFrame().setVisible(true);
                            dispose();
                        }
                    }.execute();
                }
            } else {
                cardLayout.show(cards, cardName);
            }
        });

        return btn;
    }
}
