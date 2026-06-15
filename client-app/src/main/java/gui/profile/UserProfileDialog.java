package gui.profile;

import gui.components.AvatarBadge;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.ApiConfig;
import network.UserProfileApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class UserProfileDialog extends JDialog {
    private final String username;
    private final UserProfileApiClient profileClient = new UserProfileApiClient();

    public UserProfileDialog(Frame owner, String username) {
        super(owner, "Hồ sơ người dùng", true);
        this.username = username;

        setSize(320, 420);
        setLocationRelativeTo(owner);
        setResizable(false);
        getContentPane().setBackground(AppColors.BG_PRIMARY);
        setLayout(new BorderLayout());

        // Trạng thái Loading ban đầu
        JPanel loadingPanel = new JPanel(new GridBagLayout());
        loadingPanel.setBackground(AppColors.BG_PRIMARY);
        JLabel loadingLabel = new JLabel("Đang tải thông tin...");
        loadingLabel.setForeground(AppColors.TEXT_MUTED);
        loadingLabel.setFont(AppFonts.BODY);
        loadingPanel.add(loadingLabel);
        add(loadingPanel, BorderLayout.CENTER);

        // Fetch dữ liệu từ user-profile-service bất đồng bộ (trả về Map)
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return profileClient.getProfile(username);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> profile = get();
                    remove(loadingPanel);
                    if (profile != null) {
                        initProfileUI(profile);
                    } else {
                        showError("Không tìm thấy thông tin người dùng.");
                    }
                    revalidate();
                    repaint();
                } catch (Exception e) {
                    remove(loadingPanel);
                    showError("Lỗi kết nối đến dịch vụ hồ sơ.");
                }
            }
        };
        worker.execute();
    }

    private void showError(String message) {
        JPanel errorPanel = new JPanel(new GridBagLayout());
        errorPanel.setBackground(AppColors.BG_PRIMARY);
        JLabel errLabel = new JLabel(message);
        errLabel.setForeground(AppColors.DANGER);
        errLabel.setFont(AppFonts.BODY);
        errorPanel.add(errLabel);
        add(errorPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void initProfileUI(Map<String, Object> profile) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(AppColors.BG_PRIMARY);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Trích xuất dữ liệu từ Map
        String displayName = (String) profile.get("displayName");
        if (displayName == null || displayName.isBlank()) {
            displayName = username; // Fallback nếu không có displayName
        }
        String bio = (String) profile.get("bio");

        // --- XỬ LÝ LẤY AVATAR TỪ API ---
        String avatarUrl = null;
        if (profile.get("avatarUrl") != null) {
            avatarUrl = profile.get("avatarUrl").toString();
        }

        // Khu vực hiển thị Avatar
        JPanel avatarWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        avatarWrap.setOpaque(false);

        String initial = displayName.length() > 0 ? displayName.substring(0, 1).toUpperCase() : "@";

        // Sử dụng AvatarBadge với kích thước to hơn (ví dụ: 80px)
        AvatarBadge avatarBadge = new AvatarBadge(initial, 80);

        // Load ảnh thật nếu có avatarUrl
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            String fullUrl = avatarUrl;
            if (!fullUrl.startsWith("http")) {
                fullUrl = ApiConfig.GATEWAY_HTTP + fullUrl;
            }
            avatarBadge.loadAvatarFromUrl(fullUrl);
        }

        avatarWrap.add(avatarBadge);
        mainPanel.add(avatarWrap);
        mainPanel.add(Box.createVerticalStrut(15));

        // --- TÊN HIỂN THỊ VÀ ĐỔI BIỆT DANH LOCAL ---
        String localNickname = gui.utils.NicknameManager.getNickname(username);
        JLabel nameLabel = new JLabel(localNickname != null ? localNickname : displayName);
        nameLabel.setFont(AppFonts.HEADING_MD);
        nameLabel.setForeground(AppColors.TEXT_NORMAL);

        JButton editNickBtn = new JButton(gui.components.AppIcons.edit(16)); // Đã đổi text thành icon
        editNickBtn.setContentAreaFilled(false);
        editNickBtn.setBorderPainted(false);
        editNickBtn.setFocusPainted(false); // Bỏ viền khi lỡ click vào
        editNickBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editNickBtn.setToolTipText("Đặt biệt danh");
        editNickBtn.setMargin(new Insets(0, 4, 0, 0));

        String finalDisplayName = displayName;
        editNickBtn.addActionListener(e -> {
            String newNick = JOptionPane.showInputDialog(this,
                    "Đặt biệt danh cho " + username + " (Chỉ mình bạn thấy):",
                    gui.utils.NicknameManager.getNickname(username));

            if (newNick != null) {
                gui.utils.NicknameManager.setNickname(username, newNick);
                nameLabel.setText(newNick.isBlank() ? finalDisplayName : newNick);

                Window ownerWindow = getOwner();
                if (ownerWindow instanceof gui.ChatClientGUI) {
                    ((gui.ChatClientGUI) ownerWindow).applyNicknameChange();
                }
            }
        });

        // Gom tên và nút đổi biệt danh vào 1 hàng ngang
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameRow.add(nameLabel);
        nameRow.add(editNickBtn);
        mainPanel.add(nameRow);

        // Username nguyên bản dạng @tag
        JLabel userTagLabel = new JLabel("@" + username);
        userTagLabel.setFont(AppFonts.BODY_SM);
        userTagLabel.setForeground(AppColors.TEXT_MUTED);
        userTagLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(userTagLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Khung thông tin giới thiệu (Bio/Status) - Chế độ Read-Only
        JPanel bioPanel = new JPanel(new BorderLayout());
        bioPanel.setBackground(AppColors.BG_SECONDARY);
        bioPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.SEPARATOR, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel bioTitle = new JLabel("GIỚI THIỆU");
        bioTitle.setFont(AppFonts.CAPTION_BOLD);
        bioTitle.setForeground(AppColors.TEXT_MUTED);
        bioPanel.add(bioTitle, BorderLayout.NORTH);

        JTextArea bioText = new JTextArea(bio != null && !bio.isBlank()
                ? bio : "Người dùng không có lời giới thiệu.");
        bioText.setFont(AppFonts.BODY);
        bioText.setForeground(AppColors.TEXT_NORMAL);
        bioText.setBackground(AppColors.BG_SECONDARY);
        bioText.setEditable(false);
        bioText.setLineWrap(true);
        bioText.setWrapStyleWord(true);
        bioText.setFocusable(false);
        bioText.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        bioPanel.add(bioText, BorderLayout.CENTER);

        mainPanel.add(bioPanel);
        add(mainPanel, BorderLayout.CENTER);

        // --- THANH CÔNG CỤ NÚT BẤM DƯỚI CÙNG ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.setBackground(AppColors.BG_TERTIARY);

        boolean isOwnProfile = false;
        Window ownerWindow = getOwner();
        if (ownerWindow instanceof gui.ChatClientGUI) {
            String myUsername = ((gui.ChatClientGUI) ownerWindow).getSessionUsername();
            isOwnProfile = username.equals(myUsername);
        }

        if (!isOwnProfile) {
            JButton chatBtn = new JButton("Nhắn tin");
            chatBtn.setFont(AppFonts.BODY_BOLD);
            chatBtn.addActionListener(e -> {
                if (ownerWindow instanceof gui.ChatClientGUI) {
                    ((gui.ChatClientGUI) ownerWindow).openDirectMessage(username);
                    dispose();
                }
            });
            bottomPanel.add(chatBtn);
        }

        JButton closeBtn = new JButton("Đóng");
        closeBtn.setFont(AppFonts.BODY_BOLD);
        closeBtn.addActionListener(e -> dispose());

        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}