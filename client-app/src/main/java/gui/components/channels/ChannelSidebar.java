package gui.components.channels;

import gui.channel.CreateChannelDialog;
import gui.channel.EditChannelDialog;
import gui.components.chat.IconButton;
import gui.components.chat.SidebarCategoryHeader;
import gui.theme.AppColors;
import network.ApiConfig;
import network.ChannelApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

public class ChannelSidebar extends JPanel {

    private final ChannelApiClient channelApi = new ChannelApiClient();
    private final JLabel titleLabel;
    private final JPanel listPanel;
    private final java.util.Map<Long, String> channelNames = new java.util.HashMap<>();
    private final Map<Long, ChannelListItem> channelItems = new java.util.HashMap<>();
    // Cache unread cuối cùng — re-apply sau mỗi lần rebuild để badge không mất khi điều hướng
    private final Map<Long, Integer> lastUnread = new java.util.HashMap<>();
    private UserFooterPanel accountFooter;

    private long activeServerId = -1;
    private LongConsumer onChannelSelected;
    private Runnable onChannelChanged;
    private Runnable onUserChanged;

    public void setOnChannelSelected(LongConsumer onChannelSelected) {
        this.onChannelSelected = onChannelSelected;
    }

    public void setOnChannelChanged(Runnable onChannelChanged) {
        this.onChannelChanged = onChannelChanged;
    }

    public void setOnUserChanged(Runnable onUserChanged) {
        this.onUserChanged = onUserChanged;
    }

    public long getActiveServerId() { return activeServerId; }

    public String getChannelName(long channelId) {
        return channelNames.get(channelId);
    }

    public ChannelSidebar(String sessionUsername) {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_SECONDARY);
        setPreferredSize(new Dimension(240, 0));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppColors.BG_SECONDARY);
        headerPanel.setPreferredSize(new Dimension(240, 48));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BG_PRIMARY));

        titleLabel = new JLabel("Chọn một server");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(AppColors.TEXT_WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_SECONDARY);
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JPanel scrollContentWrapper = new JPanel(new BorderLayout());
        scrollContentWrapper.setBackground(AppColors.BG_SECONDARY);
        scrollContentWrapper.add(listPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(scrollContentWrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        accountFooter = new UserFooterPanel(sessionUsername, () -> {
            if (onUserChanged != null) onUserChanged.run();
        });

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(accountFooter, BorderLayout.SOUTH);
    }

    public void refreshUserFooter() {
        if (accountFooter != null) {
            accountFooter.refreshAvatar();
        }
    }

    public void loadChannels(long serverId, String serverName) {
        this.activeServerId = serverId;
        if (serverName != null) titleLabel.setText(serverName);

        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return channelApi.getChannelsByServer(serverId);
            }

            @Override
            protected void done() {
                try {
                    renderChannels(get());
                } catch (Exception ex) {
                    listPanel.removeAll();
                    JLabel err = new JLabel("Không tải được channels");
                    err.setForeground(AppColors.TEXT_MUTED);
                    listPanel.add(err);
                    listPanel.revalidate();
                    listPanel.repaint();
                }
            }
        }.execute();
    }

    private void renderChannels(List<Map<String, Object>> channels) {
        listPanel.removeAll();
        channelItems.clear();

        boolean addedTextHeader = false;
        boolean addedVoiceHeader = false;

        for (Map<String, Object> ch : channels) {
            if (!"VOICE".equalsIgnoreCase(str(ch.get("type")))) {
                if (!addedTextHeader) {
                    listPanel.add(new SidebarCategoryHeader("KÊNH CHAT", () -> {
                        Window owner = SwingUtilities.getWindowAncestor(this);
                        new CreateChannelDialog(owner, activeServerId,
                                () -> {
                                    loadChannels(activeServerId, titleLabel.getText());
                                    if (onChannelChanged != null) onChannelChanged.run();
                                }).setVisible(true);
                    }));
                    listPanel.add(Box.createVerticalStrut(4));
                    addedTextHeader = true;
                }
                listPanel.add(buildItem(ch, false));
                listPanel.add(Box.createVerticalStrut(4));
            }
        }

        for (Map<String, Object> ch : channels) {
            if ("VOICE".equalsIgnoreCase(str(ch.get("type")))) {
                if (!addedVoiceHeader) {
                    listPanel.add(Box.createVerticalStrut(12));
                    listPanel.add(new SidebarCategoryHeader("KÊNH THOẠI"));
                    listPanel.add(Box.createVerticalStrut(4));
                    addedVoiceHeader = true;
                }
                listPanel.add(buildItem(ch, true));
                listPanel.add(Box.createVerticalStrut(4));
            }
        }

        if (!addedTextHeader && !addedVoiceHeader) {
            listPanel.add(new SidebarCategoryHeader("KÊNH CHAT", () -> {
                Window owner = SwingUtilities.getWindowAncestor(this);
                new CreateChannelDialog(owner, activeServerId,
                        () -> {
                            loadChannels(activeServerId, titleLabel.getText());
                            if (onChannelChanged != null) onChannelChanged.run();
                        }).setVisible(true);
            }));
            listPanel.add(Box.createVerticalStrut(4));
            JLabel empty = new JLabel("Chưa có channel");
            empty.setForeground(AppColors.TEXT_MUTED);
            listPanel.add(empty);
        }

        applyUnread(); // re-apply badge unread đã cache lên item vừa rebuild
        listPanel.revalidate();
        listPanel.repaint();
    }

    public void updateUnreadCounts(Map<Long, Integer> unreadCounts) {
        SwingUtilities.invokeLater(() -> {
            lastUnread.clear();
            lastUnread.putAll(unreadCounts);
            applyUnread();
        });
    }

    /** Áp dụng unread đã cache lên các item hiện có (giữ badge qua các lần rebuild). */
    private void applyUnread() {
        for (Map.Entry<Long, ChannelListItem> entry : channelItems.entrySet()) {
            Integer count = lastUnread.get(entry.getKey());
            entry.getValue().setUnreadCount(count != null ? count : 0);
        }
    }

    private ChannelListItem buildItem(Map<String, Object> ch, boolean isVoice) {
        long id = asLong(ch.get("id"));
        String name = str(ch.get("name"));
        String topic = str(ch.get("topic"));
        if (name != null) channelNames.put(id, name);
        ChannelListItem item = new ChannelListItem(name != null ? name : "channel", isVoice);
        item.setOnClick(() -> {
            if (onChannelSelected != null) onChannelSelected.accept(id);
        });
        item.setOnContextMenu(() -> showChannelMenu(item, id, name, topic));
        
        channelItems.put(id, item);
        return item;
    }

    private void showChannelMenu(Component anchor, long channelId, String name, String topic) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Sửa");
        JMenuItem deleteItem = new JMenuItem("Xóa");
        Window owner = SwingUtilities.getWindowAncestor(this);

        editItem.addActionListener(e ->
                new EditChannelDialog(owner, channelId, name, topic,
                        () -> {
                            loadChannels(activeServerId, titleLabel.getText());
                            if (onChannelChanged != null) onChannelChanged.run();
                        }).setVisible(true));

        deleteItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xóa channel \"" + name + "\"?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    channelApi.deleteChannel(channelId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadChannels(activeServerId, titleLabel.getText());
                        if (onChannelChanged != null) onChannelChanged.run();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ChannelSidebar.this,
                                "Lỗi xóa channel: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        menu.add(editItem);
        menu.add(deleteItem);
        menu.show(anchor, 0, anchor.getHeight());
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
