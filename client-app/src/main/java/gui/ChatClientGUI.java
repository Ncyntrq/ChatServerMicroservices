package gui;

import gui.components.chat.ChatMessageItem;
import gui.components.chat.UserListItem;
import gui.components.chat.SidebarCategoryHeader;
import gui.components.chat.ChatInputContainer;
import gui.theme.AppColors;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

public class ChatClientGUI extends JFrame {
    private final JPanel chatHistoryPanel;
    private final JPanel sidebarPanel;
    private final JPanel sidebarListPanel;
    private final JScrollPane chatScrollPane; // Elevated to class level for auto-scrolling
    private final String sessionUsername;

    public ChatClientGUI(String sessionUsername) {
        setTitle("Chat Server v2.0");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        this.sessionUsername = sessionUsername;

        // --- 1. CHAT HISTORY AREA (CENTER) ---
        chatHistoryPanel = new JPanel();
        chatHistoryPanel.setLayout(new BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS));
        chatHistoryPanel.setBackground(AppColors.BG_PRIMARY);

        // Add an initial invisible glue so the first messages start at the top, not the middle
        chatHistoryPanel.add(Box.createVerticalGlue());

        chatScrollPane = new JScrollPane(chatHistoryPanel);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // --- 2. RIGHT SIDEBAR: ONLINE USERS (EAST) ---
        sidebarListPanel = new JPanel();
        sidebarListPanel.setLayout(new BoxLayout(sidebarListPanel, BoxLayout.Y_AXIS));
        sidebarListPanel.setBackground(AppColors.BG_SECONDARY);

        sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBackground(AppColors.BG_SECONDARY);
        sidebarPanel.add(sidebarListPanel, BorderLayout.NORTH); // THE MAGIC PIN!

        JScrollPane sidebarScroll = new JScrollPane(sidebarPanel);
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());
        sidebarScroll.setPreferredSize(new Dimension(250, 0));
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(16);

        // --- 3. INPUT AREA (SOUTH) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(AppColors.BG_PRIMARY);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // Injecting our new Custom Component!
        ChatInputContainer chatInput = new ChatInputContainer();

        // Mock Send Event: Wires the input box directly to the chat history for testing
        chatInput.getSendButton().addActionListener(e -> {
            String text = chatInput.getMessageText();
            if (!text.trim().isEmpty()) {
                MessageDTO myMsg = new MessageDTO(
                        MessageType.CHAT,
                        sessionUsername,
                        null,
                        text,
                        LocalDateTime.now()
                );
                appendMessage(myMsg);
                chatInput.clearInput();
            }
        });

        // Pressing 'Enter' triggers the send button
        chatInput.getInputField().addActionListener(e -> chatInput.getSendButton().doClick());

        bottomPanel.add(chatInput, BorderLayout.CENTER);

        // --- 4. ASSEMBLE ---
        add(chatScrollPane, BorderLayout.CENTER);
        add(sidebarScroll, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ==========================================
    // PUBLIC API FOR DATA INJECTION
    // ==========================================

    /**
     * Appends a new message to the bottom of the chat view.
     */
    public void appendMessage(MessageDTO message) {
        boolean isHighlighted = message.getContent() != null &&
                message.getContent().contains("@" + sessionUsername);

        // Insert the new message right BEFORE the VerticalGlue (which sits at the very end)
        int insertIndex = chatHistoryPanel.getComponentCount() - 1;

        chatHistoryPanel.add(new ChatMessageItem(message, isHighlighted), insertIndex);
        chatHistoryPanel.add(Box.createVerticalStrut(10), insertIndex + 1);

        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();

        // Auto-scroll to bottom (Requires invokeLater because the UI needs a millisecond to calculate the new height)
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    /**
     * Clears and repopulates the online users sidebar.
     */
    public void setOnlineUsers(List<String> usernames) {
        sidebarListPanel.removeAll();

        sidebarListPanel.add(Box.createVerticalStrut(15));
        sidebarListPanel.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + usernames.size()));
        sidebarListPanel.add(Box.createVerticalStrut(5));

        for (String username : usernames) {
            sidebarListPanel.add(new UserListItem(username, null, AppColors.STATUS_ONLINE));
        }

        sidebarListPanel.revalidate();
        sidebarListPanel.repaint();
    }
}