package gui;

import gui.chat.ChatHistoryView;
import gui.chat.FileUploadController;
import gui.chat.RightSidebarView;
import gui.chat.OutboundMessageController;
import gui.chat.PinController;
import gui.chat.UnreadCountSync;
import gui.components.channels.ChannelSidebar;
import gui.components.chat.ChatMessageItem;
import gui.components.chat.ChatInputContainer;
import gui.components.AppIcons;
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
 * {@link ChatHistoryView}, {@link RightSidebarView}, {@link UnreadCountSync},
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
    private final RightSidebarView rightSidebar;
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
        @Override public void onReply(MessageDTO msg) {
            chatInput.setReplyContext(msg.getMessageId(), msg.getSender(), msg.getContent());
            chatInput.getInputArea().requestFocusInWindow();
        }
    };

    public ChatClientGUI(String sessionUsername) {
        setTitle("Chat Server v2.0 — " + sessionUsername);
        try {
            java.net.URL url = getClass().getResource("/logo/logo.png");
            if (url != null) {
                setIconImage(javax.imageio.ImageIO.read(url));
            }
        } catch (Exception e) {}
        // Mở theo ~80% màn hình (cap 1200×750, sàn 900×600) để hợp mọi kích thước/độ phân giải.
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(900, Math.min(1200, (int) (screen.width * 0.8)));
        int h = Math.max(600, Math.min(750, (int) (screen.height * 0.8)));
        setSize(w, h);
        setMinimumSize(new Dimension(820, 560)); // chặn layout vỡ/che ô nhập khi thu nhỏ
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        this.sessionUsername = sessionUsername;
        this.channelSidebar = new ChannelSidebar(sessionUsername);
        this.friendSidebar = new FriendSidebar(sessionUsername);
        this.chatHistoryView = new ChatHistoryView(sessionUsername, messageActions);
        this.rightSidebar = new RightSidebarView(sessionUsername, this::openAssignRoleDialog, this::confirmKick);
        this.unreadSync = new UnreadCountSync(notificationApi, serverSidebar, channelSidebar, friendSidebar, sessionUsername);
        this.fileUpload = new FileUploadController(this, fileApi, wsClient, sessionUsername, this::toast, chatInput::setUploading);
        this.outbound = new OutboundMessageController(wsClient, sessionUsername, this::toast);
        // Sau khi ghim/bỏ ghim → broadcast marker kèm channelId để client khác refresh real-time.
        this.pinController = new PinController(this, this::toast, channelApi, () -> activeChannelId,
                cid -> outbound.broadcast(MessageType.CHAT, "[SYSTEM_PIN_UPDATE]", activeServerId, cid, null));

        // Khởi tạo SystemTray (icon notification khi app minimize)
        gui.notification.SystemTrayManager.get().init(this);

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
        eastContainer.add(rightSidebar, BorderLayout.CENTER);
        eastContainer.add(miniSidebar, BorderLayout.EAST);

        add(westPanel, BorderLayout.WEST);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(eastContainer, BorderLayout.EAST);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { disconnect(); }
        });

        installZoomKeybindings();
    }

    public void disconnect() {
        if (wsClient != null) {
            wsClient.close();
        }
    }

    // ---------------------------------------------------------------
    // Zoom toàn cục (Cmd/Ctrl +, -, 0) — phóng/thu chữ + spacing đồng đều
    // ---------------------------------------------------------------
    private void installZoomKeybindings() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // Cmd@mac, Ctrl@win/linux

        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS, mask), "zoomIn");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PLUS, mask), "zoomIn");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ADD, mask), "zoomIn");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, mask), "zoomOut");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SUBTRACT, mask), "zoomOut");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, mask), "zoomReset");

        am.put("zoomIn", zoomAction(() -> gui.theme.UiScale.get().increase()));
        am.put("zoomOut", zoomAction(() -> gui.theme.UiScale.get().decrease()));
        am.put("zoomReset", zoomAction(() -> gui.theme.UiScale.get().reset()));
    }

    private Action zoomAction(java.util.function.DoubleSupplier change) {
        return new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                double factor = change.getAsDouble();
                gui.theme.AppFonts.applyGlobalScale();
                SwingUtilities.updateComponentTreeUI(ChatClientGUI.this);
                revalidate();
                repaint();
                Toast.info(ChatClientGUI.this, "Thu phóng: " + Math.round(factor * 100) + "%");
            }
        };
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

        IconButton pinBtn = new IconButton(AppIcons.pin(16), e -> pinController.openDialog());
        pinBtn.setToolTipText("Pinned messages");

        IconButton toggleMiniBtn = new IconButton(AppIcons.users(18), e -> {
            boolean show = !miniSidebar.isVisible();
            miniSidebar.setVisible(show);
            if (show) miniSidebar.refresh();
            eastContainer.revalidate();
            eastContainer.repaint();
        });
        toggleMiniBtn.setToolTipText("Friends & Servers");

        IconButton searchBtn = new IconButton(AppIcons.search(16), e -> openSearchDialog());
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
        chatInput.setOnSend(this::sendChatFromInput); // Enter = gửi (Shift+Enter xuống dòng)
        chatInput.setOnAttach(() -> fileUpload.chooseAndSend(activeChannelId, activeServerId, activePrivateUser));
        // Mũi tên Lên khi ô nhập trống → sửa nhanh tin nhắn cuối của chính mình
        chatInput.getInputArea().addKeyListener(new java.awt.event.KeyAdapter() {
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
        chatInput.getInputArea().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
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

    /** Vẽ lại nền vùng chat sau khi đổi hoạ tiết wallpaper (không cần rebuild cửa sổ). */
    public void refreshChatBackground() { chatHistoryView.repaint(); }

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

            // ---> NOTE 1: Xóa danh sách Tag @ khi về trang chủ
            chatInput.setAvailableMentions(List.of());

            if (chatHistoryView != null) {
                chatHistoryView.setPlaceholderText("Welcome to ChatServer! Select a server, channel, or friend to start chatting.");
            }
            clearChat();
            rightSidebar.renderOnline(java.util.Collections.emptyMap());
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
            // Sidebar phải: hiện section Thành viên; Ảnh/Video & File chờ tới khi chọn channel
            rightSidebar.setMembersVisible(true);
            rightSidebar.setAttachments(List.of());
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

        // Sidebar phải: hiện đủ 3 section; Ảnh/Video & File trích từ lịch sử tin nhắn (đồng bộ với đoạn chat)
        rightSidebar.setMembersVisible(true);
        rightSidebar.setAttachments(List.of());

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
                    List<MessageDTO> msgs = get();
                    for (MessageDTO m : msgs) chatHistoryView.appendMessage(m);
                    if (channelId == activeChannelId) rightSidebar.setAttachments(extractAttachments(msgs));
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    Toast.error(ChatClientGUI.this, "Failed to load history: " + cause.getMessage());
                }
            }
        }.execute();
    }

    /** Trích Ảnh/Video & File từ danh sách tin nhắn đã tải (mới nhất trước) cho sidebar phải. */
    private List<gui.components.chat.ChannelAttachment> extractAttachments(List<MessageDTO> msgs) {
        List<gui.components.chat.ChannelAttachment> atts = new java.util.ArrayList<>();
        for (MessageDTO m : msgs) {
            gui.components.chat.ChannelAttachment a = gui.components.chat.ChatMessageItem.toChannelAttachment(m);
            if (a != null) atts.add(a);
        }
        java.util.Collections.reverse(atts);
        return atts;
    }

    private void refreshSidebarAttachments() {
        if (activeChannelId > 0 || activePrivateUser != null) {
            rightSidebar.setAttachments(extractAttachments(chatHistoryView.getAllMessages()));
        }
    }

    /** Mở DM với 1 người bạn. */
    public void openDirectMessage(String username) {
        // --- FIX LỖI GIAO DIỆN KHI NHẢY TỪ SERVER SANG CHAT RIÊNG ---
        if (this.activeServerId != -1) {
            this.activeServerId = -1;
            // Xóa thanh Channels, thay bằng thanh Friends (Trang chủ)
            westPanel.remove(((BorderLayout) westPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER));
            westPanel.add(friendSidebar, BorderLayout.CENTER);

            // Ẩn danh sách thành viên Server bên phải
            eastContainer.setVisible(false);

            // ---> THÊM DÒNG NÀY ĐỂ HIGHLIGHT LẠI NÚT HOME Ở CỘT NGOÀI CÙNG BÊN TRÁI <---
            if (serverSidebar != null) {
                serverSidebar.selectHome();;
            }

            westPanel.revalidate();
            westPanel.repaint();

            // Nạp lại danh sách online/bạn bè cho Sidebar
            loadPresence();
        }
        // -------------------------------------------------------------

        this.activeChannelId = -1;
        this.activePrivateUser = username;
        chatInput.setVisible(true);

        // --- Lấy Biệt danh cục bộ (nếu có) để hiển thị lên Header ---
        String localNick = gui.utils.NicknameManager.getNickname(username);
        setChannelHeader("@ " + (localNick != null ? localNick : username));

        chatHistoryView.setPlaceholderText("Send your first message to " + username);
        clearChat();

        // Nạp tên bạn bè vào danh sách Tag @ (chỉ tag được người đang chat cùng)
        chatInput.setAvailableMentions(List.of(username));

        // Sidebar phải: hiện Ảnh/Video & File, ẩn Thành viên (DM không có danh sách thành viên)
        eastContainer.setVisible(true);
        rightSidebar.setMembersVisible(false);
        rightSidebar.setAttachments(List.of());

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
                    List<MessageDTO> msgs = get();
                    for (MessageDTO m : msgs) {
                        if (!"[SYSTEM_FRIEND_UPDATE]".equals(m.getContent())) chatHistoryView.appendMessage(m);
                    }
                    // Trích Ảnh/Video & File từ tin nhắn DM đã tải (DM không có channelId để gọi API)
                    if (username.equals(activePrivateUser)) rightSidebar.setAttachments(extractAttachments(msgs));
                } catch (Exception ex) {
                    gui.components.feedback.Toast.error(ChatClientGUI.this, "Failed to load history: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------
    // Presence / members
    // ---------------------------------------------------------------

    private void loadPresence() {
        if (activeServerId <= 0) {
            new SwingWorker<java.util.Map<String, String>, Void>() {
                @Override protected java.util.Map<String, String> doInBackground() { return presenceApi.getAllStatuses(); }
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
                java.util.Map<String, String> online = presenceApi.getAllStatuses();
                // Cũng tải danh sách roles để tính roleColor cho từng member
                java.util.List<java.util.Map<String, Object>> roles =
                        new network.RoleApiClient().getRoles(serverId);
                java.util.Map<String, Object> combined = new java.util.HashMap<>();
                combined.put("details", details);
                combined.put("online", online);
                combined.put("roles", roles);
                return combined;
            }
            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    java.util.Map<String, Object> result = get();
                    java.util.Map<String, Object> details   = (java.util.Map<String, Object>) result.get("details");
                    java.util.Map<String, String> statuses  = (java.util.Map<String, String>) result.get("online");
                    java.util.List<java.util.Map<String, Object>> roles =
                            (java.util.List<java.util.Map<String, Object>>) result.get("roles");

                    List<java.util.Map<String, Object>> members =
                            (List<java.util.Map<String, Object>>) details.get("members");
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

                    // Tính màu role cho từng member (role priority cao nhất → lấy màu đó)
                    java.util.Map<String, java.awt.Color> roleColors =
                            computeRoleColors(members, roles);

                    rightSidebar.renderServerMembers(allUsers, statuses, ownerId, roleColors);

                    // Nạp toàn bộ thành viên của Server vào danh sách Tag @
                    chatInput.setAvailableMentions(allUsers);

                } catch (Exception ex) {
                    // giữ nguyên hiển thị cũ nếu lỗi
                }
            }
        }.execute();
    }

    /**
     * Tính map username → Color dựa trên role có priority cao nhất mà member đó sở hữu.
     * Role màu trắng (#FFFFFF) hoặc không có màu sẽ bị bỏ qua (tránh màu vô hình trên nền sáng).
     */
    @SuppressWarnings("unchecked")
    private static java.util.Map<String, java.awt.Color> computeRoleColors(
            List<java.util.Map<String, Object>> members,
            java.util.List<java.util.Map<String, Object>> roles) {
        if (members == null || roles == null) return java.util.Map.of();

        // Build id → role map
        java.util.Map<String, java.util.Map<String, Object>> roleById = new java.util.HashMap<>();
        for (java.util.Map<String, Object> r : roles) {
            String id = String.valueOf(r.get("id"));
            roleById.put(id, r);
        }

        java.util.Map<String, java.awt.Color> result = new java.util.HashMap<>();
        for (java.util.Map<String, Object> member : members) {
            String username = String.valueOf(member.get("userId"));
            Object roleIdsObj = member.get("roleIds");
            if (!(roleIdsObj instanceof java.util.List<?> list) || list.isEmpty()) continue;

            int highestPriority = -1;
            java.awt.Color bestColor = null;
            for (Object o : list) {
                if (!(o instanceof Number n)) continue;
                String rid = String.valueOf(n.longValue());
                java.util.Map<String, Object> role = roleById.get(rid);
                if (role == null) continue;
                Object priObj = role.get("priority");
                int priority = (priObj instanceof Number p) ? p.intValue() : 0;
                String colorHex = String.valueOf(role.get("color"));
                if (priority > highestPriority && colorHex != null
                        && colorHex.startsWith("#")
                        && !colorHex.equalsIgnoreCase("#ffffff")
                        && !colorHex.equalsIgnoreCase("#FFFFFF")) {
                    try {
                        bestColor = java.awt.Color.decode(colorHex);
                        highestPriority = priority;
                    } catch (Exception ignore) {}
                }
            }
            if (bestColor != null) result.put(username, bestColor);
        }
        return result;
    }

    /** Cập nhật danh sách online (fallback hoặc trigger từ WS LIST). */
    private void setOnlineUsers(java.util.Map<String, String> statuses) {
        if (activeServerId > 0) { loadServerMembersAndPresence(activeServerId); return; }
        rightSidebar.renderOnline(statuses);
    }

    /**
     * Mở dialog cấp Role cho 1 thành viên.
     * Sau khi cấp thành công:
     *  1. Broadcast [SYSTEM_SERVER_UPDATE] → mọi client trong server tự động reload
     *  2. Reload lại permissions + members của chính mình để đổi màu ngay lập tức
     */
    private void openAssignRoleDialog(String username) {
        new gui.server.AssignRoleDialog(this, activeServerId, username, () -> {
            // Broadcast để mọi client trong server thấy đổi màu username
            outbound.broadcast(com.chatsever.common.enums.MessageType.CHAT,
                    "[SYSTEM_SERVER_UPDATE]", activeServerId, null, null);
            // Reload lại cho chính mình ngay không đợi WS
            loadPermissionsAndMembers(activeServerId);
        }).setVisible(true);
    }

    /** Xác nhận + thực thi Kick 1 thành viên (context menu RightSidebarView). */
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
        outbound.sendChat(text, activeChannelId, activeServerId, activePrivateUser, chatInput.getReplyToMessageId());
        chatInput.clearInput();
    }

    /** Phân loại message đến và lọc theo channel đang mở. */
    private void handleIncoming(MessageDTO msg) {
        if (msg.getType() == MessageType.PRIVATE && "[SYSTEM_FRIEND_UPDATE]".equals(msg.getContent())) {
            if (activeServerId == -1) loadPresence(); // tải lại bạn bè, không in ra UI
            return;
        }
        if ("[SYSTEM_PIN_UPDATE]".equals(msg.getContent())) {
            pinController.onRemotePinUpdate(msg.getChannelId()); // refresh dialog ghim nếu đang mở đúng kênh
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
                    refreshSidebarAttachments();
                }
            }
            case JOIN, LEAVE -> loadPresence();
            case SYSTEM -> appendSystem(msg.getContent());
            case ERROR -> Toast.error(this, msg.getContent());
            case TYPING -> {
                if (belongsToActiveTyping(msg) && !sessionUsername.equals(msg.getSender())) {
                    typingIndicatorPanel.addTypingUser(msg.getSender());
                }
            }
            case STATUS -> {
                String who = msg.getSender();
                String statusStr = msg.getContent();
                if (who != null && statusStr != null) {
                    rightSidebar.updateUserStatus(who, statusStr);
                    friendSidebar.updateUserStatus(who, statusStr);
                    if (sessionUsername.equals(who)) {
                        channelSidebar.updateUserStatus(statusStr);
                    }
                    // Force a full reload to move users between ONLINE/OFFLINE sections
                    loadPresence();
                }
            }
            case PING, PONG -> { /* ignore */ }
            case LIST -> {
                if (msg.getContent() != null) {
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    for (String u : msg.getContent().split(",")) map.put(u.trim(), "ONLINE");
                    setOnlineUsers(map);
                }
            }
            case REACT -> {
                if (belongsToActiveChannel(msg)) chatHistoryView.applyReaction(msg);
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
            refreshSidebarAttachments();
        } else {
            unreadSync.refresh();
        }

        // Notify OS
        if (msg.getSender() != null && !msg.getSender().equals(sessionUsername)) {
            new Thread(() -> {
                String title, content, avatarText, avatarUrl;
                Runnable onClick;

                if (msg.getType() == MessageType.PRIVATE) {
                    title = "Tin nhắn từ " + msg.getSender();
                    content = msg.getContent();
                    avatarText = msg.getSender();
                    
                    java.util.Map<String, Object> profile = network.UserProfileCache.get(msg.getSender());
                    if (profile == null) {
                        try {
                            profile = new network.UserProfileApiClient().getProfile(msg.getSender());
                        } catch (Exception ignored) {}
                    }
                    avatarUrl = (profile != null && profile.get("avatarUrl") != null) ? profile.get("avatarUrl").toString() : null;
                    if (avatarUrl == null && profile != null && profile.get("avatar") != null) avatarUrl = profile.get("avatar").toString();

                    onClick = () -> {
                        gui.notification.SystemTrayManager.get().restoreApp();
                        openDirectMessage(msg.getSender());
                    };
                } else {
                    String serverName = serverSidebar.getServerName(msg.getServerId());
                    if (serverName == null || serverName.isBlank()) serverName = "Server";
                    
                    String channelName = channelSidebar.getChannelName(msg.getChannelId());
                    if (channelName == null || channelName.isBlank()) {
                        try {
                            java.util.List<java.util.Map<String, Object>> channels = new network.ChannelApiClient().getChannelsByServer(msg.getServerId());
                            for (java.util.Map<String, Object> ch : channels) {
                                if (String.valueOf(msg.getChannelId()).equals(String.valueOf(ch.get("id")))) {
                                    channelName = ch.get("name") != null ? ch.get("name").toString() : null;
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    if (channelName == null || channelName.isBlank()) channelName = "Kênh";
                    
                    title = serverName;
                    avatarText = serverName;
                    avatarUrl = serverSidebar.getServerIconUrl(msg.getServerId());
                    content = "#" + channelName + " - " + msg.getSender() + ": " + msg.getContent();
                    
                    onClick = () -> {
                        gui.notification.SystemTrayManager.get().restoreApp();
                        onServerSelected(msg.getServerId(), serverSidebar.getServerName(msg.getServerId()));
                        SwingUtilities.invokeLater(() -> onChannelSelected(msg.getChannelId()));
                    };
                }

                if (content == null || content.isEmpty() || content.endsWith(": null")) {
                    if (msg.getType() == MessageType.PRIVATE) content = "[Tệp đính kèm]";
                    else content = "#" + channelSidebar.getChannelName(msg.getChannelId()) + " - " + msg.getSender() + " đã gửi 1 tệp đính kèm";
                }

                gui.notification.SystemTrayManager.get().notifyNewMessage(title, content, avatarText, avatarUrl, onClick);
            }).start();
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

    /**
     * Lọc sự kiện TYPING: chỉ hiển thị khi đang ở đúng channel/DM mà người gõ đang nhắn.
     * - DM typing: receiver phải là mình VÀ sender phải là người đang chat cùng.
     * - Channel typing: channelId phải khớp VÀ serverId phải khớp.
     * Tách riêng khỏi belongsToActiveChannel vì TYPING có trường receiver (khác PRIVATE message).
     */
    private boolean belongsToActiveTyping(MessageDTO msg) {
        boolean isDmTyping = msg.getReceiver() != null;
        if (isDmTyping) {
            // DM typing: chỉ hiển thị nếu đang mở DM với đúng người gửi
            // và receiver của typing event là chính mình
            return activePrivateUser != null
                    && activePrivateUser.equals(msg.getSender())
                    && sessionUsername.equals(msg.getReceiver());
        }
        // Channel typing: phải đang ở đúng channel VÀ đúng server
        if (activeChannelId <= 0 || activePrivateUser != null) return false;
        boolean channelMatch = msg.getChannelId() != null && msg.getChannelId() == activeChannelId;
        boolean serverMatch = msg.getServerId() == null || msg.getServerId() == activeServerId;
        return channelMatch && serverMatch;
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

    // ---------------------------------------------------------------
    // Tìm kiếm tin nhắn
    // ---------------------------------------------------------------

    /** Mở dialog tìm kiếm tin nhắn với phạm vi theo ngữ cảnh đang mở. */
    private void openMessageSearch() {
        gui.components.chat.MessageSearchPanel panel =
                new gui.components.chat.MessageSearchPanel(this, activeChannelId, activePrivateUser, this::jumpToSearchResult);
        panel.setVisible(true);
    }

    /** Điều hướng + cuộn tới tin nhắn từ kết quả tìm kiếm. */
    private void jumpToSearchResult(MessageDTO m) {
        boolean isPrivate = m.getChannelId() == null && m.getReceiver() != null;
        if (isPrivate) {
            String other = sessionUsername.equals(m.getSender()) ? m.getReceiver() : m.getSender();
            if (other == null) { tryScrollOrToast(m.getMessageId()); return; }
            if (!other.equals(activePrivateUser)) {
                openDirectMessage(other);
                scrollAfterLoad(m.getMessageId());
            } else {
                tryScrollOrToast(m.getMessageId());
            }
            return;
        }
        if (m.getChannelId() != null && m.getChannelId() == activeChannelId) {
            tryScrollOrToast(m.getMessageId());
        } else {
            Toast.info(this, "Tin nhắn thuộc kênh khác — hãy mở kênh đó rồi tìm lại.");
        }
    }

    private void tryScrollOrToast(Long messageId) {
        if (!chatHistoryView.scrollToMessage(messageId)) {
            Toast.info(this, "Tin nhắn không nằm trong phần lịch sử đã tải.");
        }
    }

    /** DM vừa mở nạp lịch sử bất đồng bộ → thử cuộn vài lần tới khi tin nhắn có mặt. */
    private void scrollAfterLoad(Long messageId) {
        if (messageId == null) return;
        final int[] tries = {0};
        Timer t = new Timer(200, null);
        t.addActionListener(e -> {
            if (chatHistoryView.scrollToMessage(messageId) || ++tries[0] >= 15) {
                t.stop();
            }
        });
        t.start();
    }

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

    public String getSessionUsername() {
        return this.sessionUsername;
    }

    public void applyNicknameChange() {
        loadPresence();

        if (activePrivateUser != null) {
            openDirectMessage(activePrivateUser);
        } else if (activeChannelId != -1) {
            onChannelSelected(activeChannelId);
        }
    }
}