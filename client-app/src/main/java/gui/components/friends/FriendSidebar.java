package gui.components.friends;

import gui.components.channels.UserFooterPanel;
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
    private Consumer<String> onFriendAction;

    public void setOnFriendSelected(Consumer<String> onFriendSelected) {
        this.onFriendSelected = onFriendSelected;
    }
    
    public void setOnFriendAction(Consumer<String> onFriendAction) {
        this.onFriendAction = onFriendAction;
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

        titleLabel = new JLabel("Bạn bè");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(AppColors.TEXT_WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        IconButton addFriendBtn = new IconButton("➕", e -> showAddFriendDialog());
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

        // --- FOOTER ---
        UserFooterPanel accountFooter = new UserFooterPanel(sessionUsername);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(accountFooter, BorderLayout.SOUTH);
    }

    public void loadFriendsAndRequests(List<String> onlineUsers) {
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
                    renderLists(friends, pending, onlineUsers);
                } catch (Exception ex) {
                    listPanel.removeAll();
                    JLabel err = new JLabel("Không tải được dữ liệu bạn bè");
                    err.setForeground(AppColors.TEXT_MUTED);
                    listPanel.add(err);
                    listPanel.revalidate();
                    listPanel.repaint();
                }
            }
        }.execute();
    }

    private void renderLists(List<String> friends, List<String> pending, List<String> onlineUsers) {
        listPanel.removeAll();

        // 1. Lời mời kết bạn
        if (pending != null && !pending.isEmpty()) {
            listPanel.add(new SidebarCategoryHeader("ĐANG CHỜ XÁC NHẬN — " + pending.size()));
            listPanel.add(Box.createVerticalStrut(4));
            for (String req : pending) {
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                UserListItem item = new UserListItem(req, "Chờ xác nhận", AppColors.STATUS_OFFLINE, true);
                row.add(item, BorderLayout.CENTER);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
                btnPanel.setOpaque(false);
                
                JButton acceptBtn = new JButton("✓");
                acceptBtn.setMargin(new Insets(2, 4, 2, 4));
                acceptBtn.addActionListener(e -> {
                    friendApi.acceptRequest(req);
                    if (onFriendAction != null) onFriendAction.accept(req);
                    loadFriendsAndRequests(onlineUsers);
                });
                
                JButton rejectBtn = new JButton("✗");
                rejectBtn.setMargin(new Insets(2, 4, 2, 4));
                rejectBtn.addActionListener(e -> {
                    friendApi.rejectOrRemoveFriend(req);
                    if (onFriendAction != null) onFriendAction.accept(req);
                    loadFriendsAndRequests(onlineUsers);
                });

                btnPanel.add(acceptBtn);
                btnPanel.add(rejectBtn);
                row.add(btnPanel, BorderLayout.EAST);
                
                listPanel.add(row);
                listPanel.add(Box.createVerticalStrut(4));
            }
            listPanel.add(Box.createVerticalStrut(10));
        }

        // 2. Trực tuyến
        List<String> onlineFriends = friends.stream().filter(onlineUsers::contains).toList();
        listPanel.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + onlineFriends.size()));
        listPanel.add(Box.createVerticalStrut(4));
        for (String f : onlineFriends) {
            UserListItem item = new UserListItem(f, null, AppColors.STATUS_ONLINE, true);
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (onFriendSelected != null) onFriendSelected.accept(f);
                }
            });
            listPanel.add(item);
            listPanel.add(Box.createVerticalStrut(4));
        }
        listPanel.add(Box.createVerticalStrut(10));

        // 3. Ngoại tuyến
        List<String> offlineFriends = friends.stream().filter(f -> !onlineUsers.contains(f)).toList();
        listPanel.add(new SidebarCategoryHeader("NGOẠI TUYẾN — " + offlineFriends.size()));
        listPanel.add(Box.createVerticalStrut(4));
        for (String f : offlineFriends) {
            UserListItem item = new UserListItem(f, null, AppColors.STATUS_OFFLINE, false);
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (onFriendSelected != null) onFriendSelected.accept(f);
                }
            });
            listPanel.add(item);
            listPanel.add(Box.createVerticalStrut(4));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void showAddFriendDialog() {
        String username = JOptionPane.showInputDialog(this, "Nhập username kết bạn:", "Thêm bạn", JOptionPane.PLAIN_MESSAGE);
        if (username != null && !username.trim().isEmpty()) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    friendApi.sendRequest(username.trim());
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(FriendSidebar.this, "Đã gửi lời mời đến " + username);
                        if (onFriendAction != null) onFriendAction.accept(username.trim());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(FriendSidebar.this, "Lỗi: " + ex.getMessage());
                    }
                }
            }.execute();
        }
    }
}
