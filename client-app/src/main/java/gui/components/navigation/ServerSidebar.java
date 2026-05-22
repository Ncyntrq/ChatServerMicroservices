package gui.components.navigation;

import gui.theme.AppColors;
import javax.swing.*;
import java.awt.*;

public class ServerSidebar extends JPanel {
    private final JPanel listPanel;

    public ServerSidebar() {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_TERTIARY);
        setPreferredSize(new Dimension(72, 0));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_TERTIARY);

        // --- 1. HOME ICON (Direct Messages Context) ---
        listPanel.add(Box.createVerticalStrut(10));
        ServerIconItem homeBtn = new ServerIconItem("💬"); // Chat bubble icon for DMs
        homeBtn.setActive(true);
        homeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPanel.add(homeBtn);

        // --- 2. SEPARATOR LINE ---
        listPanel.add(Box.createVerticalStrut(5));
        JPanel separator = new JPanel() {
            @Override public Dimension getPreferredSize() { return new Dimension(32, 2); } // ADD
            @Override public Dimension getMinimumSize()   { return new Dimension(32, 2); } // ADD
            @Override public Dimension getMaximumSize()   { return new Dimension(32, 2); }
        };
        separator.setAlignmentX(Component.CENTER_ALIGNMENT); // ADD THIS
        separator.setBackground(AppColors.BG_HOVER);
        separator.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPanel.add(separator);
        listPanel.add(Box.createVerticalStrut(5));

        // --- 3. SERVER CHANNELS ICONS ---
        ServerIconItem srv1 = new ServerIconItem("🛠️"); // Wrench icon for team server
        srv1.setHasUnread(true);
        srv1.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPanel.add(srv1);

        ServerIconItem srv2 = new ServerIconItem("🚀"); // Rocket icon for alternate environment
        srv2.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPanel.add(srv2);

        // --- 4. ACTION INTERFACE (Add New Server) ---
        listPanel.add(Box.createVerticalStrut(5));
        ServerIconItem addBtn = new ServerIconItem("➕"); // Plus icon
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPanel.add(addBtn);

        listPanel.add(Box.createVerticalGlue());
        add(listPanel, BorderLayout.CENTER);
    }
}