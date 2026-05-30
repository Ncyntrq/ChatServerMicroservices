package gui.components.mini;

import gui.components.chat.SidebarCategoryHeader;
import gui.components.chat.UserListItem;
import gui.theme.AppColors;
import network.PresenceApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Danh sách bạn bè đang online (from Presence API).
 * Click 1 friend → callback onFriendSelected(username) để mở DM.
 */
public class FriendListPanel extends JPanel {

    private final PresenceApiClient presenceApi = new PresenceApiClient();
    private final JPanel listPanel;
    private Consumer<String> onFriendSelected;

    public void setOnFriendSelected(Consumer<String> onFriendSelected) {
        this.onFriendSelected = onFriendSelected;
    }

    public FriendListPanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_SECONDARY);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_SECONDARY);
        listPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(listPanel, BorderLayout.NORTH);
    }

    /** Tải danh sách online và render. */
    public void refresh() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return presenceApi.getOnlineUsers();
            }

            @Override
            protected void done() {
                try {
                    render(get());
                } catch (Exception ex) {
                    render(List.of());
                }
            }
        }.execute();
    }

    private void render(List<String> friends) {
        listPanel.removeAll();
        listPanel.add(new SidebarCategoryHeader("BẠN BÈ — " + friends.size()));
        listPanel.add(Box.createVerticalStrut(4));

        for (String name : friends) {
            if (name == null || name.isBlank()) continue;
            String username = name.trim();
            UserListItem item = new UserListItem(username, null, AppColors.STATUS_ONLINE);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (onFriendSelected != null) onFriendSelected.accept(username);
                }
            });
            listPanel.add(item);
        }

        if (friends.isEmpty()) {
            JLabel empty = new JLabel("Không có ai online");
            empty.setForeground(AppColors.TEXT_MUTED);
            listPanel.add(empty);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }
}
