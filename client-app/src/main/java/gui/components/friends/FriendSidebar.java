package gui.components.friends;

import gui.components.channels.UserFooterPanel;
import gui.components.AppIcons;
import gui.components.chat.IconButton;
import gui.components.chat.SidebarCategoryHeader;
import gui.components.chat.UserListItem;
import gui.theme.AppColors;
import network.FriendApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class FriendSidebar extends JPanel {

    private final FriendApiClient friendApi = new FriendApiClient();
    private final JLabel titleLabel;
    private final JPanel listPanel;
    private final String sessionUsername;

    private Consumer<String> onFriendSelected;
    private java.util.function.Consumer<String> onFriendAction;
    private Runnable onUserChanged;

    // Đảm bảo chỉ 1 dialog "Thêm bạn" mở tại 1 thời điểm.
    private AddFriendDialog addFriendDialog;

    public void setOnFriendSelected(java.util.function.Consumer<String> onFriendSelected) {
        this.onFriendSelected = onFriendSelected;
    }
    
    public void setOnFriendAction(Consumer<String> onFriendAction) {
        this.onFriendAction = onFriendAction;
    }

    public void setOnUserChanged(Runnable onUserChanged) {
        this.onUserChanged = onUserChanged;
    }

    public FriendSidebar(String sessionUsername) {
        this.sessionUsername = sessionUsername;
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_SECONDARY);
        setPreferredSize(new Dimension(240, 0));

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppColors.BG_SECONDARY);
        headerPanel.setPreferredSize(new Dimension(240, 48));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BG_PRIMARY));

        titleLabel = new JLabel("Friends");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(AppColors.TEXT_WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        IconButton addFriendBtn = new IconButton(AppIcons.plus(14), e -> showAddFriendDialog());
        addFriendBtn.setToolTipText("Thêm bạn bè");
        JPanel addWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        addWrap.setOpaque(false);
        addWrap.add(addFriendBtn);
        headerPanel.add(addWrap, BorderLayout.EAST);

        // --- LIST ---
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
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gui.theme.ThinScrollBarUI.apply(scrollPane);

        // --- FOOTER ---
        accountFooter = new UserFooterPanel(sessionUsername, () -> {
            if (onUserChanged != null) onUserChanged.run();
        });

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(accountFooter, BorderLayout.SOUTH);
    }

    private UserFooterPanel accountFooter;

    public void refreshUserFooter() {
        if (accountFooter != null) {
            accountFooter.refreshAvatar();
        }
    }

    public void loadFriendsAndRequests(java.util.Map<String, String> onlineStatuses) {
        new SwingWorker<Void, Void>() {
            List<String> friends;
            List<String> pending;

            @Override
            protected Void doInBackground() {
                friends = friendApi.getFriends();
                pending = friendApi.getPendingRequests();
                return null;
            }

            @Override
            protected void done() {
                try {
                    renderLists(friends, pending, onlineStatuses);
                } catch (Exception ex) {
                    listPanel.removeAll();
                    JLabel err = new JLabel("Failed to load friends");
                    err.setForeground(AppColors.TEXT_MUTED);
                    listPanel.add(err);
                    listPanel.revalidate();
                    listPanel.repaint();
                }
            }
        }.execute();
    }

    private final java.util.Map<String, UserListItem> friendItems = new java.util.HashMap<>();
    // Cache unread cuối cùng — re-apply sau rebuild để badge không mất khi điều hướng
    private final java.util.Map<String, Integer> lastUnread = new java.util.HashMap<>();

    public void updateUnreadCounts(java.util.Map<String, Integer> unreadCounts) {
        SwingUtilities.invokeLater(() -> {
            lastUnread.clear();
            lastUnread.putAll(unreadCounts);
            applyUnread();
        });
    }

    /** Áp dụng unread đã cache lên các item bạn bè hiện có. */
    private void applyUnread() {
        for (java.util.Map.Entry<String, UserListItem> entry : friendItems.entrySet()) {
            Integer count = lastUnread.get(entry.getKey());
            entry.getValue().setUnreadCount(count != null ? count : 0);
        }
    }

    /**
     * Cập nhật trạng thái real-time cho 1 người bạn khi nhận STATUS WebSocket event.
     * Chỉ repaint item tìm được, không rebuild toàn danh sách.
     */
    public void updateUserStatus(String username, String statusStr) {
        SwingUtilities.invokeLater(() -> {
            UserListItem item = friendItems.get(username);
            if (item != null) {
                item.updatePresenceStatus(statusStr);
            }
        });
    }


    private void renderLists(List<String> friends, List<String> pending, java.util.Map<String, String> onlineStatuses) {
        listPanel.removeAll();
        friendItems.clear();

        if ((pending == null || pending.isEmpty()) && (friends == null || friends.isEmpty())) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
            emptyPanel.setOpaque(false);
            
            JLabel iconLabel = new JLabel("📭");
            iconLabel.setFont(gui.theme.AppFonts.EMOJI.deriveFont(28f));
            iconLabel.setForeground(AppColors.TEXT_MUTED);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JLabel textLabel = new JLabel("No friends yet");
            textLabel.setFont(gui.theme.AppFonts.BODY_SM);
            textLabel.setForeground(AppColors.TEXT_MUTED);
            textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            emptyPanel.add(iconLabel);
            emptyPanel.add(Box.createVerticalStrut(8));
            emptyPanel.add(textLabel);
            
            listPanel.add(Box.createVerticalGlue());
            listPanel.add(emptyPanel);
            listPanel.add(Box.createVerticalGlue());
            listPanel.revalidate();
            listPanel.repaint();
            return;
        }

        // 1. Pending Requests
        if (pending != null && !pending.isEmpty()) {
            listPanel.add(new SidebarCategoryHeader("PENDING — " + pending.size()));
            listPanel.add(Box.createVerticalStrut(4));
            for (String req : pending) {
                JPanel itemPanel = new JPanel();
                itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
                itemPanel.setOpaque(false);
                
                // Make it look like a card
                JPanel card = new JPanel(new BorderLayout());
                card.setBackground(AppColors.BG_FLOATING);
                card.setBorder(BorderFactory.createCompoundBorder(
                        gui.theme.AppBorders.rounded(AppColors.SEPARATOR, 12, 1, 1),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));

                UserListItem item = new UserListItem(req, "Incoming Request", AppColors.STATUS_OFFLINE, true);
                item.setOpaque(false); // Let card background show
                
                card.add(item, BorderLayout.CENTER);
                
                JPanel btnPanel = new JPanel(new GridLayout(1, 2, 8, 0));
                btnPanel.setOpaque(false);
                btnPanel.setBorder(BorderFactory.createEmptyBorder(4, 44, 0, 0)); // align under username
                
                JButton acceptBtn = ghostActionButton("Accept", AppColors.SUCCESS, "Accept Friend Request");
                acceptBtn.addActionListener(e -> {
                    friendApi.acceptRequest(req);
                    if (onFriendAction != null) onFriendAction.accept(req);
                    loadFriendsAndRequests(onlineStatuses);
                });

                JButton rejectBtn = ghostActionButton("Ignore", AppColors.DANGER, "Ignore Request");
                rejectBtn.addActionListener(e -> {
                    friendApi.rejectOrRemoveFriend(req);
                    if (onFriendAction != null) onFriendAction.accept(req);
                    loadFriendsAndRequests(onlineStatuses);
                });

                btnPanel.add(acceptBtn);
                btnPanel.add(rejectBtn);
                
                card.add(btnPanel, BorderLayout.SOUTH);
                itemPanel.add(card);
                listPanel.add(itemPanel);
                listPanel.add(Box.createVerticalStrut(8));
            }
            listPanel.add(Box.createVerticalStrut(10));
        }

        // 2. Online
        List<String> onlineFriends = friends.stream().filter(f -> onlineStatuses != null && onlineStatuses.containsKey(f)).toList();
        listPanel.add(new SidebarCategoryHeader("ONLINE — " + onlineFriends.size()));
        listPanel.add(Box.createVerticalStrut(4));
        for (String f : onlineFriends) {
            String statusName = onlineStatuses.get(f);
            UserListItem item = new UserListItem(f, null, statusName);
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (onFriendSelected != null) onFriendSelected.accept(f);
                }
            });
            friendItems.put(f, item);
            listPanel.add(item);
            listPanel.add(Box.createVerticalStrut(4));
        }
        listPanel.add(Box.createVerticalStrut(10));

        // 3. Offline
        List<String> offlineFriends = friends.stream().filter(f -> onlineStatuses == null || !onlineStatuses.containsKey(f)).toList();
        listPanel.add(new SidebarCategoryHeader("OFFLINE — " + offlineFriends.size()));
        listPanel.add(Box.createVerticalStrut(4));
        for (String f : offlineFriends) {
            UserListItem item = new UserListItem(f, null, "OFFLINE");
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (onFriendSelected != null) onFriendSelected.accept(f);
                }
            });
            friendItems.put(f, item);
            listPanel.add(item);
            listPanel.add(Box.createVerticalStrut(4));
        }

        applyUnread(); // re-apply badge unread đã cache lên item vừa rebuild
        listPanel.revalidate();
        listPanel.repaint();
    }

    /** Nút hành động nhỏ dạng ghost cho lời mời kết bạn. */
    private JButton ghostActionButton(String text, Color highlight, String tooltip) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover() || getModel().isPressed()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(highlight);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        b.setForeground(highlight);
        b.setOpaque(false);
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(gui.theme.AppFonts.BODY_SM);
        b.setBorder(gui.theme.AppBorders.rounded(highlight, 12, 6, 12));
        b.setToolTipText(tooltip);
        
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setForeground(highlight);
            }
        });
        
        return b;
    }

    /** Mở dialog tìm kiếm + thêm bạn (tìm theo username/tên hiển thị, gửi/chấp nhận lời mời). */
    private void showAddFriendDialog() {
        // Nếu dialog đang mở → đưa lên trước, không tạo cửa sổ mới.
        if (addFriendDialog != null && addFriendDialog.isVisible()) {
            addFriendDialog.toFront();
            return;
        }
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        Consumer<String> onAction = targetUser -> {
            if (onFriendAction != null) onFriendAction.accept(targetUser);
        };
        if (addFriendDialog != null) addFriendDialog.dispose(); // gỡ instance cũ đã ẩn
        addFriendDialog = new AddFriendDialog(owner, sessionUsername, onAction);
        addFriendDialog.setVisible(true);
    }
}
