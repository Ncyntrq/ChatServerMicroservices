package gui.components.channels;

import gui.components.chat.SidebarCategoryHeader;
import gui.theme.AppColors;
import javax.swing.*;
import java.awt.*;

public class ChannelSidebar extends JPanel {

    public ChannelSidebar(String sessionUsername) {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_SECONDARY);
        setPreferredSize(new Dimension(240, 0)); // Fixed 240px width layout

        // --- 1. TOP HEADER: SERVER NAME ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppColors.BG_SECONDARY);
        headerPanel.setPreferredSize(new Dimension(240, 48));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BG_PRIMARY));

        JLabel titleLabel = new JLabel("Nhóm Cứng - Workspace");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(AppColors.TEXT_WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // --- 2. MIDDLE: CHANNELS LIST (WITH NORTH PIN) ---
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_SECONDARY);
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        // Text Channels Category
        listPanel.add(new SidebarCategoryHeader("KÊNH CHỮ"));
        listPanel.add(Box.createVerticalStrut(4));
        listPanel.add(new ChannelListItem("thao-luan-chung", false));
        listPanel.add(Box.createVerticalStrut(4));
        listPanel.add(new ChannelListItem("tai-lieu-giao-dien", false));
        listPanel.add(Box.createVerticalStrut(16));

        // Voice Channels Category
        listPanel.add(new SidebarCategoryHeader("KÊNH THOẠI"));
        listPanel.add(Box.createVerticalStrut(4));
        listPanel.add(new ChannelListItem("Phòng Họp Midterm", true));

        // This wrapper panel forces the listPanel components to stay pinned at the top
        JPanel scrollContentWrapper = new JPanel(new BorderLayout());
        scrollContentWrapper.setBackground(AppColors.BG_SECONDARY);
        scrollContentWrapper.add(listPanel, BorderLayout.NORTH); // THE MAGIC PIN!

        JScrollPane scrollPane = new JScrollPane(scrollContentWrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // --- 3. BOTTOM: ACCOUNT FOOTER BAR ---
        UserFooterPanel accountFooter = new UserFooterPanel(sessionUsername);

        // Assemble the structural layers
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(accountFooter, BorderLayout.SOUTH);
    }
}