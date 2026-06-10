package gui;

import gui.chat.ChatHistoryView;
import gui.chat.FileUploadController;
import gui.chat.MemberListView;
import gui.chat.OutboundMessageController;
import gui.chat.PinController;
import gui.chat.UnreadCountSync;
import gui.components.channels.ChannelSidebar;
import gui.components.chat.ChatMessageItem;
import gui.components.chat.ChatInputContainer;
import gui.components.chat.IconButton;
import gui.components.feedback.Toast;
import gui.components.friends.FriendSidebar;
import gui.components.mini.MiniSidebar;
import gui.components.navigation.ServerSidebar;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.ChannelApiClient;
import network.ChatWebSocketClient;
import network.PresenceApiClient;
import network.ServerApiClient;
import network.SessionManager;
import network.PrivateMessageApiClient;
import network.FileApiClient;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * Cửa sổ chat chính. Đóng vai trò orchestrator: dựng layout, giữ trạng thái
 * điều hướng (server/channel/DM đang mở) và nối các thành phần con:
 * {@link ChatHistoryView}, {@link MemberListView}, {@link UnreadCountSync},
 * {@link FileUploadController} + các sidebar.
 */
public class ChatClientGUI extends JFrame {

    private final String sessionUsername;

    private final ChatWebSocketClient wsClient = new ChatWebSocketClient();
    private final ChannelApiClient channelApi = new ChannelApiClient();
    private final PresenceApiClient presenceApi = new PresenceApiClient();
    private final ServerApiClient serverApi = new ServerApiClient();
    private final PrivateMessageApiClient privateMessageApi = new PrivateMessageApiClient();
    private final FileApiClient fileApi = new FileApiClient();
    private final network.NotificationApiClient notificationApi = new network.NotificationApiClient();
    private final network.PermissionApiClient permissionApi = new network.PermissionApiClient();

    private final ServerSidebar serverSidebar = new ServerSidebar();
    private final ChannelSidebar channelSidebar;
    private final FriendSidebar friendSidebar;
    private final MiniSidebar miniSidebar = new MiniSidebar();
    private final JPanel westPanel;
    private final JPanel eastContainer;
    private final ChatInputContainer chatInput = new ChatInputContainer();

    // Thành phần con tách ra (Phase 0 refactor)
    private final ChatHistoryView chatHistoryView;
    private final MemberListView memberListView;
    private final UnreadCountSync unreadSync;
    private final FileUploadController fileUpload;
    private final PinController pinController;
    private final OutboundMessageController outbound;

    // Header vùng chat (tiêu đề kênh + icon ghim 📌)
    private JLabel channelTitleLabel;
    private JPanel channelHeaderPanel;
    
    private gui.components.chat.TypingIndicatorPanel typingIndicatorPanel;

    private long activeServerId = -1;
    private long activeChannelId = -1;
    private String activePrivateUser = null;

    // Callback các thao tác trên tin nhắn (Sửa / Xóa / Ghim)
    private final ChatMessageItem.MessageActions messageActions = new ChatMessageItem.MessageActions() {
        @Override public void onEdit(MessageDTO msg, String newContent) { outbound.sendEdit(msg, newContent); }
        @Override public void onDelete(MessageDTO msg) { outbound.sendDelete(msg); }
        @Override public void onPin(MessageDTO msg) { pinController.pin(msg); }
    };

    public ChatClientGUI(String sessionUsername) {
        setTitle("Chat Server v2.0 — " + sessionUsername);
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        this.sessionUsername = sessionUsername;
        this.channelSidebar = new ChannelSidebar(sessionUsername);
        this.friendSidebar = new FriendSidebar(sessionUsername);
        this.chatHistoryView = new ChatHistoryView(sessionUsername, messageActions);
        this.memberListView = new MemberListView(sessionUsername, this::openAssignRoleDialog, this::confirmKick);
        this.unreadSync = new UnreadCountSync(notificationApi, serverSidebar, channelSidebar, friendSidebar, sessionUsername);
        this.fileUpload = new FileUploadController(this, fileApi, wsClient, sessionUsername, this::toast, chatInput::setUploading);
        this.pinController = new PinController(this, this::toast, channelApi, () -> activeChannelId);
        this.outbound = new OutboundMessageController(wsClient, sessionUsername, this::toast);

        wireSidebarCallbacks();

        westPanel = new JPanel(new BorderLayout());
        westPanel.add(serverSidebar, BorderLayout.WEST);
        westPanel.add(friendSidebar, BorderLayout.CENTER); // Ban đầu là trang chủ

        // --- EAST: online members + MiniSidebar ---
        miniSidebar.setVisible(false);
        miniSidebar.setOnFriendSelected(this::openDirectMessage);
        miniSidebar.setOnServerSelected((id, name) -> {
            this.activeServerId = id;
            channelSidebar.loadChannels(id, name);
        });

        eastContainer = new JPanel(new BorderLayout());
        eastContainer.setBackground(AppColors.BG_SECONDARY);
        eastContainer.add(memberListView, BorderLayout.CENTER);
        eastContainer.add(miniSidebar, BorderLayout.EAST);

        add(westPanel, BorderLayout.WEST);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(eastContainer, BorderLayout.EAST);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { wsClient.close(); }
        });
    }

    /** Gắn các callback chuyển server/channel/bạn bè + broadcast thay đổi qua WS. */
    private void wireSidebarCallbacks() {
        serverSidebar.setOnServerSelected(this::onServerSelected);
        channelSidebar.setOnChannelSelected(this::onChannelSelected);
        friendSidebar.setOnFriendSelected(this::openDirectMessage);

        Runnable onUserChanged = () -> outbound.broadcast(MessageType.CHAT, "[SYSTEM_USER_UPDATE]", null, null, null);
        channelSidebar.setOnUserChanged(onUserChanged);
        friendSidebar.setOnUserChanged(onUserChanged);

        friendSidebar.setOnFriendAction(targetUser ->
                outbound.broadcast(MessageType.PRIVATE, "[SYSTEM_FRIEND_UPDATE]", null, null, targetUser));

        channelSidebar.setOnChannelChanged(() -> {
            if (activeServerId != -1) outbound.broadcast(MessageType.CHAT, "[SYSTEM_CHANNEL_UPDATE]", activeServerId, null, null);
        });

        serverSidebar.setOnServerChanged(changedServerId -> {
            if (changedServerId != -1) outbound.broadcast(MessageType.CHAT, "[SYSTEM_SERVER_UPDATE]", changedServerId, null, null);
        });
    }

    private JPanel buildCenterPanel() {
        channelTitleLabel = new JLabel("");
        channelTitleLabel.setFont(AppFonts.BODY_BOLD);
        channelTitleLabel.setForeground(AppColors.TEXT_HEADER);

        IconButton pinBtn = new IconButton("📌", e -> pinController.openDialog());
        pinBtn.setToolTipText("Pinned messages");

        IconButton toggleMiniBtn = new IconButton("👥", e -> {
            boolean show = !miniSidebar.isVisible();
            miniSidebar.setVisible(show);
            if (show) miniSidebar.refresh();
            eastContainer.revalidate();
            eastContainer.repaint();
        });
        toggleMiniBtn.setToolTipText("Friends & Servers");

        IconButton searchBtn = new IconButton("🔍", e -> openSearchDialog());
        searchBtn.setToolTipText("Search messages");

        JPanel headerRightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        headerRightWrap.setOpaque(false);
        headerRightWrap.add(searchBtn);
        headerRightWrap.add(pinBtn);
        headerRightWrap.add(toggleMiniBtn);

        channelHeaderPanel = new JPanel(new BorderLayout());
        channelHeaderPanel.setBackground(AppColors.BG_PRIMARY);
        channelHeaderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BG_TERTIARY),
                BorderFactory.createEmptyBorder(12, 20, 12, 16)));
        channelHeaderPanel.add(channelTitleLabel, BorderLayout.WEST);
        channelHeaderPanel.add(headerRightWrap, BorderLayout.EAST);
        channelHeaderPanel.setVisible(false);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(AppColors.BG_PRIMARY);
        centerPanel.add(channelHeaderPanel, BorderLayout.NORTH);
        centerPanel.add(chatHistoryView, BorderLayout.CENTER);
        centerPanel.add(buildBottomPanel(), BorderLayout.SOUTH);
        return centerPanel;
    }

    private JPanel buildBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(AppColors.BG_PRIMARY);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        typingIndicatorPanel = new gui.components.chat.TypingIndicatorPanel();
        JPanel inputWrapper = new JPanel(new BorderLayout());
        inputWrapper.setOpaque(false);
        inputWrapper.add(typingIndicatorPanel, BorderLayout.NORTH);
        inputWrapper.add(chatInput, BorderLayout.CENTER);

        chatInput.setVisible(false); // Ẩn ban đầu
        chatInput.getSendButton().addActionListener(e -> sendChatFromInput());
        chatInput.getInputField().addActionListener(e -> sendChatFromInput());
        chatInput.setOnAttach(() -> fileUpload.chooseAndSend(activeChannelId, activeServerId, activePrivateUser));
        // Mũi tên Lên khi ô nhập trống → sửa nhanh tin nhắn cuối của chính mình
        chatInput.getInputField().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP && chatInput.getMessageText().isEmpty()) {
                    chatHistoryView.startEditingLastOwn();
                }
            }
        });

        // Typing indicator event sender
        Timer typingTimer = new Timer(3000, e -> {});
        typingTimer.setRepeats(false);
        chatInput.getInputField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void triggerTyping() {
                if (!typingTimer.isRunning() && wsClient.isOpen() && chatInput.isVisible() && !chatInput.getMessageText().isEmpty()) {
                    Long cId = activeChannelId > 0 ? activeChannelId : null;
                    Long sId = activeServerId > 0 ? activeServerId : null;
                    outbound.broadcast(MessageType.TYPING, "typing", sId, cId, activePrivateUser);
                    typingTimer.start();
                }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { triggerTyping(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { triggerTyping(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { triggerTyping(); }
        });

        bottomPanel.add(inputWrapper, BorderLayout.CENTER);
        return bottomPanel;
    }

    /** Bắt đầu phiên: load servers, presence, connect WS, load trang chủ. */
    public void startSession() {
        serverSidebar.loadServers();
        onServerSelected(-1, null); // Gọi trực tiếp hàm xử lý logic Trang chủ
        connectWebSocket();
        unreadSync.refresh();
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
            network.PermissionCache.get().clear(); // Home/DM: không có quyền theo server
            westPanel.add(friendSidebar, BorderLayout.CENTER);
            eastContainer.setVisible(false); // Ẩn thanh thành viên
            chatInput.setVisible(false);      // Ẩn thanh nhập khi ở Home
            if (chatHistoryView != null) {
            chatHistoryView.setPlaceholderText("Welcome to ChatServer! Select a server, channel, or friend to start chatting.");
        }    clearChat();
            memberListView.renderOnline(List.of());
            loadPresence();
        } else {
            this.activePrivateUser = null;
            this.activeChannelId = -1;
            westPanel.add(channelSidebar, BorderLayout.CENTER);
            eastContainer.setVisible(true);  // Hiện thanh thành viên
            chatInput.setVisible(false);     // Ẩn cho đến khi chọn channel
            setChannelHeader(null);
            channelSidebar.loadChannels(serverId, serverName != null ? serverName : "Server #" + serverId);
            clearChat();
            loadPermissionsAndMembers(serverId);
        }
        unreadSync.refresh(); // làm tươi badge chưa đọc sau khi điều hướng
        westPanel.revalidate();
        westPanel.repaint();
    }

    private void onChannelSelected(long channelId) {
        this.activeChannelId = channelId;
        chatInput.setVisible(true);
        String name = channelSidebar.getChannelName(channelId);
        setChannelHeader("# " + (name != null ? name : "kênh"));
        chatHistoryView.setPlaceholderText("No messages yet — start the conversation 👋");
        clearChat();

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { notificationApi.ackChannel(channelId, sessionUsername); return null; }
            @Override protected void done() { unreadSync.refresh(); }
        }.execute();

        new SwingWorker<List<MessageDTO>, Void>() {
            @Override protected List<MessageDTO> doInBackground() {
                return channelApi.fetchRecentMessages(channelId, 1000);
            }
            @Override protected void done() {
                try {
                    for (MessageDTO m : get()) chatHistoryView.appendMessage(m);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    Toast.error(ChatClientGUI.this, "Failed to load history: " + cause.getMessage());
                }
            }
        }.execute();
    }

    /** Mở DM với 1 người bạn. */
    private void openDirectMessage(String username) {
        this.activeChannelId = -1;
        this.activePrivateUser = username;
        chatInput.setVisible(true);
        setChannelHeader("@ " + username);
        chatHistoryView.setPlaceholderText("Send your first message to " + username);
        clearChat();

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { notificationApi.ackDm(username, sessionUsername); return null; }
            @Override protected void done() { unreadSync.refresh(); }
        }.execute();

        new SwingWorker<List<MessageDTO>, Void>() {
            @Override protected List<MessageDTO> doInBackground() {
                return privateMessageApi.fetchPrivateMessages(username, 1000);
            }
            @Override protected void done() {
                try {
                    for (MessageDTO m : get()) {
                        if (!"[SYSTEM_FRIEND_UPDATE]".equals(m.getContent())) chatHistoryView.appendMessage(m);
                    }
                } catch (Exception ex) {
                    Toast.error(ChatClientGUI.this, "Failed to load history: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------
    // Presence / members
    // ---------------------------------------------------------------

    private void loadPresence() {
        if (activeServerId <= 0) {
            new SwingWorker<List<String>, Void>() {
                @Override protected List<String> doInBackground() { return presenceApi.getOnlineUsers(); }
                @Override protected void done() {
                    try { friendSidebar.loadFriendsAndRequests(get()); } catch (Exception ignore) {}
                }
            }.execute();
            return;
        }
        loadServerMembersAndPresence(activeServerId);
    }

    /** Nạp quyền của user hiện tại cho server rồi mới render danh sách thành viên. */
    private void loadPermissionsAndMembers(long serverId) {
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() {
                return permissionApi.getPermissionBitmask(serverId, sessionUsername);
            }
            @Override protected void done() {
                try { network.PermissionCache.get().set(serverId, get()); }
                catch (Exception ignore) { network.PermissionCache.get().set(serverId, 0); }
                loadServerMembersAndPresence(serverId);
            }
        }.execute();
    }

    private void loadServerMembersAndPresence(long serverId) {
        new SwingWorker<java.util.Map<String, Object>, Void>() {
            @Override protected java.util.Map<String, Object> doInBackground() {
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
                    List<String> allUsers = new ArrayList<>();
                    if (members != null) {
                        for (java.util.Map<String, Object> m : members) {
                            Object uid = m.get("userId");
                            if (uid != null) allUsers.add(uid.toString());
                        }
                    }
                    java.util.Map<String, Object> serverData = (java.util.Map<String, Object>) details.get("server");
                    if (serverData == null) serverData = details;
                    String ownerId = String.valueOf(serverData.get("ownerId"));

                    memberListView.renderServerMembers(allUsers, online, ownerId);
                } catch (Exception ex) {
                    // giữ nguyên hiển thị cũ nếu lỗi
                }
            }
        }.execute();
    }

    /** Cập nhật danh sách online (fallback hoặc trigger từ WS LIST). */
    private void setOnlineUsers(List<String> usernames) {
        if (activeServerId > 0) { loadServerMembersAndPresence(activeServerId); return; }
        memberListView.renderOnline(usernames);
    }

    /** Mở dialog cấp Role cho 1 thành viên (context menu MemberListView). */
    private void openAssignRoleDialog(String username) {
        new gui.server.AssignRoleDialog(this, activeServerId, username).setVisible(true);
    }

    /** Xác nhận + thực thi Kick 1 thành viên (context menu MemberListView). */
    private void confirmKick(String username) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to kick " + username + "?",
                "Confirm Kick", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                new network.RoleApiClient().kickMember(activeServerId, username);
                return null;
            }
            @Override protected void done() {
                try { get(); loadServerMembersAndPresence(activeServerId); }
                catch (Exception ex) { JOptionPane.showMessageDialog(ChatClientGUI.this, "Kick error: " + ex.getMessage()); }
            }
        }.execute();
    }

    // ---------------------------------------------------------------
    // WebSocket
    // ---------------------------------------------------------------

    private void connectWebSocket() {
        String token = SessionManager.get().getAccessToken();
        if (token == null) {
            Toast.error(this, "Missing JWT — please log in again");
            return;
        }
        wsClient.setOnMessage(msg -> SwingUtilities.invokeLater(() -> handleIncoming(msg)));
        wsClient.setOnError(err -> SwingUtilities.invokeLater(() -> Toast.error(this, err)));
        wsClient.setOnClose(() -> SwingUtilities.invokeLater(() -> {}));
        wsClient.connect(token).whenComplete((v, err) -> SwingUtilities.invokeLater(() -> {
            if (err != null) Toast.error(this, "WebSocket error: " + err.getMessage());
        }));
    }

    private void sendChatFromInput() {
        String text = chatInput.getMessageText();
        if (text == null || text.trim().isEmpty()) return;
        if (!wsClient.isOpen()) {
            Toast.warn(this, "WebSocket not ready, message not sent");
            return;
        }
        outbound.sendChat(text, activeChannelId, activeServerId, activePrivateUser);
        chatInput.clearInput();
    }

    /** Phân loại message đến và lọc theo channel đang mở. */
    private void handleIncoming(MessageDTO msg) {
        if (msg.getType() == MessageType.PRIVATE && "[SYSTEM_FRIEND_UPDATE]".equals(msg.getContent())) {
            if (activeServerId == -1) loadPresence(); // tải lại bạn bè, không in ra UI
            return;
        }
        if ("[SYSTEM_CHANNEL_UPDATE]".equals(msg.getContent())) {
            if (activeServerId == msg.getServerId()) channelSidebar.loadChannels(activeServerId, null);
            return;
        }
        if ("[SYSTEM_SERVER_UPDATE]".equals(msg.getContent())) {
            serverSidebar.loadServers();
            if (activeServerId == msg.getServerId()) {
                loadPresence();
                channelSidebar.loadChannels(activeServerId, null);
            }
            return;
        }
        if ("[SYSTEM_USER_UPDATE]".equals(msg.getContent())) {
            network.UserProfileCache.clear(msg.getSender());
            loadPresence();
            serverSidebar.loadServers();
            friendSidebar.refreshUserFooter();
            channelSidebar.refreshUserFooter();
            return;
        }

        if (msg.getType() == null) {
            deliverOrCount(msg);
            return;
        }
        switch (msg.getType()) {
            case CHAT, PRIVATE -> deliverOrCount(msg);
            case EDIT -> {
                if (belongsToActiveChannel(msg)) chatHistoryView.applyEdit(msg);
                else unreadSync.refresh();
            }
            case DELETE -> {
                if (belongsToActiveChannel(msg)) {
                    chatHistoryView.applyDelete(msg);
                    pinController.removeByMessageId(msg.getMessageId());
                }
            }
            case JOIN, LEAVE -> loadPresence();
            case SYSTEM -> appendSystem(msg.getContent());
            case ERROR -> Toast.error(this, msg.getContent());
            case TYPING -> {
                if (belongsToActiveChannel(msg) && !sessionUsername.equals(msg.getSender())) {
                    typingIndicatorPanel.addTypingUser(msg.getSender());
                }
            }
            case PING, PONG -> { /* ignore */ }
            case LIST -> {
                if (msg.getContent() != null) setOnlineUsers(List.of(msg.getContent().split(",")));
            }
            default -> {
                if (belongsToActiveChannel(msg)) chatHistoryView.appendMessage(msg);
            }
        }
    }

    /** Tin thuộc kênh đang mở → hiển thị + ack; ngược lại chỉ cập nhật badge. */
    private void deliverOrCount(MessageDTO msg) {
        if (belongsToActiveChannel(msg)) {
            chatHistoryView.appendMessage(msg);
            unreadSync.ack(msg);
        } else {
            unreadSync.refresh();
        }
    }

    /** Chỉ hiển thị tin của channel đang mở, hoặc tin private. */
    private boolean belongsToActiveChannel(MessageDTO msg) {
        if (msg.getType() == MessageType.PRIVATE) {
            if (activePrivateUser == null) return false;
            boolean isFromFriend = activePrivateUser.equals(msg.getSender());
            boolean isToFriend = sessionUsername.equals(msg.getSender()) && activePrivateUser.equals(msg.getReceiver());
            return isFromFriend || isToFriend;
        }
        return msg.getChannelId() == null || msg.getChannelId() == activeChannelId;
    }

    /** Tin SYSTEM thật từ server → giữ trong luồng chat. */
    private void appendSystem(String text) {
        chatHistoryView.appendSystem(text);
    }

    /** Thông báo tạm thời (lỗi/tiến trình) → toast nổi, không chèn vào luồng chat. */
    private void toast(Toast.Level level, String message) {
        Toast.show(this, message, level);
    }

    /** Xóa lịch sử đang hiển thị + reset danh sách ghim (khi chuyển channel/DM). */
    private void clearChat() {
        chatHistoryView.clear();
        pinController.clear();
        if (typingIndicatorPanel != null) {
            typingIndicatorPanel.clear();
        }
    }

    // ---------------------------------------------------------------
    // Sửa / Xóa / Ghim tin nhắn
    // ---------------------------------------------------------------

    /** Cập nhật tiêu đề header vùng chat; title == null sẽ ẩn header. */
    private void setChannelHeader(String title) {
        if (channelTitleLabel != null && title != null) channelTitleLabel.setText(title);
        if (channelHeaderPanel != null) {
            channelHeaderPanel.setVisible(title != null);
            channelHeaderPanel.revalidate();
            channelHeaderPanel.repaint();
        }
    }

    private void openSearchDialog() {
        Long chId = activeChannelId != -1 ? activeChannelId : null;
        Long svId = activeServerId != -1 ? activeServerId : null;
        new gui.components.dialogs.SearchDialog(this, channelApi, chId, svId, msgId -> {
            chatHistoryView.scrollToMessage(msgId);
        }).setVisible(true);
    }
}
