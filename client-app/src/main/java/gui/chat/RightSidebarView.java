package gui.chat;

import gui.components.chat.ChannelAttachment;
import gui.components.chat.CollapsibleSection;
import gui.components.chat.FileListPanel;
import gui.components.chat.MediaGalleryPanel;
import gui.components.chat.SidebarCategoryHeader;
import gui.components.chat.UserListItem;
import gui.theme.AppColors;
import gui.theme.ThinScrollBarUI;
import network.ApiConfig;
import network.FileApiClient;
import network.PermissionCache;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Sidebar phải khi ở Server/Channel hoặc DM. Gồm 3 section thu gọn được:
 *  - Ảnh/Video (mở sẵn) — lưới thumbnail từ file của channel
 *  - File (mở sẵn) — danh sách tài liệu
 *  - Thành viên (đóng sẵn) — danh sách online/offline (chỉ ở Server, ẩn ở DM)
 */
public class RightSidebarView extends JScrollPane {

    private final String sessionUsername;
    private final Consumer<String> onAssignRole;
    private final Consumer<String> onKick;

    private final MediaGalleryPanel mediaPanel = new MediaGalleryPanel();
    private final FileListPanel filePanel = new FileListPanel();
    private final JPanel memberContent = new JPanel();
    private final CollapsibleSection mediaSection;
    private final CollapsibleSection fileSection;
    private final CollapsibleSection memberSection;

    /** Map username → UserListItem để cập nhật trạng thái nhanh khi có STATUS event. */
    private final Map<String, UserListItem> memberItems = new HashMap<>();
    /** Chặn race: chỉ áp kết quả tải media của channel đang xem. */
    private long loadingChannelId = -1;

    public RightSidebarView(String sessionUsername, Consumer<String> onAssignRole, Consumer<String> onKick) {
        this.sessionUsername = sessionUsername;
        this.onAssignRole = onAssignRole;
        this.onKick = onKick;

        memberContent.setLayout(new BoxLayout(memberContent, BoxLayout.Y_AXIS));
        memberContent.setOpaque(false);
        memberContent.setAlignmentX(Component.LEFT_ALIGNMENT);

        mediaSection = new CollapsibleSection("Ảnh/Video", mediaPanel, true);
        fileSection = new CollapsibleSection("File", filePanel, true);
        memberSection = new CollapsibleSection("Thành viên", memberContent, false);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_SECONDARY);
        listPanel.add(Box.createVerticalStrut(10));
        listPanel.add(memberSection);
        listPanel.add(Box.createVerticalStrut(8));
        listPanel.add(mediaSection);
        listPanel.add(Box.createVerticalStrut(8));
        listPanel.add(fileSection);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(AppColors.BG_SECONDARY);
        wrap.add(listPanel, BorderLayout.NORTH);

        setViewportView(wrap);
        setBorder(BorderFactory.createEmptyBorder());
        setPreferredSize(new Dimension(240, 0));
        getVerticalScrollBar().setUnitIncrement(16);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ThinScrollBarUI.apply(this);
    }

    // ---------------------------------------------------------------
    // Thành viên (logic cũ của MemberListView, đặt trong section Thành viên)
    // ---------------------------------------------------------------

    /** Hiện/ẩn section Thành viên (ẩn khi ở DM). */
    public void setMembersVisible(boolean visible) {
        memberSection.setVisible(visible);
        revalidate();
        repaint();
    }

    public void renderServerMembers(List<String> allUsers, List<String> onlineUsers, String ownerId) {
        memberContent.removeAll();
        memberItems.clear();

        List<String> onlineList = new ArrayList<>();
        List<String> offlineList = new ArrayList<>();
        for (String u : allUsers) {
            if (onlineUsers.contains(u)) onlineList.add(u);
            else offlineList.add(u);
        }
        boolean isOwner = sessionUsername.equals(ownerId);

        memberContent.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + onlineList.size()));
        memberContent.add(Box.createVerticalStrut(5));
        for (String username : onlineList) addMemberItem(username, AppColors.STATUS_ONLINE, true, isOwner);

        memberContent.add(Box.createVerticalStrut(12));
        memberContent.add(new SidebarCategoryHeader("NGOẠI TUYẾN — " + offlineList.size()));
        memberContent.add(Box.createVerticalStrut(5));
        for (String username : offlineList) addMemberItem(username, AppColors.STATUS_OFFLINE, false, isOwner);

        memberSection.setTitle("Thành viên — " + allUsers.size());
        memberContent.revalidate();
        memberContent.repaint();
    }

    public void renderOnline(List<String> usernames) {
        memberContent.removeAll();
        memberItems.clear();
        memberContent.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + usernames.size()));
        memberContent.add(Box.createVerticalStrut(5));
        for (String username : usernames) {
            if (username == null || username.isBlank()) continue;
            String name = username.trim();
            UserListItem item = new UserListItem(name, null, AppColors.STATUS_ONLINE, true);
            memberItems.put(name, item);
            memberContent.add(item);
        }
        memberSection.setTitle("Thành viên — " + usernames.size());
        memberContent.revalidate();
        memberContent.repaint();
    }

    public void updateUserStatus(String username, String statusStr) {
        SwingUtilities.invokeLater(() -> {
            UserListItem item = memberItems.get(username);
            if (item != null) item.updatePresenceStatus(statusStr);
        });
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
        memberItems.put(name, item);
        memberContent.add(item);
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
        if (menu.getComponentCount() > 0) menu.show(anchor, anchor.getWidth() / 2, anchor.getHeight() / 2);
    }

    // ---------------------------------------------------------------
    // Ảnh/Video & File
    // ---------------------------------------------------------------

    /** Nạp media/file của 1 channel từ backend (mới nhất trước). */
    public void loadChannelMedia(long channelId) {
        loadingChannelId = channelId;
        setAttachments(List.of()); // clear ngay, chờ kết quả
        new SwingWorker<List<ChannelAttachment>, Void>() {
            @Override protected List<ChannelAttachment> doInBackground() {
                return mapAttachments(new FileApiClient().getFilesByChannel(channelId, null, 100));
            }
            @Override protected void done() {
                if (channelId != loadingChannelId) return; // đã chuyển channel khác
                try { setAttachments(get()); }
                catch (Exception ignore) { /* giữ trạng thái rỗng nếu lỗi */ }
            }
        }.execute();
    }

    /** Đặt danh sách đính kèm sẵn (dùng cho DM — trích từ tin nhắn đã tải). */
    public void setAttachments(List<ChannelAttachment> all) {
        List<ChannelAttachment> images = new ArrayList<>();
        List<ChannelAttachment> files = new ArrayList<>();
        if (all != null) for (ChannelAttachment a : all) (a.isImage() ? images : files).add(a);
        mediaPanel.setImages(images);
        filePanel.setFiles(files);
    }

    /** Map JSON metadata (url tương đối) → ChannelAttachment (url đầy đủ qua gateway). */
    private List<ChannelAttachment> mapAttachments(List<Map<String, Object>> raw) {
        List<ChannelAttachment> out = new ArrayList<>();
        if (raw == null) return out;
        for (Map<String, Object> m : raw) {
            String url = full(str(m.get("url")));
            String thumb = full(str(m.get("thumbnailUrl")));
            out.add(new ChannelAttachment(
                    str(m.get("contentType")), str(m.get("originalName")),
                    url, thumb, asLong(m.get("fileSize")), parseDate(m.get("createdAt"))));
        }
        return out;
    }

    private static String full(String relativeUrl) {
        return relativeUrl == null ? null : ApiConfig.GATEWAY_HTTP + relativeUrl;
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private static long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try { return o == null ? 0L : Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    private static LocalDateTime parseDate(Object o) {
        if (o == null) return null;
        try { return LocalDateTime.parse(o.toString()); } catch (Exception e) { return null; }
    }
}
