package gui.components.chat;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import gui.components.AvatarBadge;
import gui.components.AppIcons;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.FileApiClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

public class ChatMessageItem extends JPanel {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Callback cho các thao tác trên tin nhắn. ChatClientGUI cung cấp implementation. */
    public interface MessageActions {
        void onEdit(MessageDTO message, String newContent);
        void onDelete(MessageDTO message);
        void onPin(MessageDTO message);
    }

    private final MessageDTO message;
    private final String currentUser;
    private final MessageActions actions;
    private final boolean isOwn;

    private boolean isHovered = false;
    private boolean menuOpen = false; // menu "⋯" đang mở → giữ toolbar hiển thị
    private boolean isEditing = false;
    private boolean isAttachment = false;
    private final boolean isHighlighted;
    private final boolean isSystemMsg;

    // Components dùng cho inline-edit & cập nhật động
    private JPanel contentPanel;
    private JPanel centerWrap; // bọc content + toolbar (căn giữa dọc, toolbar nằm cạnh tin)
    private JPanel headerRow;
    private JTextPane messageBody;
    private JPanel toolbar;
    private boolean editedBadgeShown = false;

    /** Layout gọn cho tin nhắn liên tiếp cùng người gửi (gộp nhóm). */
    private final boolean isConsecutive;

    /** Constructor cũ — giữ tương thích (không có toolbar). */
    public ChatMessageItem(MessageDTO message, boolean isHighlighted) {
        this(message, isHighlighted, null, null, false);
    }

    /** Constructor với hành động (sửa/xóa/ghim). */
    public ChatMessageItem(MessageDTO message, boolean isHighlighted,
                           String currentUser, MessageActions actions) {
        this(message, isHighlighted, currentUser, actions, false);
    }

    /** Constructor với layout gọn cho tin liên tiếp. */
    public ChatMessageItem(MessageDTO message, boolean isHighlighted, boolean isConsecutive) {
        this(message, isHighlighted, null, null, isConsecutive);
    }

    public ChatMessageItem(MessageDTO message, boolean isHighlighted,
                           String currentUser, MessageActions actions, boolean isConsecutive) {
        this.message = message;
        this.currentUser = currentUser;
        this.actions = actions;
        this.isHighlighted = isHighlighted;
        this.isConsecutive = isConsecutive;
        this.isOwn = currentUser != null && currentUser.equals(message.getSender());
        this.isSystemMsg = message.getType() == MessageType.SYSTEM
                || message.getType() == MessageType.JOIN
                || message.getType() == MessageType.LEAVE
                || message.getType() == MessageType.ERROR
                || "SYSTEM".equals(message.getSender());

        setLayout(new BorderLayout(12, 0));
        setOpaque(false);

        // Padding (margin dọc giữa các tin nhỏ lại → các box sát nhau hơn)
        if (isHighlighted) {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, AppColors.MSG_HIGHLIGHT_BORDER),
                    BorderFactory.createEmptyBorder(isConsecutive ? 1 : 4, 16, isConsecutive ? 1 : 4, 20)));
        } else {
            setBorder(BorderFactory.createEmptyBorder(isConsecutive ? 1 : 4, 20, isConsecutive ? 1 : 4, 20));
        }

        if (isSystemMsg) {
            buildSystemLayout(message);
        } else {
            // Tin gộp nhóm (consecutive) dùng CHUNG layout, chỉ ẩn avatar + header
            // ⇒ vẫn đủ tính năng: attachment, wrap, và toolbar Sửa/Ghim/Xóa.
            buildChatLayout(message, isConsecutive);
            // Panel hover: giờ (cho tin compact) + nút "⋯" (nếu có quyền). Tự bỏ qua nếu rỗng.
            buildHoverBar();
        }
        // Gắn listener lên CẢ hàng để mọi lần con trỏ đổi vùng đều có sự kiện; việc hiện/ẩn
        // do updateHover quyết định dựa trên con trỏ có nằm trong bubble/toolbar hay không.
        installHover(this);
    }

    private void buildSystemLayout(MessageDTO message) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        row.setOpaque(false);

        // Dash line left
        JLabel dash = new JLabel("—");
        dash.setForeground(AppColors.TEXT_MUTED);
        dash.setFont(AppFonts.CAPTION);

        // System icon
        String icon = "SYSTEM".equals(message.getSender()) ? "ℹ" : "→";
        if (message.getType() == MessageType.ERROR)
            icon = "⚠";
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setForeground(message.getType() == MessageType.ERROR
                ? AppColors.WARNING
                : AppColors.TEXT_MUTED);
        iconLabel.setFont(AppFonts.BODY_SM);

        // Content — giới hạn bề rộng để tin hệ thống/lỗi dài tự wrap, không kéo giãn hàng.
        JTextPane content = new JTextPane() {
            @Override public Dimension getPreferredSize() {
                setSize(maxBubbleWidth(), Short.MAX_VALUE);
                return super.getPreferredSize();
            }
            @Override public Dimension getMaximumSize() {
                return new Dimension(maxBubbleWidth(), getPreferredSize().height);
            }
        };
        content.setEditorKit(new WrapEditorKit()); // bẻ được từ dài
        content.setEditable(false);
        makeNonInteractive(content); // chặn focus/bôi đen/con trỏ soạn thảo
        content.setOpaque(false);
        content.setFont(AppFonts.BODY_SM);
        content.setForeground(AppColors.TEXT_MUTED);
        EmojiHelper.renderTextWithEmojis(content, message.getContent());

        // Time
        String time = message.getTimestamp() != null
                ? message.getTimestamp().format(TIME_FMT)
                : "";
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(AppFonts.TINY);
        timeLabel.setForeground(new Color(0x80, 0x84, 0x8E, 0x80));

        row.add(dash);
        row.add(iconLabel);
        row.add(content);
        row.add(timeLabel);
        JLabel dashRight = new JLabel("—");
        dashRight.setForeground(AppColors.TEXT_MUTED);
        dashRight.setFont(AppFonts.CAPTION);
        row.add(dashRight);

        add(row, BorderLayout.CENTER);
    }

    /** Thân tin dạng text: Xử lý hiển thị Tag @ (Màu xanh) và bẻ wrap từ dài */
    private JTextPane createTextBody(String content, int topPadding) {
        final String raw = content == null ? "" : content;
        JTextPane pane = new JTextPane() {
            @Override public Dimension getPreferredSize() {
                int cap = maxBubbleWidth();
                FontMetrics fm = getFontMetrics(getFont());
                int longest = 0;
                for (String line : raw.split("\n", -1)) longest = Math.max(longest, fm.stringWidth(line));
                int w = Math.max(24, Math.min(cap, longest + 6));
                setSize(w, Short.MAX_VALUE);
                return new Dimension(w, super.getPreferredSize().height);
            }
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        pane.setEditorKit(new WrapEditorKit());
        pane.setEditable(false);
        makeNonInteractive(pane); // chặn focus/bôi đen/con trỏ soạn thảo
        pane.setOpaque(false);
        pane.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.setBorder(BorderFactory.createEmptyBorder(topPadding, 0, 0, 0));
        EmojiHelper.renderTextWithEmojis(pane, raw);

        try {
            javax.swing.text.StyledDocument doc = pane.getStyledDocument();
            String docText = doc.getText(0, doc.getLength());

            javax.swing.text.SimpleAttributeSet mentionStyle = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setForeground(mentionStyle, new Color(88, 101, 242));
            javax.swing.text.StyleConstants.setBold(mentionStyle, true);

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@[\\p{L}\\p{N}_]+");
            java.util.regex.Matcher matcher = pattern.matcher(docText);

            while (matcher.find()) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), mentionStyle, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }

    /**
     * Biến JTextPane chỉ-đọc thành "không tương tác": không nhận focus, không bôi đen,
     * không hiện con trỏ soạn thảo. Dùng cho thân tin nhắn (chỉ hiển thị + render emoji).
     */
    private static void makeNonInteractive(JTextPane pane) {
        pane.setFocusable(false);
        pane.setHighlighter(null);            // bỏ chọn/bôi đen text
        pane.getCaret().setVisible(false);    // ẩn con trỏ nhấp nháy
        pane.setCursor(Cursor.getDefaultCursor()); // không hiện con trỏ chữ "I"
    }

    /**
     * Dựng layout tin nhắn. {@code compact==true} (tin gộp nhóm cùng người gửi) ẩn avatar + header,
     * nhưng vẫn dùng CHUNG phần thân + được gắn toolbar ⇒ đủ tính năng như tin đầu nhóm.
     */
    private void buildChatLayout(MessageDTO message, boolean compact) {
        String senderName = message.getSender();

        // WEST: avatar đầy đủ, hoặc spacer canh lề khi gộp nhóm
        JComponent west;
        AvatarBadge avatar = null;
        int col = gui.theme.UiScale.get().scaled(40); // cột avatar scale theo zoom (#3)
        if (compact) {
            JPanel spacer = new JPanel();
            spacer.setOpaque(false);
            spacer.setPreferredSize(new Dimension(col, 10));
            spacer.setMinimumSize(new Dimension(col, 1));
            spacer.setMaximumSize(new Dimension(col, Short.MAX_VALUE));
            west = spacer;
        } else {
            String initial = senderName != null && !senderName.isEmpty()
                    ? senderName.substring(0, 1).toUpperCase() : "?";
            avatar = new AvatarBadge(initial, col);
            JPanel avatarWrapper = new JPanel(new BorderLayout());
            avatarWrapper.setOpaque(false);
            avatarWrapper.add(avatar, BorderLayout.NORTH);
            west = avatarWrapper;
        }

        // Content panel
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        if (!compact) {
            // Header row: username + badges + timestamp (chỉ ở tin đầu nhóm)
            headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            headerRow.setOpaque(false);
            headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel senderLabel = new JLabel(senderName);
            senderLabel.setFont(AppFonts.BODY_BOLD);
            senderLabel.setForeground(AppColors.avatarColorFor(senderName));
            headerRow.add(senderLabel);

            // #1: ProfileLoader (cache + gộp request trùng) → tối đa 1 HTTP/user thay vì 1/tin nhắn
            final AvatarBadge avatarRef = avatar;
            gui.utils.ProfileLoader.load(senderName, profile -> {
                if (profile == null) return;
                Object dn = profile.get("displayName");
                if (dn != null && !dn.toString().isBlank()) senderLabel.setText(dn.toString());
                Object av = profile.get("avatarUrl");
                if (av != null && avatarRef != null) {
                    String url = av.toString();
                    if (!url.startsWith("http")) url = network.ApiConfig.GATEWAY_HTTP + url;
                    avatarRef.loadAvatarFromUrl(url);
                }
            });

            // Admin badge
            if ("admin".equalsIgnoreCase(message.getSender())) {
                JLabel badge = new JLabel(" ADMIN ") {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(AppColors.BRAND_PRIMARY);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                badge.setOpaque(false);
                badge.setForeground(Color.WHITE);
                badge.setFont(AppFonts.TINY);
                badge.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
                headerRow.add(badge);
            }

            // Timestamp
            String timeStr = message.getTimestamp() != null
                    ? message.getTimestamp().format(TIME_FMT) : "Bây giờ";
            JLabel timeLabel = new JLabel(timeStr);
            timeLabel.setFont(AppFonts.CAPTION);
            timeLabel.setForeground(new Color(0x80, 0x84, 0x8E, 0x99));
            headerRow.add(timeLabel);

            if (Boolean.TRUE.equals(message.getIsEdited())) {
                addEditedBadge();
            }

            contentPanel.add(headerRow);
        }

        // Body chung: đính kèm (ảnh/file) hay text (emoji + wrap)
        Attachment att = parseAttachment(message.getContent());
        this.isAttachment = att != null;
        if (att != null) {
            contentPanel.add(att.isImage() ? buildImageAttachment(att) : buildFileCard(att));
        } else {
            messageBody = createTextBody(message.getContent(), compact ? 0 : 3);
            contentPanel.add(messageBody);
        }

        // Bọc content trong GridBag: content ôm sát (gridx0) + glue đẩy về trái (gridx2).
        // Toolbar sẽ chèn vào gridx1 (cạnh tin) trong buildToolbar. weighty=1 ⇒ căn giữa dọc.
        centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);
        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.anchor = GridBagConstraints.WEST; cc.weighty = 1; cc.fill = GridBagConstraints.NONE;
        centerWrap.add(contentPanel, cc);

        GridBagConstraints gl = new GridBagConstraints();
        gl.gridx = 2; gl.weightx = 1; gl.fill = GridBagConstraints.HORIZONTAL;
        centerWrap.add(Box.createHorizontalGlue(), gl);

        add(west, BorderLayout.WEST);
        add(centerWrap, BorderLayout.CENTER);


    }

    private void addEditedBadge() {
        if (editedBadgeShown || headerRow == null) return; // tin gộp nhóm không có header
        JLabel editedBadge = new JLabel("(đã sửa)");
        editedBadge.setFont(AppFonts.TINY);
        editedBadge.setForeground(AppColors.TEXT_MUTED);
        headerRow.add(editedBadge);
        editedBadgeShown = true;
        headerRow.revalidate();
        headerRow.repaint();
    }

    // ---------------------------------------------------------------
    // Panel hover: [giờ (tin compact)] + nút "⋯" (bấm mở menu Sửa/Ghim/Xóa)
    // ---------------------------------------------------------------
    private void buildHoverBar() {
        boolean canManage = network.PermissionCache.get().can(network.PermissionCache.MANAGE_MESSAGES);
        boolean canEdit = isOwn && !isAttachment;
        boolean canPin = canManage;
        boolean canDelete = isOwn || canManage;
        boolean hasActions = canEdit || canPin || canDelete;

        // Tin compact (ẩn giờ ở header) sẽ hiện giờ nhỏ khi hover. Tin đầu nhóm đã có giờ ở header.
        boolean showHoverTime = isConsecutive && message.getTimestamp() != null;
        if (!hasActions && !showHoverTime) return; // không có gì để hiện → bỏ qua

        toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                java.awt.Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                // Frosted khi bật wallpaper; ngược lại nền đặc BG_FLOATING như cũ.
                if (gui.theme.WallpaperManager.get().isEnabled()) {
                    gui.theme.WallpaperRenderer.paintFrosted(g2, this, shape, AppColors.MSG_BUBBLE);
                } else {
                    g2.setColor(AppColors.BG_FLOATING);
                    g2.fill(shape);
                }
                g2.setColor(AppColors.BG_TERTIARY);
                g2.draw(shape);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        toolbar.setOpaque(false);

        // Giờ nhỏ (chỉ tin compact) — đặt bên trái nút "⋯".
        if (showHoverTime) {
            JLabel timeLabel = new JLabel(message.getTimestamp().format(TIME_FMT));
            timeLabel.setFont(AppFonts.TINY);
            timeLabel.setForeground(AppColors.TEXT_MUTED);
            toolbar.add(timeLabel);
        }

        if (hasActions) {
            IconButton moreBtn = new IconButton(AppIcons.ellipsis(14), null);
            moreBtn.setToolTipText("Tùy chọn");
            moreBtn.addActionListener(e -> showActionMenu(moreBtn, canEdit, canPin, canDelete));
            toolbar.add(moreBtn);
        }

        toolbar.setVisible(false);
        // Đặt toolbar NGAY CẠNH tin nhắn (gridx1). Khi ẩn, GridBag bỏ qua ⇒ không chiếm chỗ.
        if (centerWrap != null) {
            GridBagConstraints tc = new GridBagConstraints();
            tc.gridx = 1; tc.weighty = 1; tc.fill = GridBagConstraints.NONE;
            tc.anchor = GridBagConstraints.WEST;
            tc.insets = new Insets(0, 8, 0, 0);
            centerWrap.add(toolbar, tc);
        }
    }

    /** Mở menu hành động (Sửa/Ghim/Xóa) ngay dưới nút "⋯". */
    private void showActionMenu(JComponent anchor, boolean canEdit, boolean canPin, boolean canDelete) {
        JPopupMenu menu = new JPopupMenu();
        if (canEdit) menu.add(actionItem("  Sửa tin nhắn", this::startEditing));
        if (canPin) menu.add(actionItem("  Ghim tin nhắn", () -> { if (actions != null) actions.onPin(message); }));
        if (canDelete) menu.add(actionItem("  Xóa tin nhắn", this::confirmDelete));

        menuOpen = true; // giữ toolbar hiện trong lúc menu mở (chuột rời item không ẩn nút)
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                menuOpen = false;
                if (!isHovered && toolbar != null) toolbar.setVisible(false);
                repaint();
            }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
        menu.show(anchor, 0, anchor.getHeight());
    }

    private JMenuItem actionItem(String text, Runnable onClick) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(AppFonts.BODY);
        item.addActionListener(e -> onClick.run());
        return item;
    }

    private void confirmDelete() {
        int ok = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                "Xóa tin nhắn này?", "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION && actions != null) {
            actions.onDelete(message);
        }
    }

    // ---------------------------------------------------------------
    // Inline edit
    // ---------------------------------------------------------------
    /** Bắt đầu chỉnh sửa: thay messageBody bằng JTextField viền xanh. */
    public void startEditing() {
        if (isEditing || isAttachment || messageBody == null || !isOwn) return;
        isEditing = true;
        if (toolbar != null) toolbar.setVisible(false);

        JTextField editField = new JTextField(message.getContent());
        editField.setFont(AppFonts.BODY);
        editField.setForeground(AppColors.TEXT_NORMAL);
        editField.setBackground(AppColors.BG_TERTIARY);
        editField.setCaretColor(AppColors.TEXT_WHITE);
        editField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BRAND_PRIMARY, 2),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        editField.setAlignmentX(Component.LEFT_ALIGNMENT);
        editField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        editField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String newText = editField.getText().trim();
                    finishEditing();
                    if (!newText.isEmpty() && !newText.equals(message.getContent()) && actions != null) {
                        actions.onEdit(message, newText);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    finishEditing();
                }
            }
        });

        int idx = indexInContent(messageBody);
        contentPanel.remove(messageBody);
        contentPanel.add(editField, idx);
        contentPanel.revalidate();
        contentPanel.repaint();
        editField.requestFocusInWindow();
        editField.selectAll();
    }

    private void finishEditing() {
        if (!isEditing) return;
        isEditing = false;
        // Dựng lại layout content: bỏ mọi thứ sau headerRow, gắn lại messageBody
        rebuildContentBody();
    }

    private void rebuildContentBody() {
        // Xóa tất cả ngoại trừ headerRow rồi thêm lại messageBody
        for (Component c : contentPanel.getComponents()) {
            if (c != headerRow) contentPanel.remove(c);
        }
        contentPanel.add(messageBody);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private int indexInContent(Component target) {
        Component[] comps = contentPanel.getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] == target) return i;
        }
        return contentPanel.getComponentCount();
    }

    /** Cập nhật nội dung khi nhận broadcast EDIT từ server. */
    public void updateContent(String newContent, boolean edited) {
        if (messageBody != null) {
            messageBody.setText(newContent);
            try {
                javax.swing.text.SimpleAttributeSet normalStyle = new javax.swing.text.SimpleAttributeSet();
                javax.swing.text.StyleConstants.setForeground(normalStyle, AppColors.TEXT_NORMAL);

                javax.swing.text.SimpleAttributeSet mentionStyle = new javax.swing.text.SimpleAttributeSet();
                javax.swing.text.StyleConstants.setForeground(mentionStyle, new Color(88, 101, 242));
                javax.swing.text.StyleConstants.setBold(mentionStyle, true);

                javax.swing.text.StyledDocument doc = messageBody.getStyledDocument();
                doc.setCharacterAttributes(0, doc.getLength(), normalStyle, true);

                // FIX LỖI OFFSET TƯƠNG TỰ BÊN TRÊN
                String docText = doc.getText(0, doc.getLength());
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@[\\p{L}\\p{N}_.]+");
                java.util.regex.Matcher matcher = pattern.matcher(docText);

                while (matcher.find()) {
                    doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), mentionStyle, false);
                }
            } catch (Exception e) {}

            messageBody.revalidate();
        }
        message.setContent(newContent);
        if (edited) {
            message.setIsEdited(true);
            addEditedBadge();
        }
    }

    public Long getMessageId() {
        return message.getMessageId();
    }

    public MessageDTO getMessage() {
        return message;
    }

    // ---------------------------------------------------------------
    // Hover: hiện/ẩn toolbar + đổi nền (gắn đệ quy lên toàn bộ con)
    // ---------------------------------------------------------------
    private void installHover(Component c) {
        c.addMouseListener(hoverAdapter);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                installHover(child);
            }
        }
    }

    private final MouseAdapter hoverAdapter = new MouseAdapter() {
        @Override public void mouseEntered(MouseEvent e) { updateHover(e); }
        @Override public void mouseExited(MouseEvent e) { updateHover(e); }
    };

    /** Hiện toolbar CHỈ khi con trỏ nằm trong bubble (hoặc toolbar đang mở), ngược lại ẩn. */
    private void updateHover(MouseEvent e) {
        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), this);
        Rectangle zone = hotZoneBounds();
        setHover(zone != null && zone.contains(p));
    }

    /** Vùng "nóng" kích hoạt hover = bubble nội dung (+ toolbar khi đang hiện). Gộp để không nhấp nháy ở khe hở. */
    private Rectangle hotZoneBounds() {
        if (contentPanel == null) return null;
        Rectangle zone = SwingUtilities.convertRectangle(
                contentPanel.getParent(), contentPanel.getBounds(), this);
        if (toolbar != null && toolbar.isVisible()) {
            zone = zone.union(SwingUtilities.convertRectangle(
                    toolbar.getParent(), toolbar.getBounds(), this));
        }
        return zone;
    }

    private void setHover(boolean hovered) {
        if (isHovered == hovered) return;
        isHovered = hovered;
        // Không ẩn toolbar khi menu "⋯" đang mở (để popup không bị đóng theo invoker).
        if (toolbar != null && !isEditing && !(menuOpen && !hovered)) {
            toolbar.setVisible(hovered);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Box "frosted" ôm sát nội dung khi bật wallpaper → chữ dễ đọc, không bị hoạ tiết gây rối.
        boolean wallpaper = !isSystemMsg && gui.theme.WallpaperManager.get().isEnabled();
        Rectangle box = wallpaper ? contentBubbleRect() : null;
        int arc = 16; // bo góc box tin nhắn
        if (box != null) {
            RoundRectangle2D shape = new RoundRectangle2D.Float(box.x, box.y, box.width, box.height, arc, arc);
            gui.theme.WallpaperRenderer.paintFrosted(g2, this, shape, AppColors.MSG_BUBBLE);
            // Viền bo góc mảnh cho box rõ nét
            g2.setColor(AppColors.SEPARATOR);
            g2.draw(shape);
        }

        // Hover/highlight: ôm content khi có box, ngược lại trải full-width như cũ.
        int hx = box != null ? box.x : 6;
        int hy = box != null ? box.y : 1;
        int hw = box != null ? box.width : getWidth() - 12;
        int hh = box != null ? box.height : getHeight() - 2;
        if (isHighlighted) {
            g2.setColor(AppColors.MSG_HIGHLIGHT_BG);
            g2.fillRoundRect(hx, hy, hw, hh, arc, arc);
        } else if ((isHovered || isEditing) && !isSystemMsg) {
            g2.setColor(AppColors.BG_MESSAGE_HOVER);
            g2.fillRoundRect(hx, hy, hw, hh, arc, arc);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    /** Chiều cao tối thiểu của box (tương xứng với toolbar Sửa/Ghim/Xoá). */
    private static final int MIN_BUBBLE_H = 36;

    /**
     * Box ôm sát BỀ RỘNG nội dung, CHIỀU CAO bám theo hàng (đã có chiều cao tối thiểu ổn định)
     * ⇒ box hiển thị đầy đủ ngay từ đầu, không bị cắt, không "nhảy" khi hover.
     */
    private Rectangle contentBubbleRect() {
        if (contentPanel == null || centerWrap == null) return null;
        int cw = contentPanel.getWidth();
        if (cw <= 0) return null;
        int padX = 12, vInset = 2;
        int x = centerWrap.getX() + contentPanel.getX() - padX;
        int h = Math.max(getHeight() - 2 * vInset, MIN_BUBBLE_H);
        return new Rectangle(x, vInset, cw + 2 * padX, h);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // Bảo đảm hàng đủ cao để box hiển thị ĐẦY ĐỦ ngay từ đầu (không bị cắt) và toolbar
        // hiện khi hover vẫn nằm gọn trong hàng ⇒ không "nhảy" kích thước lúc hover.
        if (!isSystemMsg) d.height = Math.max(d.height, MIN_BUBBLE_H + 4);
        return d;
    }

    @Override
    public Dimension getMaximumSize() {
        // Item phủ full chiều rộng (để nền hover trải đều); chỉ THÂN tin bị giới hạn (xem maxBubbleWidth).
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /**
     * Bề rộng tối đa của THÂN tin nhắn ("giới hạn 2 biên"): bám theo viewport nhưng có trần.
     * - Màn rộng → cap ở {@code scaled(720)} ⇒ luôn chừa lề phải, không chạm 2 biên.
     * - Màn hẹp → dùng đúng bề rộng khả dụng ⇒ vẫn wrap, không tràn/cắt chữ.
     */
    int maxBubbleWidth() {
        int cap = gui.theme.UiScale.get().scaled(720);
        java.awt.Container vp = SwingUtilities.getAncestorOfClass(JViewport.class, this);
        int reserve = gui.theme.UiScale.get().scaled(110); // avatar + khoảng cách + padding 2 bên
        int avail = (vp != null ? vp.getWidth() : 600) - reserve;
        if (avail <= 0) avail = 600; // fallback khi chưa gắn vào viewport
        return Math.max(160, Math.min(avail, cap));
    }

    // ---------------------------------------------------------------
    // File đính kèm & preview ảnh (feature #9)
    // ---------------------------------------------------------------

    /** Marker mã hóa attachment trong nội dung tin nhắn (text-only nên backend lưu/broadcast bình thường). */
    public static final String ATTACH_PREFIX = "\u0001ATTACH\u0001";
    private static final String DELIM = "\u0001";

    /** Mã hóa metadata file vào content để gửi qua WebSocket. */
    public static String encodeAttachment(String contentType, long size,
                                          String thumbUrl, String url, String name) {
        return ATTACH_PREFIX
                + nz(contentType) + DELIM
                + size + DELIM
                + nz(thumbUrl) + DELIM
                + nz(url) + DELIM
                + nz(name);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    static Attachment parseAttachment(String content) {
        if (content == null || !content.startsWith(ATTACH_PREFIX)) return null;
        String rest = content.substring(ATTACH_PREFIX.length());
        String[] p = rest.split(DELIM, 5);
        if (p.length < 5) return null;
        Attachment a = new Attachment();
        a.contentType = p[0];
        try { a.size = Long.parseLong(p[1]); } catch (Exception ignore) { a.size = 0; }
        a.thumbnailUrl = p[2].isEmpty() ? null : p[2];
        a.url = p[3].isEmpty() ? null : p[3];
        a.name = p[4];
        return a;
    }

    static class Attachment {
        String contentType, thumbnailUrl, url, name;
        long size;
        boolean isImage() { return contentType != null && contentType.startsWith("image/"); }
    }

    private JComponent buildImageAttachment(Attachment att) {
        JLabel img = new JLabel("Đang tải ảnh…");
        img.setForeground(AppColors.TEXT_MUTED);
        img.setFont(AppFonts.BODY_SM);
        img.setAlignmentX(Component.LEFT_ALIGNMENT);
        img.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        img.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        img.setToolTipText("Bấm để xem ảnh gốc");

        final String loadUrl = att.thumbnailUrl != null ? att.thumbnailUrl : att.url;
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() {
                return loadScaledIcon(loadUrl, 320, 240);
            }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) { img.setText(null); img.setIcon(icon); }
                    else img.setText("[Không tải được ảnh]");
                } catch (Exception e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    img.setText("[Không tải được ảnh]");
                }
                img.revalidate();
                img.repaint();
            }
        }.execute();

        img.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openFullImage(att); }
        });
        return img;
    }

    private JComponent buildFileCard(Attachment att) {
        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppColors.BG_FLOATING);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(340, 68));

        // Icon file: thay emoji (dễ bị ô vuông) bằng badge màu + text viết tắt
        JLabel icon = buildFileIconLabel(att.name);
        card.add(icon, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        JLabel name = new JLabel(att.name != null ? att.name : "tệp tin");
        name.setFont(AppFonts.BODY_BOLD);
        name.setForeground(AppColors.TEXT_LINK);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel size = new JLabel(humanSize(att.size));
        size.setFont(AppFonts.TINY);
        size.setForeground(AppColors.TEXT_MUTED);
        size.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(name);
        center.add(size);
        card.add(center, BorderLayout.CENTER);

        IconButton download = new IconButton(AppIcons.download(16), e -> downloadFile(att));
        download.setToolTipText("Tải xuống");
        JPanel dlWrap = new JPanel(new BorderLayout());
        dlWrap.setOpaque(false);
        dlWrap.add(download, BorderLayout.NORTH);
        card.add(dlWrap, BorderLayout.EAST);

        return card;
    }

    private ImageIcon loadScaledIcon(String fullUrl, int maxW, int maxH) {
        if (fullUrl == null) return null;
        try {
            byte[] bytes = new FileApiClient().download(fullUrl); // JWT qua header, chặn host lạ
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
            if (src == null) return null;
            // Scale bicubic chất lượng cao (thay SCALE_SMOOTH/bilinear) ⇒ ảnh phóng to nét hơn.
            return new ImageIcon(gui.utils.ImageUtils.highQualityScale(src, maxW, maxH));
        } catch (Exception e) {
            return null;
        }
    }

    private void openFullImage(Attachment att) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, att.name != null ? att.name : "Ảnh", Dialog.ModalityType.MODELESS);
        JLabel label = new JLabel("Đang tải…", SwingConstants.CENTER);
        label.setForeground(AppColors.TEXT_MUTED);
        dlg.setContentPane(new JScrollPane(label));
        // Cửa sổ xem ảnh chiếm tối đa ~85% màn hình ⇒ ảnh hiển thị lớn & nét hơn.
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxW = (int) (screen.width * 0.85), maxH = (int) (screen.height * 0.85);
        dlg.setSize(Math.min(900, maxW), Math.min(720, maxH));
        dlg.setLocationRelativeTo(owner);

        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() { return loadScaledIcon(att.url, maxW, maxH); }
            @Override protected void done() {
                try {
                    ImageIcon ic = get();
                    if (ic != null) { label.setText(null); label.setIcon(ic); }
                    else label.setText("[Không tải được ảnh]");
                } catch (Exception e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    label.setText("[Lỗi tải ảnh]");
                }
            }
        }.execute();
        dlg.setVisible(true);
    }

    private void downloadFile(Attachment att) {
        if (att.url == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(att.name != null ? att.name : "download"));
        if (fc.showSaveDialog(SwingUtilities.getWindowAncestor(this)) != JFileChooser.APPROVE_OPTION) return;
        final File dest = fc.getSelectedFile();

        new SwingWorker<Void, Void>() {
            private Exception err;
            @Override protected Void doInBackground() {
                try {
                    byte[] bytes = new FileApiClient().download(att.url); // JWT qua header, chặn host lạ
                    Files.write(dest.toPath(), bytes);
                } catch (Exception e) { err = e; }
                return null;
            }
            @Override protected void done() {
                Window w = SwingUtilities.getWindowAncestor(ChatMessageItem.this);
                if (err != null) {
                    JOptionPane.showMessageDialog(w, "Tải tệp thất bại: " + err.getMessage());
                } else {
                    JOptionPane.showMessageDialog(w, "Đã lưu: " + dest.getAbsolutePath());
                }
            }
        }.execute();
    }

    private String humanSize(long b) {
        if (b <= 0) return "";
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.1f MB", b / (1024.0 * 1024));
    }

    /** Badge icon cho loại tệp — không dùng emoji (tránh ô vuông). */
    private JLabel buildFileIconLabel(String name) {
        String n = name == null ? "" : name.toLowerCase();
        record FileType(String abbr, Color color) {}
        FileType ft;
        if      (n.endsWith(".pdf"))                                     ft = new FileType("PDF", new Color(0xE74C3C));
        else if (n.endsWith(".zip") || n.endsWith(".rar") || n.endsWith(".7z")) ft = new FileType("ZIP", new Color(0xF39C12));
        else if (n.endsWith(".doc") || n.endsWith(".docx"))              ft = new FileType("DOC", new Color(0x2980B9));
        else if (n.endsWith(".xls") || n.endsWith(".xlsx") || n.endsWith(".csv")) ft = new FileType("XLS", new Color(0x27AE60));
        else if (n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg")) ft = new FileType("AUD", new Color(0x9B59B6));
        else if (n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi")) ft = new FileType("VID", new Color(0xE67E22));
        else                                                             ft = new FileType("FILE", new Color(0x7F8C8D));

        JLabel label = new JLabel(ft.abbr(), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ft.color());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setPreferredSize(new Dimension(38, 38));
        label.setOpaque(false);
        return label;
    }

    /** @deprecated Thay bằng buildFileIconLabel() */
    @SuppressWarnings("unused")
    private String fileEmoji(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".pdf")) return "PDF";
        if (n.endsWith(".zip") || n.endsWith(".rar") || n.endsWith(".7z")) return "ZIP";
        if (n.endsWith(".doc") || n.endsWith(".docx")) return "DOC";
        if (n.endsWith(".xls") || n.endsWith(".xlsx") || n.endsWith(".csv")) return "XLS";
        return "FILE";
    }
}
