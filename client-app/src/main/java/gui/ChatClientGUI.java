package gui;

import gui.components.channels.ChannelSidebar;
import gui.components.chat.ChatMessageItem;
import gui.components.chat.UserListItem;
import gui.components.chat.SidebarCategoryHeader;
import gui.components.chat.ChatInputContainer;
import gui.components.chat.IconButton;
import gui.components.friends.FriendSidebar;
import gui.components.mini.MiniSidebar;
import gui.components.navigation.ServerSidebar;
import gui.theme.AppColors;
import network.ApiConfig;
import network.ChannelApiClient;
import network.ChatWebSocketClient;
import network.PresenceApiClient;
import network.ServerApiClient;
import network.SessionManager;
import network.PrivateMessageApiClient;
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
    private final ServerApiClient serverApi = new ServerApiClient();
    private final PrivateMessageApiClient privateMessageApi = new PrivateMessageApiClient();
    private final network.NotificationApiClient notificationApi = new network.NotificationApiClient();

    private final ServerSidebar serverSidebar = new ServerSidebar();
    private final ChannelSidebar channelSidebar;
    private final FriendSidebar friendSidebar;
    private final MiniSidebar miniSidebar = new MiniSidebar();
    private final JPanel westPanel;
    private final JPanel eastContainer;
    private final ChatInputContainer chatInput = new ChatInputContainer();

    private long activeServerId = -1;
    private long activeChannelId = -1;
    private String activePrivateUser = null;
    private String lastSender = null;

    public ChatClientGUI(String sessionUsername) {
        setTitle("Chat Server v2.0 — " + sessionUsername);
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        this.sessionUsername = sessionUsername;
        this.channelSidebar = new ChannelSidebar(sessionUsername);
        this.friendSidebar = new FriendSidebar(sessionUsername);

        // --- WEST: ServerSidebar (72px) + Center (Sidebar Tương ứng) ---
        serverSidebar.setOnServerSelected(this::onServerSelected);
        channelSidebar.setOnChannelSelected(this::onChannelSelected);
        friendSidebar.setOnFriendSelected(this::openDirectMessage);
        
        // Gửi WebSocket "thông báo" ngầm tới người kia khi thao tác bạn bè
        friendSidebar.setOnFriendAction(targetUser -> {
            if (wsClient.isOpen()) {
                MessageDTO out = new MessageDTO(MessageType.PRIVATE, sessionUsername, null, "[SYSTEM_FRIEND_UPDATE]", LocalDateTime.now());
                out.setReceiver(targetUser);
                wsClient.send(out);
            }
        });

        westPanel = new JPanel(new BorderLayout());
        westPanel.add(serverSidebar, BorderLayout.WEST);
        // Ban đầu là trang chủ
        westPanel.add(friendSidebar, BorderLayout.CENTER);

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

        eastContainer = new JPanel(new BorderLayout());
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
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setOpaque(false);
        toolbar.add(toggleMiniBtn);
        bottomPanel.add(toolbar, BorderLayout.WEST);

        chatInput.setVisible(false); // Ẩn ban đầu
        chatInput.getSendButton().addActionListener(e -> sendChatFromInput());
        chatInput.getInputField().addActionListener(e -> sendChatFromInput());
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

    /** Bắt đầu phiên: load servers, presence, connect WS, load trang chủ. */
    public void startSession() {
        serverSidebar.loadServers();
        onServerSelected(-1, null); // Gọi trực tiếp hàm xử lý logic Trang chủ
        connectWebSocket();
        refreshUnreadCounts();
    }
    
    private void refreshUnreadCounts() {
        new SwingWorker<java.util.Map<String, Object>, Void>() {
            @Override
            protected java.util.Map<String, Object> doInBackground() {
                try {
                    return notificationApi.getUnreadCounts(sessionUsername);
                } catch (Exception e) {
                    return null;
                }
            }
            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    java.util.Map<String, Object> resp = get();
                    if (resp == null) return;
                    
                    if (resp.get("unreadCounts") != null) {
                        java.util.Map<String, Number> unreadMap = (java.util.Map<String, Number>) resp.get("unreadCounts");
                        java.util.Map<Long, Integer> channelCounts = new java.util.HashMap<>();
                        for (java.util.Map.Entry<String, Number> entry : unreadMap.entrySet()) {
                            channelCounts.put(Long.parseLong(entry.getKey()), entry.getValue().intValue());
                        }
                        channelSidebar.updateUnreadCounts(channelCounts);
                    }
                    
                    if (resp.get("privateCounts") != null) {
                        java.util.Map<String, Number> privateMap = (java.util.Map<String, Number>) resp.get("privateCounts");
                        java.util.Map<String, Integer> friendCounts = new java.util.HashMap<>();
                        for (java.util.Map.Entry<String, Number> entry : privateMap.entrySet()) {
                            friendCounts.put(entry.getKey(), entry.getValue().intValue());
                        }
                        friendSidebar.updateUnreadCounts(friendCounts);
                    }
                } catch (Exception ignore) {}
            }
        }.execute();
    }

    private void ackMessage(MessageDTO msg) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                if (msg.getType() == MessageType.PRIVATE) {
                    notificationApi.ackDm(msg.getSender(), sessionUsername);
                } else if (msg.getChannelId() != null) {
                    notificationApi.ackChannel(msg.getChannelId(), sessionUsername);
                }
                return null;
            }
        }.execute();
    }

    // ---------------------------------------------------------------
    // Selection handlers
    // ---------------------------------------------------------------

    private void onServerSelected(long serverId, String serverName) {
        this.activeServerId = serverId;
        westPanel.remove(((BorderLayout) westPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER));
        
        if (serverId == -1) {
            this.activeChannelId = -1;
            this.activePrivateUser = null;
            westPanel.add(friendSidebar, BorderLayout.CENTER);
            eastContainer.setVisible(false); // Ẩn thanh thành viên
            chatInput.setVisible(false); // Ẩn thanh nhập tin nhắn khi ở Home
            clearChat();
            setOnlineUsers(List.of()); 
            loadPresence(); 
        } else {
            this.activePrivateUser = null;
            this.activeChannelId = -1;
            westPanel.add(channelSidebar, BorderLayout.CENTER);
            eastContainer.setVisible(true); // Hiện thanh thành viên
            chatInput.setVisible(false); // Ẩn cho đến khi chọn channel
            channelSidebar.loadChannels(serverId, serverName != null ? serverName : "Server #" + serverId);
            clearChat();
            loadServerMembersAndPresence(serverId);
        }
        westPanel.revalidate();
        westPanel.repaint();
    }

    private void onChannelSelected(long channelId) {
        switchToChannel(channelId);
    }

    /** Chuyển channel: clear chat, load lịch sử mới. WS giữ nguyên kết nối. */
    private void switchToChannel(long channelId) {
        this.activeChannelId = channelId;
        chatInput.setVisible(true); // Hiện thanh nhập tin nhắn
        clearChat();
        
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { notificationApi.ackChannel(channelId, sessionUsername); return null; }
            @Override protected void done() { refreshUnreadCounts(); }
        }.execute();

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

    /** Mở DM với 1 người bạn. */
    private void openDirectMessage(String username) {
        this.activeChannelId = -1;
        this.activePrivateUser = username;
        chatInput.setVisible(true); // Hiện thanh nhập tin nhắn
        clearChat();
        
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { notificationApi.ackDm(username, sessionUsername); return null; }
            @Override protected void done() { refreshUnreadCounts(); }
        }.execute();

        new SwingWorker<List<MessageDTO>, Void>() {
            @Override
            protected List<MessageDTO> doInBackground() {
                return privateMessageApi.fetchPrivateMessages(username, 50);
            }

            @Override
            protected void done() {
                try {
                    List<MessageDTO> history = get();
                    for (MessageDTO m : history) {
                        if (!"[SYSTEM_FRIEND_UPDATE]".equals(m.getContent())) {
                            appendMessage(m);
                        }
                    }
                } catch (Exception ex) {
                    appendSystem("Không tải được lịch sử: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadPresence() {
        if (activeServerId <= 0) {
            // Loading friends presence
            new SwingWorker<List<String>, Void>() {
                @Override
                protected List<String> doInBackground() {
                    return presenceApi.getOnlineUsers();
                }
                @Override
                protected void done() {
                    try {
                        friendSidebar.loadFriendsAndRequests(get());
                    } catch (Exception ignore) {}
                }
            }.execute();
            return;
        }
        loadServerMembersAndPresence(activeServerId);
    }

    private void loadServerMembersAndPresence(long serverId) {
        new SwingWorker<java.util.Map<String, Object>, Void>() {
            @Override
            protected java.util.Map<String, Object> doInBackground() {
                java.util.Map<String, Object> details = serverApi.getServerDetails(serverId);
                List<String> online = presenceApi.getOnlineUsers();
                return java.util.Map.of("details", details, "online", online);
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    java.util.Map<String, Object> result = get();
                    java.util.Map<String, Object> details = (java.util.Map<String, Object>) result.get("details");
                    List<String> online = (List<String>) result.get("online");
                    
                    List<java.util.Map<String, Object>> members = (List<java.util.Map<String, Object>>) details.get("members");
                    List<String> allUsers = new java.util.ArrayList<>();
                    if (members != null) {
                        for (java.util.Map<String, Object> m : members) {
                            Object uid = m.get("userId");
                            if (uid != null) allUsers.add(uid.toString());
                        }
                    }
                    java.util.Map<String, Object> serverData = (java.util.Map<String, Object>) details.get("server");
                    if (serverData == null) serverData = details;
                    String ownerId = String.valueOf(serverData.get("ownerId"));

                    renderServerMembers(allUsers, online, ownerId);
                } catch (Exception ex) {
                    // fall back to previous behavior on error
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
        wsClient.setOnClose(() -> SwingUtilities.invokeLater(() -> {}));

        wsClient.connect(token).whenComplete((v, err) -> SwingUtilities.invokeLater(() -> {
            if (err != null) {
                appendSystem("Lỗi WebSocket: " + err.getMessage());
            }
        }));
    }

    private void sendChatFromInput() {
        String text = chatInput.getMessageText();
        if (text == null || text.trim().isEmpty()) return;

        if (!wsClient.isOpen()) {
            appendSystem("WebSocket chưa sẵn sàng, tin nhắn chưa được gửi");
            return;
        }

        MessageDTO out;
        if (activeChannelId == -1 && activePrivateUser != null) {
            // Private message
            out = new MessageDTO(MessageType.PRIVATE, sessionUsername, null, text, LocalDateTime.now());
            out.setReceiver(activePrivateUser);
        } else {
            // Channel message
            out = new MessageDTO(MessageType.CHAT, sessionUsername, null, text, LocalDateTime.now());
            out.setServerId(activeServerId);
            out.setChannelId(activeChannelId);
        }

        wsClient.send(out).whenComplete((ws, err) -> {
            if (err != null) {
                SwingUtilities.invokeLater(() -> appendSystem("Gửi thất bại: " + err.getMessage()));
            }
        });
        chatInput.clearInput();
    }

    /** Phân loại message đến và lọc theo channel đang mở. */
    private void handleIncoming(MessageDTO msg) {
        if (msg.getType() == MessageType.PRIVATE && "[SYSTEM_FRIEND_UPDATE]".equals(msg.getContent())) {
            // Tải lại danh sách bạn bè mà không in ra giao diện
            if (activeServerId == -1) loadPresence();
            return;
        }

        if (msg.getType() == null) {
            if (belongsToActiveChannel(msg)) {
                appendMessage(msg);
                ackMessage(msg);
            } else {
                refreshUnreadCounts();
            }
            return;
        }
        switch (msg.getType()) {
            case CHAT, PRIVATE, EDIT, DELETE -> {
                if (belongsToActiveChannel(msg)) {
                    appendMessage(msg);
                    if (msg.getType() == MessageType.CHAT || msg.getType() == MessageType.PRIVATE) {
                        ackMessage(msg);
                    }
                } else {
                    refreshUnreadCounts();
                }
            }
            case JOIN, LEAVE -> loadPresence();
            case SYSTEM -> appendSystem(msg.getContent());
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

    /** Chỉ hiển thị tin của channel đang mở, hoặc tin private. */
    private boolean belongsToActiveChannel(MessageDTO msg) {
        if (msg.getType() == MessageType.PRIVATE) {
            if (activePrivateUser == null) return false;
            // Nếu người gửi là bạn bè hoặc chính mình gửi cho bạn bè
            boolean isFromFriend = activePrivateUser.equals(msg.getSender());
            boolean isToFriend = sessionUsername.equals(msg.getSender()) && activePrivateUser.equals(msg.getReceiver());
            return isFromFriend || isToFriend;
        }
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
        lastSender = null;
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
    }

    public void appendMessage(MessageDTO message) {
        boolean isHighlighted = message.getContent() != null &&
                message.getContent().contains("@" + sessionUsername);

        boolean isConsecutive = false;
        if (message.getType() != MessageType.SYSTEM && message.getType() != MessageType.JOIN && message.getType() != MessageType.LEAVE && message.getType() != MessageType.ERROR && !"SYSTEM".equals(message.getSender())) {
            if (message.getSender() != null && message.getSender().equals(lastSender)) {
                isConsecutive = true;
            }
            lastSender = message.getSender();
        } else {
            lastSender = null;
        }

        int insertIndex = chatHistoryPanel.getComponentCount() - 1;
        chatHistoryPanel.add(new ChatMessageItem(message, isHighlighted, isConsecutive), insertIndex);
        chatHistoryPanel.add(Box.createVerticalStrut(10), insertIndex + 1);

        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public void renderServerMembers(List<String> allUsers, List<String> onlineUsers, String ownerId) {
        sidebarListPanel.removeAll();
        sidebarListPanel.add(Box.createVerticalStrut(15));
        
        List<String> onlineList = new java.util.ArrayList<>();
        List<String> offlineList = new java.util.ArrayList<>();
        
        for (String u : allUsers) {
            if (onlineUsers.contains(u)) onlineList.add(u);
            else offlineList.add(u);
        }

        boolean isOwner = sessionUsername.equals(ownerId);

        // --- TRỰC TUYẾN ---
        sidebarListPanel.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + onlineList.size()));
        sidebarListPanel.add(Box.createVerticalStrut(5));
        for (String username : onlineList) {
            if (username == null || username.isBlank()) continue;
            UserListItem item = new UserListItem(username.trim(), null, AppColors.STATUS_ONLINE, true);
            if (isOwner && !username.equals(sessionUsername)) {
                item.setOnContextMenu(() -> showMemberContextMenu(item, username.trim()));
            }
            sidebarListPanel.add(item);
        }

        sidebarListPanel.add(Box.createVerticalStrut(15));

        // --- NGOẠI TUYẾN ---
        sidebarListPanel.add(new SidebarCategoryHeader("NGOẠI TUYẾN — " + offlineList.size()));
        sidebarListPanel.add(Box.createVerticalStrut(5));
        for (String username : offlineList) {
            if (username == null || username.isBlank()) continue;
            UserListItem item = new UserListItem(username.trim(), null, AppColors.STATUS_OFFLINE, false);
            if (isOwner && !username.equals(sessionUsername)) {
                item.setOnContextMenu(() -> showMemberContextMenu(item, username.trim()));
            }
            sidebarListPanel.add(item);
        }

        sidebarListPanel.revalidate();
        sidebarListPanel.repaint();
    }

    private void showMemberContextMenu(Component anchor, String username) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem assignRoleItem = new JMenuItem("Cấp Role");
        JMenuItem kickItem = new JMenuItem("Kick Khỏi Server");
        kickItem.setForeground(AppColors.DANGER);

        assignRoleItem.addActionListener(e -> {
            new gui.server.AssignRoleDialog(this, activeServerId, username).setVisible(true);
        });

        kickItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc muốn đuổi " + username + " khỏi server?", 
                "Xác nhận Kick", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() {
                        new network.RoleApiClient().kickMember(activeServerId, username);
                        return null;
                    }
                    @Override protected void done() {
                        try { get(); loadServerMembersAndPresence(activeServerId); }
                        catch(Exception ex) { JOptionPane.showMessageDialog(ChatClientGUI.this, "Lỗi Kick: " + ex.getMessage()); }
                    }
                }.execute();
            }
        });

        menu.add(assignRoleItem);
        menu.add(kickItem);
        menu.show(anchor, anchor.getWidth() / 2, anchor.getHeight() / 2);
    }



    public void setOnlineUsers(List<String> usernames) {
        // Fallback or WS trigger
        if (activeServerId > 0) {
            loadServerMembersAndPresence(activeServerId);
            return;
        }
        sidebarListPanel.removeAll();
        sidebarListPanel.add(Box.createVerticalStrut(15));
        sidebarListPanel.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + usernames.size()));
        sidebarListPanel.add(Box.createVerticalStrut(5));

        for (String username : usernames) {
            if (username == null || username.isBlank()) continue;
            sidebarListPanel.add(new UserListItem(username.trim(), null, AppColors.STATUS_ONLINE, true));
        }

        sidebarListPanel.revalidate();
        sidebarListPanel.repaint();
    }
}
