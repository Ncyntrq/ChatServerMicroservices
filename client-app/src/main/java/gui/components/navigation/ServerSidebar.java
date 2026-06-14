package gui.components.navigation;

import gui.server.CreateServerDialog;
import gui.server.JoinServerDialog;
import gui.server.ServerSettingsDialog;
import gui.theme.AppColors;
import network.ServerApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

/**
 * Thanh server bên trái (72px). Load danh sách server từ API và render động.
 * - Click icon server → callback onServerSelected(serverId)
 * - Right-click icon → mở ServerSettingsDialog
 * - Nút ➕ → popup Tạo / Tham gia server
 */
public class ServerSidebar extends JPanel {

    private final JPanel listPanel;
    private final ServerApiClient serverApi = new ServerApiClient();
    private final Map<Long, ServerIconItem> serverItems = new java.util.HashMap<>();
    private final Map<Long, String> serverNames = new java.util.HashMap<>();
    private final Map<Long, String> serverIconUrls = new java.util.HashMap<>();
    // Cache unread cuối cùng — re-apply sau rebuild để badge không mất khi điều hướng
    private final Map<Long, Integer> lastUnread = new java.util.HashMap<>();

    private final java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(ServerSidebar.class);
    private final java.util.List<Long> pinnedServerIds = new java.util.LinkedList<>();

    private BiConsumer<Long, String> onServerSelected;
    private java.util.function.Consumer<Long> onServerChanged;
    private long activeServerId = -1;

    public void setOnServerSelected(BiConsumer<Long, String> onServerSelected) {
        this.onServerSelected = onServerSelected;
    }

    public void setOnServerChanged(java.util.function.Consumer<Long> onServerChanged) {
        this.onServerChanged = onServerChanged;
    }

    public ServerSidebar() {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_TERTIARY);
        setPreferredSize(new Dimension(72, 0));

        String saved = prefs.get("pinned_servers", "");
        if (!saved.isEmpty()) {
            for (String s : saved.split(",")) {
                try {
                    long id = Long.parseLong(s);
                    if (!pinnedServerIds.contains(id)) pinnedServerIds.add(id);
                } catch (Exception ignore) {}
            }
        }

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_TERTIARY);

        add(listPanel, BorderLayout.CENTER);
        renderServers(List.of()); // khung rỗng ban đầu (HOME + ➕)
    }

    /** Tải danh sách server từ API rồi render lại. */
    public void loadServers() {
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return serverApi.getMyServers();
            }

            @Override
            protected void done() {
                try {
                    renderServers(get());
                } catch (Exception ex) {
                    renderServers(List.of());
                }
            }
        }.execute();
    }

    /** Render lại toàn bộ: HOME, separator, từng server, nút ➕. */
    private void renderServers(List<Map<String, Object>> servers) {
        listPanel.removeAll();
        serverItems.clear();
        serverNames.clear();
        serverIconUrls.clear();

        listPanel.add(Box.createVerticalStrut(10));
        ServerIconItem homeBtn = new ServerIconItem("💬");
        homeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        homeBtn.setActive(activeServerId == -1);
        homeBtn.setOnClick(() -> {
            activeServerId = -1;
            if (onServerSelected != null) onServerSelected.accept(-1L, null);
            refreshActiveStates();
        });
        listPanel.add(homeBtn);

        listPanel.add(Box.createVerticalStrut(5));
        JPanel separator = new JPanel() {
            @Override public Dimension getPreferredSize() { return new Dimension(32, 2); }
            @Override public Dimension getMinimumSize()   { return new Dimension(32, 2); }
            @Override public Dimension getMaximumSize()   { return new Dimension(32, 2); }
        };
        separator.setAlignmentX(Component.CENTER_ALIGNMENT);
        separator.setBackground(AppColors.BG_HOVER);
        listPanel.add(separator);
        listPanel.add(Box.createVerticalStrut(5));

        // Tách danh sách thành Ghim và Chưa ghim
        java.util.List<Map<String, Object>> pinnedServers = new java.util.ArrayList<>();
        java.util.List<Map<String, Object>> unpinnedServers = new java.util.ArrayList<>();

        Map<Long, Map<String, Object>> serverMap = new java.util.HashMap<>();
        for (Map<String, Object> server : servers) {
            serverMap.put(asLong(server.get("id")), server);
        }

        for (Long id : pinnedServerIds) {
            Map<String, Object> server = serverMap.remove(id);
            if (server != null) {
                pinnedServers.add(server);
            }
        }
        unpinnedServers.addAll(serverMap.values());

        for (Map<String, Object> server : pinnedServers) {
            renderServerIcon(server, true);
        }

        if (!pinnedServers.isEmpty() && !unpinnedServers.isEmpty()) {
            listPanel.add(Box.createVerticalStrut(5));
            JPanel separator2 = new JPanel() {
                @Override public Dimension getPreferredSize() { return new Dimension(32, 2); }
                @Override public Dimension getMinimumSize()   { return new Dimension(32, 2); }
                @Override public Dimension getMaximumSize()   { return new Dimension(32, 2); }
            };
            separator2.setAlignmentX(Component.CENTER_ALIGNMENT);
            separator2.setBackground(AppColors.BG_HOVER);
            listPanel.add(separator2);
            listPanel.add(Box.createVerticalStrut(5));
        }

        for (Map<String, Object> server : unpinnedServers) {
            renderServerIcon(server, false);
        }

        listPanel.add(Box.createVerticalStrut(5));
        ServerIconItem addBtn = new ServerIconItem("➕");
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addBtn.setOnClick(() -> showAddServerMenu(addBtn));
        listPanel.add(addBtn);

        listPanel.add(Box.createVerticalGlue());
        applyUnread(); // re-apply badge unread đã cache lên item vừa rebuild
        listPanel.revalidate();
        listPanel.repaint();

        // Notify ChatClientGUI of the latest name if a server is currently active
        if (activeServerId != -1 && onServerSelected != null) {
            for (Map<String, Object> server : servers) {
                if (asLong(server.get("id")) == activeServerId) {
                    onServerSelected.accept(activeServerId, str(server.get("name")));
                    break;
                }
            }
        }
    }

    private void renderServerIcon(Map<String, Object> server, boolean isPinned) {
        long id = asLong(server.get("id"));
        String name = str(server.get("name"));
        String iconUrl = str(server.get("icon"));
        serverNames.put(id, name);
        serverIconUrls.put(id, iconUrl);
        String symbol = (name == null || name.isBlank()) ? "?" : name.substring(0, 1).toUpperCase();

        ServerIconItem item = new ServerIconItem(symbol);
        serverItems.put(id, item);
        item.setAlignmentX(Component.CENTER_ALIGNMENT);
        item.setActive(id == activeServerId);

        if (iconUrl != null && !iconUrl.isBlank()) {
            if (!iconUrl.startsWith("http")) iconUrl = network.ApiConfig.GATEWAY_HTTP + iconUrl;
            item.loadServerIconFromUrl(iconUrl);
        }

        item.setOnClick(() -> {
            activeServerId = id;
            if (onServerSelected != null) onServerSelected.accept(id, name);
            refreshActiveStates();
        });

        item.setOnContextMenu(() -> {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem pinItem = new JMenuItem(isPinned ? "Bỏ ghim máy chủ" : "Ghim máy chủ");
            pinItem.addActionListener(e -> {
                if (isPinned) {
                    pinnedServerIds.remove(id);
                } else {
                    pinnedServerIds.remove(id); // Xóa khỏi vị trí cũ nếu có
                    pinnedServerIds.add(0, id); // Lên đầu (LIFO)
                }
                savePinnedServers();
                loadServers();
            });
            menu.add(pinItem);
            menu.show(item, item.getWidth(), item.getHeight() / 2);
        });

        listPanel.add(item);
    }

    private void savePinnedServers() {
        String s = pinnedServerIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        prefs.put("pinned_servers", s);
    }

    public void updateUnreadCounts(Map<Long, Integer> unreadCounts) {
        SwingUtilities.invokeLater(() -> {
            lastUnread.clear();
            lastUnread.putAll(unreadCounts);
            applyUnread();
        });
    }

    // --- ĐÃ THÊM HÀM NÀY ĐỂ BÊN NGOÀI GỌI ---
    public void selectHome() {
        this.activeServerId = -1;
        refreshActiveStates();
    }

    public String getServerName(long id) {
        return serverNames.get(id);
    }

    public String getServerIconUrl(long id) {
        return serverIconUrls.get(id);
    }

    /** Áp dụng unread đã cache lên các icon server hiện có. */
    private void applyUnread() {
        for (Map.Entry<Long, ServerIconItem> entry : serverItems.entrySet()) {
            Integer count = lastUnread.get(entry.getKey());
            entry.getValue().setUnreadCount(count != null ? count : 0);
        }
    }

    private void refreshActiveStates() {
        loadServers();
    }

    private void showAddServerMenu(Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem createItem = new JMenuItem("Create Server");
        JMenuItem joinItem = new JMenuItem("Join Server");
        Window owner = SwingUtilities.getWindowAncestor(this);
        createItem.addActionListener(e ->
                new CreateServerDialog(owner, () -> {
                    loadServers();
                    // Tạo mới thì không có ai khác trong server, có thể broadcast với -1 hoặc bỏ qua
                    if (onServerChanged != null) onServerChanged.accept(-1L);
                }).setVisible(true));
        joinItem.addActionListener(e ->
                new JoinServerDialog(owner, (joinedServerId) -> {
                    loadServers();
                    if (onServerChanged != null && joinedServerId != -1) onServerChanged.accept(joinedServerId);
                }).setVisible(true));
        menu.add(createItem);
        menu.add(joinItem);
        menu.show(anchor, anchor.getWidth(), 0);
    }

    private static long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o != null) {
            try { return Long.parseLong(o.toString()); } catch (NumberFormatException ignore) {}
        }
        return -1;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }
}