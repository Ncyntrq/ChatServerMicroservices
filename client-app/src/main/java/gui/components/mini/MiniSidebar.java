package gui.components.mini;

import gui.theme.AppColors;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Thanh sidebar thu gọn (toggle show/hide) gồm:
 * - FriendListPanel: bạn bè online
 * - JoinedServerListPanel: server đã tham gia
 */
public class MiniSidebar extends JPanel {

    private final FriendListPanel friendListPanel = new FriendListPanel();
    private final JoinedServerListPanel serverListPanel = new JoinedServerListPanel();

    public MiniSidebar() {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_SECONDARY);
        setPreferredSize(new Dimension(220, 0));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, AppColors.BG_PRIMARY));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppColors.BG_SECONDARY);

        friendListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        serverListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(friendListPanel);
        content.add(Box.createVerticalStrut(12));
        content.add(serverListPanel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    public void setOnFriendSelected(Consumer<String> cb) {
        friendListPanel.setOnFriendSelected(cb);
    }

    public void setOnServerSelected(BiConsumer<Long, String> cb) {
        serverListPanel.setOnServerSelected(cb);
    }

    /** Tải lại cả 2 danh sách. */
    public void refresh() {
        friendListPanel.refresh();
        serverListPanel.refresh();
    }
}
