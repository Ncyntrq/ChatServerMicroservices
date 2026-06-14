package gui.components.chat;

import com.chatsever.common.dto.MessageDTO;
import gui.components.AvatarBadge;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import gui.utils.NicknameManager;
import gui.utils.ProfileLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReactionDetailsDialog extends JDialog {
    private final List<MessageDTO.ReactionDTO> reactions;
    private final JPanel listPanel;
    private final JPanel tabsPanel;
    private String currentActiveTab;

    private final Map<String, List<MessageDTO.ReactionDTO>> groupedReactions = new LinkedHashMap<>();
    private final List<JPanel> tabPanels = new ArrayList<>();

    public ReactionDetailsDialog(Window owner, List<MessageDTO.ReactionDTO> reactions, String initialTab) {
        super(owner, "Cảm xúc", ModalityType.APPLICATION_MODAL);
        this.reactions = reactions;

        setSize(420, 450);
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppColors.BG_PRIMARY);

        // Gom nhóm dữ liệu
        for (MessageDTO.ReactionDTO r : reactions) {
            groupedReactions.computeIfAbsent(r.getEmoji(), k -> new ArrayList<>()).add(r);
        }

        // --- Khu vực Tabs (Left Sidebar - BorderLayout.WEST) ---
        tabsPanel = new JPanel();
        tabsPanel.setLayout(new BoxLayout(tabsPanel, BoxLayout.Y_AXIS));
        tabsPanel.setBackground(AppColors.BG_TERTIARY); // Xám nhạt cho sidebar
        tabsPanel.setPreferredSize(new Dimension(120, 0));
        tabsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppColors.SEPARATOR));

        // Thêm khoảng trống nhỏ ở trên
        tabsPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Thêm tab "Tất cả"
        addTab("Tất cả", reactions.size());

        // Thêm các tab Emoji
        for (Map.Entry<String, List<MessageDTO.ReactionDTO>> entry : groupedReactions.entrySet()) {
            addTab(entry.getKey(), entry.getValue().size());
        }

        add(tabsPanel, BorderLayout.WEST);

        // --- Khu vực Danh sách (BorderLayout.CENTER) ---
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_PRIMARY);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        // Hiển thị tab ban đầu
        if (initialTab == null || (!"Tất cả".equals(initialTab) && !groupedReactions.containsKey(initialTab))) {
            initialTab = "Tất cả";
        }
        selectTab(initialTab);
    }

    private void addTab(String name, int count) {
        JPanel tabItem = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        tabItem.setOpaque(false);
        tabItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tabItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tabItem.putClientProperty("tabName", name);

        String title = "Tất cả".equals(name) ? name + " " + count : name + " " + count;
        JLabel tabLabel = new JLabel(title);
        tabLabel.setFont(AppFonts.BODY_BOLD);
        tabItem.putClientProperty("labelRef", tabLabel); // Lưu tham chiếu label

        // Nạp icon emoji nếu là tab emoji
        if (!"Tất cả".equals(name)) {
            EmojiHelper.iconForCharAsync(name, 18, icon -> {
                if (icon != null) {
                    tabLabel.setText(" " + count);
                    tabLabel.setIcon(icon);
                }
            });
        }

        tabItem.add(tabLabel);

        tabItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectTab(name);
            }
        });

        tabPanels.add(tabItem);
        tabsPanel.add(tabItem);
    }

    private void selectTab(String tabName) {
        this.currentActiveTab = tabName;

        // Cập nhật giao diện Tabs
        for (JPanel panel : tabPanels) {
            String name = (String) panel.getClientProperty("tabName");
            JLabel lbl = (JLabel) panel.getClientProperty("labelRef");

            if (name.equals(tabName)) {
                panel.setOpaque(true);
                panel.setBackground(new Color(88, 101, 242, 30)); // Xanh nhạt / xám nhạt
                panel.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, AppColors.BRAND_PRIMARY));
                lbl.setForeground(AppColors.BRAND_PRIMARY);
            } else {
                panel.setOpaque(false);
                panel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0)); // Giữ 4px lề trái để chữ không bị giật
                lbl.setForeground(AppColors.TEXT_MUTED);
            }
        }

        // Cập nhật danh sách (Filter)
        listPanel.removeAll();

        List<MessageDTO.ReactionDTO> filteredList = new ArrayList<>();
        if ("Tất cả".equals(tabName)) {
            filteredList.addAll(reactions);
        } else {
            List<MessageDTO.ReactionDTO> emojis = groupedReactions.get(tabName);
            if (emojis != null) {
                filteredList.addAll(emojis);
            }
        }

        for (MessageDTO.ReactionDTO r : filteredList) {
            listPanel.add(new ReactionRow(r));
        }

        // Thêm một panel rỗng để lấy không gian cuối cùng, đẩy các items lên trên
        listPanel.add(Box.createVerticalGlue());

        listPanel.revalidate();
        listPanel.repaint();
    }

    // --- Inner class cho mỗi dòng hiển thị ---
    private class ReactionRow extends JPanel {
        public ReactionRow(MessageDTO.ReactionDTO r) {
            setLayout(new BorderLayout(12, 0));
            setBackground(AppColors.BG_PRIMARY);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 24));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setPreferredSize(new Dimension(300, 50));

            String username = r.getUserId();
            String initial = username != null && !username.isEmpty() ? username.substring(0, 1).toUpperCase() : "?";

            // Left: Avatar + Username
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            leftPanel.setOpaque(false);

            AvatarBadge avatar = new AvatarBadge(initial, 36);
            leftPanel.add(avatar);

            JLabel nameLabel = new JLabel(username);
            nameLabel.setFont(AppFonts.BODY_BOLD);
            nameLabel.setForeground(AppColors.TEXT_NORMAL);
            leftPanel.add(nameLabel);

            add(leftPanel, BorderLayout.WEST);

            // --- Right Panel: Emoji Icon + Số lượng ---
            JLabel emojiLabel = new JLabel("1");
            emojiLabel.setFont(AppFonts.BODY); // hoặc AppFonts.TINY tùy thuộc bộ Font
            emojiLabel.setForeground(AppColors.TEXT_MUTED);
            emojiLabel.setIconTextGap(8);
            emojiLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
            emojiLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // Padding cha đã lo
            
            EmojiHelper.iconForCharAsync(r.getEmoji(), 24, icon -> emojiLabel.setIcon(icon));

            add(emojiLabel, BorderLayout.EAST);

            // Load thông tin avatar và nickname bất đồng bộ
            ProfileLoader.load(username, profile -> {
                if (profile == null)
                    return;

                // Tên hiển thị (Nickname > DisplayName > Username)
                String nick = NicknameManager.getNickname(username);
                if (nick != null) {
                    nameLabel.setText(nick);
                } else {
                    Object dn = profile.get("displayName");
                    if (dn != null && !dn.toString().isBlank()) {
                        nameLabel.setText(dn.toString());
                    }
                }

                // Avatar URL
                Object av = profile.get("avatarUrl");
                if (av != null) {
                    String url = av.toString();
                    if (!url.startsWith("http")) {
                        url = network.ApiConfig.GATEWAY_HTTP + url;
                    }
                    avatar.loadAvatarFromUrl(url);
                }
            });
        }
    }
}
