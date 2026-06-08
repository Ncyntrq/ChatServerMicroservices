package gui.chat;

import gui.components.chat.SidebarCategoryHeader;
import gui.components.chat.UserListItem;
import gui.theme.AppColors;
import gui.theme.ThinScrollBarUI;
import network.PermissionCache;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Thanh thành viên (EAST) hiển thị danh sách TRỰC TUYẾN / NGOẠI TUYẾN.
 * Context menu (Cấp Role / Kick) chỉ gắn cho chủ server, ủy quyền hành động
 * qua callback để frame xử lý dialog + API.
 */
public class MemberListView extends JScrollPane {

    private final JPanel listPanel;
    private final String sessionUsername;
    private final Consumer<String> onAssignRole;
    private final Consumer<String> onKick;

    public MemberListView(String sessionUsername, Consumer<String> onAssignRole, Consumer<String> onKick) {
        this.sessionUsername = sessionUsername;
        this.onAssignRole = onAssignRole;
        this.onKick = onKick;

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_SECONDARY);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(AppColors.BG_SECONDARY);
        wrap.add(listPanel, BorderLayout.NORTH);

        setViewportView(wrap);
        setBorder(BorderFactory.createEmptyBorder());
        setPreferredSize(new Dimension(240, 0));
        getVerticalScrollBar().setUnitIncrement(16);
        ThinScrollBarUI.apply(this);
    }

    /** Render danh sách thành viên 1 server, tách online/offline; gắn context menu nếu là chủ server. */
    public void renderServerMembers(List<String> allUsers, List<String> onlineUsers, String ownerId) {
        listPanel.removeAll();
        listPanel.add(Box.createVerticalStrut(15));

        List<String> onlineList = new java.util.ArrayList<>();
        List<String> offlineList = new java.util.ArrayList<>();
        for (String u : allUsers) {
            if (onlineUsers.contains(u)) onlineList.add(u);
            else offlineList.add(u);
        }

        boolean isOwner = sessionUsername.equals(ownerId);

        listPanel.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + onlineList.size()));
        listPanel.add(Box.createVerticalStrut(5));
        for (String username : onlineList) {
            addMemberItem(username, AppColors.STATUS_ONLINE, true, isOwner);
        }

        listPanel.add(Box.createVerticalStrut(15));

        listPanel.add(new SidebarCategoryHeader("NGOẠI TUYẾN — " + offlineList.size()));
        listPanel.add(Box.createVerticalStrut(5));
        for (String username : offlineList) {
            addMemberItem(username, AppColors.STATUS_OFFLINE, false, isOwner);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void addMemberItem(String username, Color statusColor, boolean online, boolean isOwner) {
        if (username == null || username.isBlank()) return;
        String name = username.trim();
        UserListItem item = new UserListItem(name, null, statusColor, online);
        boolean canModerate = isOwner
                || PermissionCache.get().can(PermissionCache.KICK_MEMBER)
                || PermissionCache.get().can(PermissionCache.MANAGE_ROLES);
        if (canModerate && !name.equals(sessionUsername)) {
            item.setOnContextMenu(() -> showContextMenu(item, name, isOwner));
        }
        listPanel.add(item);
    }

    /** Render danh sách online đơn giản (màn hình Home, không thuộc server nào). */
    public void renderOnline(List<String> usernames) {
        listPanel.removeAll();
        listPanel.add(Box.createVerticalStrut(15));
        listPanel.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + usernames.size()));
        listPanel.add(Box.createVerticalStrut(5));
        for (String username : usernames) {
            if (username == null || username.isBlank()) continue;
            listPanel.add(new UserListItem(username.trim(), null, AppColors.STATUS_ONLINE, true));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void showContextMenu(Component anchor, String username, boolean isOwner) {
        JPopupMenu menu = new JPopupMenu();

        if (isOwner || PermissionCache.get().can(PermissionCache.MANAGE_ROLES)) {
            JMenuItem assignRoleItem = new JMenuItem("Cấp Role");
            assignRoleItem.addActionListener(e -> onAssignRole.accept(username));
            menu.add(assignRoleItem);
        }
        if (isOwner || PermissionCache.get().can(PermissionCache.KICK_MEMBER)) {
            JMenuItem kickItem = new JMenuItem("Kick Khỏi Server");
            kickItem.setForeground(AppColors.DANGER);
            kickItem.addActionListener(e -> onKick.accept(username));
            menu.add(kickItem);
        }
        if (menu.getComponentCount() > 0) {
            menu.show(anchor, anchor.getWidth() / 2, anchor.getHeight() / 2);
        }
    }
}
