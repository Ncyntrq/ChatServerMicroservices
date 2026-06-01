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
import gui.theme.AppFonts;
import gui.components.dialogs.PinnedMessagesDialog;
import network.ApiConfig;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

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
    private final FileApiClient fileApi = new FileApiClient();

    private final ServerSidebar serverSidebar = new ServerSidebar();
    private final ChannelSidebar channelSidebar;
    private final FriendSidebar friendSidebar;
    private final MiniSidebar miniSidebar = new MiniSidebar();
    private final JPanel westPanel;
    private final JPanel eastContainer;
    private final ChatInputContainer chatInput = new ChatInputContainer();

    // Header vùng chat (tiêu đề kênh + icon ghim 📌) — feature #8
    private JLabel channelTitleLabel;
    private JPanel channelHeaderPanel;

    private long activeServerId = -1;
    private long activeChannelId = -1;
    private String activePrivateUser = null;

    // Theo dõi item theo messageId để cập nhật/xóa tại chỗ khi nhận EDIT/DELETE
    private final Map<Long, ChatMessageItem> messageItems = new LinkedHashMap<>();
    // Tin nhắn đã ghim của kênh đang mở (panel danh sách ghim ở feature #8)
    private final List<MessageDTO> pinnedMessages = new ArrayList<>();

    // Callback các thao tác trên tin nhắn (Sửa / Xóa / Ghim)
    private final ChatMessageItem.MessageActions messageActions = new ChatMessageItem.MessageActions() {
        @Override public void onEdit(MessageDTO msg, String newContent) { sendEdit(msg, newContent); }
        @Override public void onDelete(MessageDTO msg) { sendDelete(msg); }
        @Override public void onPin(MessageDTO msg) { pinMessage(msg); }
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
        chatInput.setOnAttach(this::chooseAndSendFile);
        // Mũi tên Lên khi ô nhập trống → sửa nhanh tin nhắn cuối của chính mình
        chatInput.getInputField().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP
                        && chatInput.getMessageText().isEmpty()) {
                    editLastOwnMessage();
                }
            }
        });
        bottomPanel.add(chatInput, BorderLayout.CENTER);

        // --- Header kênh: tiêu đề + icon ghim 📌 ---
        channelTitleLabel = new JLabel("");
        channelTitleLabel.setFont(AppFonts.BODY_BOLD);
        channelTitleLabel.setForeground(AppColors.TEXT_HEADER);

        IconButton pinBtn = new IconButton("📌", e -> openPinnedDialog());
        JPanel pinWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pinWrap.setOpaque(false);
        pinWrap.add(pinBtn);

        channelHeaderPanel = new JPanel(new BorderLayout());
        channelHeaderPanel.setBackground(AppColors.BG_PRIMARY);
        channelHeaderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BG_TERTIARY),
                BorderFactory.createEmptyBorder(12, 20, 12, 16)));
        channelHeaderPanel.add(channelTitleLabel, BorderLayout.WEST);
        channelHeaderPanel.add(pinWrap, BorderLayout.EAST);
        channelHeaderPanel.setVisible(false);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(AppColors.BG_PRIMARY);
        centerPanel.add(channelHeaderPanel, BorderLayout.NORTH);
        centerPanel.add(chatScrollPane, BorderLayout.CENTER);

        add(westPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
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
            setChannelHeader(null);
            clearChat();
            setOnlineUsers(List.of()); 
            loadPresence(); 
        } else {
            this.activePrivateUser = null;
            this.activeChannelId = -1;
            westPanel.add(channelSidebar, BorderLayout.CENTER);
            eastContainer.setVisible(true); // Hiện thanh thành viên
            chatInput.setVisible(false); // Ẩn cho đến khi chọn channel
            setChannelHeader(null);
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
        String name = channelSidebar.getChannelName(channelId);
        setChannelHeader("# " + (name != null ? name : "kênh"));
        clearChat();
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
        setChannelHeader("@ " + username);
        clearChat();
        new SwingWorker<List<MessageDTO>, Void>() {
            @Override
            protected List<MessageDTO> doInBackground() {
                return privateMessageApi.fetchPrivateMessages(username, 50);
            }

            @Override
            protected void done() {
                try {
                    List<MessageDTO> history = get();
                    for (int i = history.size() - 1; i >= 0; i--) {
                        appendMessage(history.get(i));
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

    /** Chọn 1 tệp tin, upload qua file-service rồi gửi tin nhắn đính kèm. */
    private void chooseAndSendFile() {
        if (!wsClient.isOpen()) { appendSystem("WebSocket chưa sẵn sàng"); return; }
        if (activeChannelId == -1 && activePrivateUser == null) {
            appendSystem("Hãy mở một kênh hoặc cuộc trò chuyện trước khi gửi tệp");
            return;
        }

        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        final java.io.File file = fc.getSelectedFile();
        if (file == null || !file.isFile()) return;

        // Chặn sớm phía client: server giới hạn 10MB → tránh upload tốn công rồi bị từ chối
        final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
        if (file.length() > MAX_UPLOAD_BYTES) {
            appendSystem("Tệp \"" + file.getName() + "\" vượt quá 10MB, vui lòng chọn tệp nhỏ hơn");
            return;
        }

        final long ch = activeChannelId;
        final long sv = activeServerId;
        final String dm = activePrivateUser;
        appendSystem("Đang tải lên: " + file.getName() + "…");

        new SwingWorker<FileApiClient.Uploaded, Void>() {
            @Override
            protected FileApiClient.Uploaded doInBackground() {
                return fileApi.uploadFile(file, ch != -1 ? ch : null);
            }
            @Override
            protected void done() {
                try {
                    FileApiClient.Uploaded up = get();
                    String content = ChatMessageItem.encodeAttachment(
                            up.contentType, up.size, up.thumbnailUrl, up.url, up.name);
                    MessageDTO out;
                    if (ch == -1 && dm != null) {
                        out = new MessageDTO(MessageType.PRIVATE, sessionUsername, null, content, LocalDateTime.now());
                        out.setReceiver(dm);
                    } else {
                        out = new MessageDTO(MessageType.CHAT, sessionUsername, null, content, LocalDateTime.now());
                        out.setServerId(sv);
                        out.setChannelId(ch);
                    }
                    wsClient.send(out).whenComplete((ws, err) -> {
                        if (err != null) SwingUtilities.invokeLater(
                                () -> appendSystem("Gửi tệp thất bại: " + err.getMessage()));
                    });
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    appendSystem("Tải tệp thất bại: " + cause.getMessage());
                }
            }
        }.execute();
    }

    /** Phân loại message đến và lọc theo channel đang mở. */
    private void handleIncoming(MessageDTO msg) {
        if (msg.getType() == MessageType.PRIVATE && "[SYSTEM_FRIEND_UPDATE]".equals(msg.getContent())) {
            // Tải lại danh sách bạn bè mà không in ra giao diện
            if (activeServerId == -1) loadPresence();
            return;
        }

        if (msg.getType() == null) {
            if (belongsToActiveChannel(msg)) appendMessage(msg);
            return;
        }
        switch (msg.getType()) {
            case CHAT, PRIVATE -> {
                if (belongsToActiveChannel(msg)) appendMessage(msg);
            }
            case EDIT -> {
                if (belongsToActiveChannel(msg)) applyEdit(msg);
            }
            case DELETE -> {
                if (belongsToActiveChannel(msg)) applyDelete(msg);
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
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
        messageItems.clear();
        pinnedMessages.clear();
    }

    public void appendMessage(MessageDTO message) {
        boolean isHighlighted = message.getContent() != null &&
                message.getContent().contains("@" + sessionUsername);

        ChatMessageItem item = new ChatMessageItem(message, isHighlighted, sessionUsername, messageActions);

        int insertIndex = chatHistoryPanel.getComponentCount() - 1;
        chatHistoryPanel.add(item, insertIndex);
        chatHistoryPanel.add(Box.createVerticalStrut(10), insertIndex + 1);

        if (message.getMessageId() != null) {
            messageItems.put(message.getMessageId(), item);
        }

        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    // ---------------------------------------------------------------
    // Sửa / Xóa / Ghim tin nhắn (feature #7)
    // ---------------------------------------------------------------

    /** Gửi yêu cầu sửa tin nhắn qua WebSocket. */
    private void sendEdit(MessageDTO original, String newContent) {
        if (!wsClient.isOpen() || original.getMessageId() == null) return;
        MessageDTO out = new MessageDTO(MessageType.EDIT, sessionUsername, null, newContent, LocalDateTime.now());
        out.setMessageId(original.getMessageId());
        out.setChannelId(original.getChannelId());
        out.setServerId(original.getServerId());
        out.setReceiver(original.getReceiver());
        wsClient.send(out).whenComplete((ws, err) -> {
            if (err != null) SwingUtilities.invokeLater(() -> appendSystem("Sửa thất bại: " + err.getMessage()));
        });
    }

    /** Gửi yêu cầu xóa tin nhắn qua WebSocket. */
    private void sendDelete(MessageDTO original) {
        if (!wsClient.isOpen() || original.getMessageId() == null) return;
        MessageDTO out = new MessageDTO(MessageType.DELETE, sessionUsername, null, null, LocalDateTime.now());
        out.setMessageId(original.getMessageId());
        out.setChannelId(original.getChannelId());
        out.setServerId(original.getServerId());
        out.setReceiver(original.getReceiver());
        wsClient.send(out).whenComplete((ws, err) -> {
            if (err != null) SwingUtilities.invokeLater(() -> appendSystem("Xóa thất bại: " + err.getMessage()));
        });
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

    /** Mở popup danh sách tin nhắn đã ghim của kênh đang mở. */
    private void openPinnedDialog() {
        PinnedMessagesDialog dlg = new PinnedMessagesDialog(this, pinnedMessages, this::unpinMessage);
        dlg.setVisible(true);
    }

    /** Bỏ ghim 1 tin nhắn. */
    private void unpinMessage(MessageDTO m) {
        if (m.getMessageId() != null) {
            pinnedMessages.removeIf(p -> m.getMessageId().equals(p.getMessageId()));
        } else {
            pinnedMessages.remove(m);
        }
    }

    /** Ghim tin nhắn (hiển thị trong PinnedMessagesDialog). */
    private void pinMessage(MessageDTO msg) {
        if (msg.getMessageId() != null) {
            boolean exists = pinnedMessages.stream()
                    .anyMatch(m -> msg.getMessageId().equals(m.getMessageId()));
            if (!exists) pinnedMessages.add(msg);
        }
        appendSystem("📌 Đã ghim tin nhắn của " + msg.getSender());
    }

    /** Tìm và mở chỉnh sửa tin nhắn cuối cùng của chính mình trong kênh đang mở. */
    private void editLastOwnMessage() {
        ChatMessageItem target = null;
        for (ChatMessageItem item : messageItems.values()) {
            MessageDTO m = item.getMessage();
            if (m != null && sessionUsername.equals(m.getSender())) {
                target = item; // LinkedHashMap giữ thứ tự chèn → cái cuối là mới nhất
            }
        }
        if (target != null) target.startEditing();
    }

    /** Áp dụng broadcast EDIT: cập nhật nội dung item tại chỗ. */
    private void applyEdit(MessageDTO msg) {
        if (msg.getMessageId() == null) return;
        ChatMessageItem item = messageItems.get(msg.getMessageId());
        if (item != null) {
            item.updateContent(msg.getContent(), true);
            chatHistoryPanel.revalidate();
            chatHistoryPanel.repaint();
        }
    }

    /** Áp dụng broadcast DELETE: gỡ item khỏi danh sách hiển thị. */
    private void applyDelete(MessageDTO msg) {
        if (msg.getMessageId() == null) return;
        ChatMessageItem item = messageItems.remove(msg.getMessageId());
        if (item == null) return;

        Component[] comps = chatHistoryPanel.getComponents();
        int idx = -1;
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] == item) { idx = i; break; }
        }
        if (idx < 0) return;
        chatHistoryPanel.remove(item);
        // Xóa luôn strut đệm ngay sau tin nhắn (nếu có)
        if (idx < chatHistoryPanel.getComponentCount()) {
            Component next = chatHistoryPanel.getComponent(idx);
            if (next instanceof Box.Filler) chatHistoryPanel.remove(next);
        }
        pinnedMessages.removeIf(m -> msg.getMessageId().equals(m.getMessageId()));
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
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
