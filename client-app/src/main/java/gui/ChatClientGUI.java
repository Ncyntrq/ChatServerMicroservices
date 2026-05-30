package gui;

import gui.components.channels.ChannelSidebar;
import gui.components.chat.ChatMessageItem;
import gui.components.chat.UserListItem;
import gui.components.chat.SidebarCategoryHeader;
import gui.components.chat.ChatInputContainer;
import gui.components.chat.IconButton;
import gui.components.mini.MiniSidebar;
import gui.components.navigation.ServerSidebar;
import gui.theme.AppColors;
import network.ApiConfig;
import network.ChannelApiClient;
import network.ChatWebSocketClient;
import network.PresenceApiClient;
import network.SessionManager;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.util.List;

public class ChatClientGUI extends JFrame {
    private final JPanel chatHistoryPanel;
    private final JPanel sidebarListPanel;
    private final JScrollPane chatScrollPane;
    private final String sessionUsername;

    private final ChatWebSocketClient wsClient = new ChatWebSocketClient();
    private final ChannelApiClient channelApi = new ChannelApiClient();
    private final PresenceApiClient presenceApi = new PresenceApiClient();

    private final ServerSidebar serverSidebar = new ServerSidebar();
    private final ChannelSidebar channelSidebar;
    private final MiniSidebar miniSidebar = new MiniSidebar();

    private long activeServerId = ApiConfig.DEFAULT_SERVER_ID;
    private long activeChannelId = ApiConfig.DEFAULT_CHANNEL_ID;

    public ChatClientGUI(String sessionUsername) {
        setTitle("Chat Server v2.0 — " + sessionUsername);
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        this.sessionUsername = sessionUsername;
        this.channelSidebar = new ChannelSidebar(sessionUsername);

        // --- WEST: ServerSidebar (72px) + ChannelSidebar (240px) ---
        serverSidebar.setOnServerSelected(this::onServerSelected);
        channelSidebar.setOnChannelSelected(this::onChannelSelected);

        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.add(serverSidebar, BorderLayout.WEST);
        westPanel.add(channelSidebar, BorderLayout.CENTER);

        // --- CENTER: chat history ---
        chatHistoryPanel = new JPanel();
        chatHistoryPanel.setLayout(new BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS));
        chatHistoryPanel.setBackground(AppColors.BG_PRIMARY);
        chatHistoryPanel.add(Box.createVerticalGlue());

        chatScrollPane = new JScrollPane(chatHistoryPanel);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // --- EAST: online members ---
        sidebarListPanel = new JPanel();
        sidebarListPanel.setLayout(new BoxLayout(sidebarListPanel, BoxLayout.Y_AXIS));
        sidebarListPanel.setBackground(AppColors.BG_SECONDARY);

        JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBackground(AppColors.BG_SECONDARY);
        sidebarPanel.add(sidebarListPanel, BorderLayout.NORTH);

        JScrollPane sidebarScroll = new JScrollPane(sidebarPanel);
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());
        sidebarScroll.setPreferredSize(new Dimension(240, 0));
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(16);

        // MiniSidebar (ẩn ban đầu) — bạn bè online + server đã join
        miniSidebar.setVisible(false);
        miniSidebar.setOnFriendSelected(this::openDirectMessage);
        miniSidebar.setOnServerSelected((id, name) -> {
            this.activeServerId = id;
            channelSidebar.loadChannels(id, name);
        });

        JPanel eastContainer = new JPanel(new BorderLayout());
        eastContainer.setBackground(AppColors.BG_SECONDARY);
        eastContainer.add(sidebarScroll, BorderLayout.CENTER);
        eastContainer.add(miniSidebar, BorderLayout.EAST);

        // --- SOUTH: input + toggle MiniSidebar ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(AppColors.BG_PRIMARY);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        IconButton toggleMiniBtn = new IconButton("👥", e -> {
            boolean show = !miniSidebar.isVisible();
            miniSidebar.setVisible(show);
            if (show) miniSidebar.refresh();
            eastContainer.revalidate();
            eastContainer.repaint();
        });
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbar.setOpaque(false);
        toolbar.add(toggleMiniBtn);
        bottomPanel.add(toolbar, BorderLayout.WEST);

        ChatInputContainer chatInput = new ChatInputContainer();
        chatInput.getSendButton().addActionListener(e -> sendChatFromInput(chatInput));
        chatInput.getInputField().addActionListener(e -> sendChatFromInput(chatInput));
        bottomPanel.add(chatInput, BorderLayout.CENTER);

        add(westPanel, BorderLayout.WEST);
        add(chatScrollPane, BorderLayout.CENTER);
        add(eastContainer, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                wsClient.close();
            }
        });
    }

    /** Bắt đầu phiên: load servers, presence, connect WS, load channel mặc định. */
    public void startSession() {
        serverSidebar.loadServers();
        channelSidebar.loadChannels(activeServerId, "Server #" + activeServerId);
        loadPresence();
        connectWebSocket();
        switchToChannel(activeChannelId);
    }

    // ---------------------------------------------------------------
    // Selection handlers
    // ---------------------------------------------------------------

    private void onServerSelected(long serverId) {
        this.activeServerId = serverId;
        channelSidebar.loadChannels(serverId, "Server #" + serverId);
    }

    private void onChannelSelected(long channelId) {
        switchToChannel(channelId);
    }

    /** Chuyển channel: clear chat, load lịch sử mới. WS giữ nguyên kết nối. */
    private void switchToChannel(long channelId) {
        this.activeChannelId = channelId;
        clearChat();
        appendSystem("Đang tải lịch sử kênh #" + channelId + "...");
        new SwingWorker<List<MessageDTO>, Void>() {
            @Override
            protected List<MessageDTO> doInBackground() {
                return channelApi.fetchRecentMessages(channelId, 50);
            }

            @Override
            protected void done() {
                try {
                    List<MessageDTO> history = get();
                    for (MessageDTO m : history) appendMessage(m);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    appendSystem("Không tải được lịch sử: " + cause.getMessage());
                }
            }
        }.execute();
    }

    /** Mở DM với 1 người bạn (tính năng DM sẽ hoàn thiện sau). */
    private void openDirectMessage(String username) {
        JOptionPane.showMessageDialog(this,
                "Tin nhắn riêng (DM) với " + username + " — tính năng sắp ra mắt.",
                "Direct Message", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadPresence() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return presenceApi.getOnlineUsers();
            }

            @Override
            protected void done() {
                try {
                    setOnlineUsers(get());
                } catch (Exception ex) {
                    // bỏ qua — danh sách online sẽ cập nhật khi có tin LIST từ WS
                }
            }
        }.execute();
    }

    private void connectWebSocket() {
        String token = SessionManager.get().getAccessToken();
        if (token == null) {
            appendSystem("Thiếu JWT — vui lòng đăng nhập lại");
            return;
        }

        wsClient.setOnMessage(msg -> SwingUtilities.invokeLater(() -> handleIncoming(msg)));
        wsClient.setOnError(err -> SwingUtilities.invokeLater(() -> appendSystem(err)));
        wsClient.setOnClose(() -> SwingUtilities.invokeLater(() -> appendSystem("Mất kết nối tới server")));

        appendSystem("Đang kết nối WebSocket...");
        wsClient.connect(token).whenComplete((v, err) -> SwingUtilities.invokeLater(() -> {
            if (err != null) {
                appendSystem("Lỗi WebSocket: " + err.getMessage());
            } else {
                appendSystem("Đã kết nối. Server #" + activeServerId);
            }
        }));
    }

    private void sendChatFromInput(ChatInputContainer chatInput) {
        String text = chatInput.getMessageText();
        if (text == null || text.trim().isEmpty()) return;

        if (!wsClient.isOpen()) {
            appendSystem("WebSocket chưa sẵn sàng, tin nhắn chưa được gửi");
            return;
        }

        MessageDTO out = new MessageDTO(MessageType.CHAT, sessionUsername, null, text, LocalDateTime.now());
        out.setServerId(activeServerId);
        out.setChannelId(activeChannelId);

        wsClient.send(out).whenComplete((ws, err) -> {
            if (err != null) {
                SwingUtilities.invokeLater(() -> appendSystem("Gửi thất bại: " + err.getMessage()));
            }
        });
        chatInput.clearInput();
    }

    /** Phân loại message đến và lọc theo channel đang mở. */
    private void handleIncoming(MessageDTO msg) {
        if (msg.getType() == null) {
            if (belongsToActiveChannel(msg)) appendMessage(msg);
            return;
        }
        switch (msg.getType()) {
            case CHAT, PRIVATE, EDIT, DELETE -> {
                if (belongsToActiveChannel(msg)) appendMessage(msg);
            }
            case JOIN, LEAVE, SYSTEM -> appendSystem(msg.getContent());
            case ERROR -> appendSystem("⚠ " + msg.getContent());
            case TYPING, PING, PONG -> { /* ignore */ }
            case LIST -> {
                if (msg.getContent() != null) {
                    setOnlineUsers(List.of(msg.getContent().split(",")));
                }
            }
            default -> {
                if (belongsToActiveChannel(msg)) appendMessage(msg);
            }
        }
    }

    /** Chỉ hiển thị tin của channel đang mở (nếu message có channelId). */
    private boolean belongsToActiveChannel(MessageDTO msg) {
        return msg.getChannelId() == null || msg.getChannelId() == activeChannelId;
    }

    private void appendSystem(String text) {
        MessageDTO sys = new MessageDTO(MessageType.SYSTEM, "SYSTEM", null, text, LocalDateTime.now());
        appendMessage(sys);
    }

    /** Xóa toàn bộ lịch sử chat đang hiển thị (khi chuyển channel). */
    private void clearChat() {
        chatHistoryPanel.removeAll();
        chatHistoryPanel.add(Box.createVerticalGlue());
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
    }

    public void appendMessage(MessageDTO message) {
        boolean isHighlighted = message.getContent() != null &&
                message.getContent().contains("@" + sessionUsername);

        int insertIndex = chatHistoryPanel.getComponentCount() - 1;
        chatHistoryPanel.add(new ChatMessageItem(message, isHighlighted), insertIndex);
        chatHistoryPanel.add(Box.createVerticalStrut(10), insertIndex + 1);

        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public void setOnlineUsers(List<String> usernames) {
        sidebarListPanel.removeAll();
        sidebarListPanel.add(Box.createVerticalStrut(15));
        sidebarListPanel.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + usernames.size()));
        sidebarListPanel.add(Box.createVerticalStrut(5));

        for (String username : usernames) {
            if (username == null || username.isBlank()) continue;
            sidebarListPanel.add(new UserListItem(username.trim(), null, AppColors.STATUS_ONLINE));
        }

        sidebarListPanel.revalidate();
        sidebarListPanel.repaint();
    }
}
