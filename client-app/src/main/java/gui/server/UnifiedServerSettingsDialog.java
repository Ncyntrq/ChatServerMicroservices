package gui.server;

import gui.server.settings.ServerOverviewPanel;
import gui.server.settings.ServerRolesPanel;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.ServerApiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class UnifiedServerSettingsDialog extends JDialog {

    private final ServerApiClient serverApi = new ServerApiClient();
    private final long serverId;
    private final Runnable onChange;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    
    private final ServerRolesPanel rolesPanel;

    public UnifiedServerSettingsDialog(Window owner, long serverId, Runnable onChange) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.serverId = serverId;
        this.onChange = onChange;
        setUndecorated(true);
        setBackground(AppColors.BG_PRIMARY);

        if (owner != null) {
            setBounds(owner.getBounds());
            owner.addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) { setBounds(owner.getBounds()); }
                @Override public void componentMoved(ComponentEvent e) { setBounds(owner.getBounds()); }
            });
        } else {
            setSize(1024, 768);
            setLocationRelativeTo(null);
        }

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

        JLabel headerServer = new JLabel("SERVER SETTINGS");
        headerServer.setFont(AppFonts.CAPTION_BOLD);
        headerServer.setForeground(AppColors.TEXT_MUTED);
        headerServer.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarList.add(headerServer);
        sidebarList.add(Box.createVerticalStrut(8));

        sidebarList.add(createSidebarBtn("Overview", "Overview"));
        sidebarList.add(Box.createVerticalStrut(4));
        sidebarList.add(createSidebarBtn("Roles", "Roles"));
        sidebarList.add(Box.createVerticalStrut(4));
        
        JButton inviteBtn = createSidebarBtn("Generate Invite", "Invite");
        sidebarList.add(inviteBtn);

        sidebarList.add(Box.createVerticalStrut(20));
        JSeparator sep = new JSeparator();
        sep.setForeground(AppColors.SEPARATOR);
        sep.setMaximumSize(new Dimension(200, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarList.add(sep);
        sidebarList.add(Box.createVerticalStrut(20));

        JButton deleteBtn = createSidebarBtn("Delete Server", "Delete");
        deleteBtn.setForeground(AppColors.DANGER);
        sidebarList.add(deleteBtn);

        sidebarWrapper.add(sidebarList, BorderLayout.WEST);

        // --- Right Content Area ---
        cards.setBackground(AppColors.BG_PRIMARY);
        cards.setBorder(BorderFactory.createEmptyBorder(60, 40, 60, 40));

        cards.add(new ServerOverviewPanel(serverId, onChange), "Overview");
        
        rolesPanel = new ServerRolesPanel(serverId);
        cards.add(rolesPanel, "Roles");

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
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
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
                if (!"Delete Server".equals(text)) {
                    btn.setForeground(AppColors.TEXT_NORMAL);
                } else {
                    btn.setForeground(AppColors.DANGER);
                }
            }
        });

        btn.addActionListener(e -> {
            if ("Delete".equals(cardName)) {
                deleteServer();
            } else if ("Invite".equals(cardName)) {
                generateInvite();
            } else {
                cardLayout.show(cards, cardName);
                if ("Roles".equals(cardName)) rolesPanel.reloadRoles();
            }
        });

        return btn;
    }

    private void generateInvite() {
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return serverApi.createInviteCode(serverId);
            }
            @Override protected void done() {
                try {
                    String code = get();
                    new InviteCodeDialog(UnifiedServerSettingsDialog.this, code).setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UnifiedServerSettingsDialog.this, "Error generating invite: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void deleteServer() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this server permanently? This action cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        
        if (onChange != null) onChange.run(); // Broadcast before deleting
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                serverApi.deleteServer(serverId);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    if (onChange != null) onChange.run();
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UnifiedServerSettingsDialog.this, "Error deleting server", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
