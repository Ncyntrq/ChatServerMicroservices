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

    private LongConsumer onServerSelected;
    private long activeServerId = -1;

    public void setOnServerSelected(LongConsumer onServerSelected) {
        this.onServerSelected = onServerSelected;
    }

    public ServerSidebar() {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_TERTIARY);
        setPreferredSize(new Dimension(72, 0));

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

        listPanel.add(Box.createVerticalStrut(10));
        ServerIconItem homeBtn = new ServerIconItem("💬");
        homeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        homeBtn.setActive(activeServerId == -1);
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

        for (Map<String, Object> server : servers) {
            long id = asLong(server.get("id"));
            String name = str(server.get("name"));
            String symbol = (name == null || name.isBlank()) ? "?" : name.substring(0, 1).toUpperCase();

            ServerIconItem item = new ServerIconItem(symbol);
            item.setAlignmentX(Component.CENTER_ALIGNMENT);
            item.setActive(id == activeServerId);
            item.setOnClick(() -> {
                activeServerId = id;
                if (onServerSelected != null) onServerSelected.accept(id);
                refreshActiveStates();
            });
            item.setOnContextMenu(() -> {
                Window owner = SwingUtilities.getWindowAncestor(this);
                new ServerSettingsDialog(owner, id, this::loadServers).setVisible(true);
            });
            listPanel.add(item);
        }

        listPanel.add(Box.createVerticalStrut(5));
        ServerIconItem addBtn = new ServerIconItem("➕");
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addBtn.setOnClick(() -> showAddServerMenu(addBtn));
        listPanel.add(addBtn);

        listPanel.add(Box.createVerticalGlue());
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void refreshActiveStates() {
        loadServers();
    }

    private void showAddServerMenu(Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem createItem = new JMenuItem("Tạo Server Mới");
        JMenuItem joinItem = new JMenuItem("Tham Gia Server");
        Window owner = SwingUtilities.getWindowAncestor(this);
        createItem.addActionListener(e ->
                new CreateServerDialog(owner, this::loadServers).setVisible(true));
        joinItem.addActionListener(e ->
                new JoinServerDialog(owner, this::loadServers).setVisible(true));
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
