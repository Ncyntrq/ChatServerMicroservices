package gui.components.chat;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;
import gui.components.AvatarBadge;
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
    private boolean isEditing = false;
    private boolean isAttachment = false;
    private final boolean isHighlighted;
    private final boolean isSystemMsg;

    // Components dùng cho inline-edit & cập nhật động
    private JPanel contentPanel;
    private JPanel headerRow;
    private JTextArea messageBody;
    private JPanel toolbar;
    private JLabel editedBadgeLabel;

    /** Layout gọn cho tin nhắn liên tiếp cùng người gửi (gộp nhóm). */
    private final boolean isConsecutive;
    private final Container floatingLayer;

    /** Constructor cũ — giữ tương thích. */
    public ChatMessageItem(MessageDTO message, boolean isHighlighted) {
        this(message, isHighlighted, null, null, false, null);
    }

    public ChatMessageItem(MessageDTO message, boolean isHighlighted,
                           String currentUser, MessageActions actions) {
        this(message, isHighlighted, currentUser, actions, false, null);
    }

    public ChatMessageItem(MessageDTO message, boolean isHighlighted,
                           String currentUser, MessageActions actions, boolean isConsecutive, Container floatingLayer) {
        this.message = message;
        this.currentUser = currentUser;
        this.actions = actions;
        this.isHighlighted = isHighlighted;
        this.isConsecutive = isConsecutive;
        this.floatingLayer = floatingLayer;
        this.isOwn = currentUser != null && currentUser.equals(message.getSender());
        this.isSystemMsg = message.getType() == MessageType.SYSTEM
                || message.getType() == MessageType.JOIN
                || message.getType() == MessageType.LEAVE
                || message.getType() == MessageType.ERROR
                || "SYSTEM".equals(message.getSender());

        setLayout(new BorderLayout(12, 0));
        setOpaque(false);

        // Padding tự gánh khoảng cách (bỏ strut)
        if (isHighlighted) {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, AppColors.MSG_HIGHLIGHT_BORDER),
                    BorderFactory.createEmptyBorder(isConsecutive ? 2 : 12, 12, isConsecutive ? 2 : 4, 20)));
        } else {
            setBorder(BorderFactory.createEmptyBorder(isConsecutive ? 2 : 16, 16, isConsecutive ? 2 : 4, 20));
        }

        if (isSystemMsg) {
            buildSystemLayout(message);
            installHover(this);
        } else if (isConsecutive) {
            buildCompactLayout(message);
            if (actions != null && floatingLayer != null) {
                buildToolbar();
            }
            installHover(this);
        } else {
            buildChatLayout(message);
            if (actions != null && floatingLayer != null) {
                buildToolbar();
            }
            installHover(this);
        }
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

        // Content
        JTextPane content = new JTextPane();
        content.setEditable(false);
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

    private void buildCompactLayout(MessageDTO message) {
        JPanel leftSpacer = new JPanel(new BorderLayout());
        leftSpacer.setOpaque(false);
        leftSpacer.setPreferredSize(new Dimension(36, 10));

        JLabel timeLabel = new JLabel(message.getTimestamp() != null ? message.getTimestamp().format(TIME_FMT) : "");
        timeLabel.setFont(AppFonts.TINY);
        timeLabel.setForeground(new Color(0, 0, 0, 0)); // Hidden by default
        timeLabel.setPreferredSize(new Dimension(36, 15));
        leftSpacer.add(timeLabel, BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Header row: required for edited badge
        headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (Boolean.TRUE.equals(message.getIsEdited())) {
            addEditedBadge();
        }
        contentPanel.add(headerRow);

        Attachment att = parseAttachment(message.getContent());
        this.isAttachment = att != null;
        if (att != null) {
            contentPanel.add(att.isImage() ? buildImageAttachment(att) : buildFileCard(att));
        } else {
            messageBody = new JTextArea(message.getContent());
            messageBody.setLineWrap(true);
            messageBody.setWrapStyleWord(true);
            messageBody.setEditable(false);
            messageBody.setOpaque(false);
            applyMessageStyle(messageBody, message.getContent());
            messageBody.setAlignmentX(Component.LEFT_ALIGNMENT);
            messageBody.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            contentPanel.add(messageBody);
        }

        add(leftSpacer, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void buildChatLayout(MessageDTO message) {
        String senderName = message.getSender();
        String initial = senderName != null && !senderName.isEmpty()
                ? senderName.substring(0, 1).toUpperCase()
                : "?";

        // Avatar: size 36 for compact feel
        AvatarBadge avatar = new AvatarBadge(initial, 36);
        JPanel avatarWrapper = new JPanel(new BorderLayout());
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatar, BorderLayout.NORTH);

        // Content panel
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Header row: username + badges + timestamp
        headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel senderLabel = new JLabel(senderName);
        senderLabel.setFont(AppFonts.BODY_BOLD);
        senderLabel.setForeground(AppColors.avatarColorFor(senderName));
        headerRow.add(senderLabel);

        // Async fetch profile for displayName and avatar
        new SwingWorker<java.util.Map<String, Object>, Void>() {
            @Override
            protected java.util.Map<String, Object> doInBackground() {
                return new network.UserProfileApiClient().getProfile(senderName);
            }

            @Override
            protected void done() {
                try {
                    java.util.Map<String, Object> profile = get();
                    if (profile != null) {
                        if (profile.get("displayName") != null && !profile.get("displayName").toString().isBlank()) {
                            senderLabel.setText(profile.get("displayName").toString());
                        }
                        if (profile.get("avatarUrl") != null) {
                            String url = profile.get("avatarUrl").toString();
                            if (!url.startsWith("http"))
                                url = network.ApiConfig.GATEWAY_HTTP + url;
                            avatar.loadAvatarFromUrl(url);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception ignore) {
                }
            }
        }.execute();

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
                ? message.getTimestamp().format(TIME_FMT)
                : "Bây giờ";
        JLabel timeLabel = new JLabel(timeStr);
        timeLabel.setFont(AppFonts.CAPTION);
        timeLabel.setForeground(new Color(0x80, 0x84, 0x8E, 0x99));
        headerRow.add(timeLabel);

        // Edited badge (nếu đã sửa từ trước)
        if (Boolean.TRUE.equals(message.getIsEdited())) {
            addEditedBadge();
        }

        contentPanel.add(headerRow);

        // Phân biệt: tin nhắn đính kèm file/ảnh hay text thường
        Attachment att = parseAttachment(message.getContent());
        this.isAttachment = att != null;
        if (att != null) {
            contentPanel.add(att.isImage() ? buildImageAttachment(att) : buildFileCard(att));
        } else {
            // Message body (text thường)
            messageBody = new JTextArea(message.getContent());
            messageBody.setLineWrap(true);
            messageBody.setWrapStyleWord(true);
            messageBody.setEditable(false);
            messageBody.setOpaque(false);
            applyMessageStyle(messageBody, message.getContent());
            messageBody.setAlignmentX(Component.LEFT_ALIGNMENT);
            messageBody.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
            contentPanel.add(messageBody);
        }

        add(avatarWrapper, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createMessageBodyWrapper(String content, int topPadding) {
        JTextPane messageBody = new JTextPane();
        messageBody.setEditable(false);
        messageBody.setOpaque(false);
        messageBody.setForeground(AppColors.TEXT_NORMAL);
        messageBody.setFont(AppFonts.BODY);
        messageBody.setBorder(BorderFactory.createEmptyBorder(topPadding, 0, 0, 0));
        EmojiHelper.renderTextWithEmojis(messageBody, content);

        JPanel messageWrapper = new JPanel(new BorderLayout());
        messageWrapper.setOpaque(false);
        messageWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageWrapper.add(messageBody, BorderLayout.CENTER);

        return messageWrapper;
    }

    private void applyMessageStyle(JTextArea textArea, String content) {
        if ("Tin nhắn bị gỡ".equals(content)) {
            textArea.setFont(AppFonts.BODY_ITALIC);
            textArea.setForeground(AppColors.TEXT_MUTED);
        } else {
            textArea.setFont(AppFonts.BODY);
            textArea.setForeground(AppColors.TEXT_NORMAL);
        }
    }

    private void addEditedBadge() {
        if (editedBadgeLabel == null) {
            editedBadgeLabel = new JLabel("(đã sửa)");
            editedBadgeLabel.setFont(AppFonts.TINY);
            editedBadgeLabel.setForeground(AppColors.TEXT_MUTED);
            headerRow.add(editedBadgeLabel);
            headerRow.revalidate();
            headerRow.repaint();
        }
        editedBadgeLabel.setVisible(!"Tin nhắn bị gỡ".equals(message.getContent()));
    }

    // ---------------------------------------------------------------
    // Toolbar nổi (Sửa / Ghim / Xóa)
    // ---------------------------------------------------------------
    public JPanel getToolbar() {
        return toolbar;
    }

    private void buildToolbar() {
        toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 1)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppColors.BG_FLOATING);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setStroke(new BasicStroke(0.7f));
                g2.setColor(new Color(255, 255, 255, 20)); // Subtle light grey
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));

        boolean canManage = network.PermissionCache.get().can(network.PermissionCache.MANAGE_MESSAGES);

        // Sửa — chỉ cho tin text của chính mình (không áp dụng cho file đính kèm)
        if (isOwn && !isAttachment) {
            IconButton editBtn = new IconButton("✏", e -> startEditing());
            editBtn.setFont(AppFonts.EMOJI_SM);
            editBtn.setPreferredSize(new Dimension(28, 28));
            editBtn.setToolTipText("Sửa");
            toolbar.add(editBtn);
        }
        // Ghim — chỉ người có quyền quản lý tin nhắn
        if (canManage) {
            IconButton pinBtn = new IconButton("📌", e -> {
                if (actions != null) actions.onPin(message);
            });
            pinBtn.setFont(AppFonts.EMOJI_SM);
            pinBtn.setPreferredSize(new Dimension(28, 28));
            pinBtn.setToolTipText("Ghim");
            toolbar.add(pinBtn);
        }
        // Xóa — chủ tin hoặc người có quyền quản lý tin nhắn (backend vẫn kiểm tra lại)
        if (isOwn || canManage) {
            IconButton delBtn = new IconButton("🗑", e -> confirmDelete());
            delBtn.setFont(AppFonts.EMOJI_SM);
            delBtn.setPreferredSize(new Dimension(28, 28));
            delBtn.setToolTipText("Xóa");
            toolbar.add(delBtn);
        }

        toolbar.setVisible(false);
        floatingLayer.add(toolbar);

        // Giữ hover nếu chuột di chuyển vào trong toolbar
        toolbar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                Point pItem = SwingUtilities.convertPoint(toolbar, e.getPoint(), ChatMessageItem.this);
                if (!ChatMessageItem.this.contains(pItem)) {
                    setHover(false);
                }
            }
        });
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
        if (isEditing || isAttachment || messageBody == null || !isOwn || "Tin nhắn bị gỡ".equals(message.getContent())) return;
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
            applyMessageStyle(messageBody, newContent);
            messageBody.revalidate();
        }
        message.setContent(newContent);
        if (edited) {
            message.setIsEdited(true);
            addEditedBadge();
        } else if (editedBadgeLabel != null) {
            editedBadgeLabel.setVisible(!"Tin nhắn bị gỡ".equals(newContent));
        }
        if ("Tin nhắn bị gỡ".equals(newContent) && toolbar != null) {
            toolbar.setVisible(false);
        }
    }

    public Long getMessageId() {
        return message.getMessageId();
    }

    public MessageDTO getMessage() {
        return message;
    }

    public boolean isConsecutive() {
        return isConsecutive;
    }

    public boolean isHighlighted() {
        return isHighlighted;
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
        @Override
        public void mouseEntered(MouseEvent e) {
            setHover(true);
        }
        @Override
        public void mouseExited(MouseEvent e) {
            Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), ChatMessageItem.this);
            Point pTool = toolbar != null ? SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), toolbar) : new Point(-1, -1);
            if (!ChatMessageItem.this.contains(p) && (toolbar == null || !toolbar.contains(pTool))) {
                setHover(false);
            }
        }
    };

    private void setHover(boolean hovered) {
        if (isHovered == hovered) return;
        isHovered = hovered;
        if (toolbar != null && !isEditing && !"Tin nhắn bị gỡ".equals(message.getContent())) {
            if (hovered) {
                int tw = toolbar.getPreferredSize().width;
                int th = toolbar.getPreferredSize().height;
                // Vị trí absolute đối với floatingLayer: neo bên phải, nổi lêntrên tin nhắn một chút
                toolbar.setBounds(getX() + getWidth() - tw - 24, getY() - 10, tw, th);
                toolbar.setVisible(true);
            } else {
                toolbar.setVisible(false);
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isHighlighted) {
            g2.setColor(AppColors.MSG_HIGHLIGHT_BG);
            g2.fillRoundRect(6, 1, getWidth() - 12, getHeight() - 2, 12, 12);
        } else if ((isHovered || isEditing) && !isSystemMsg) {
            g2.setColor(AppColors.BG_MESSAGE_HOVER);
            g2.fillRoundRect(6, 1, getWidth() - 12, getHeight() - 2, 12, 12);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
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

        JLabel icon = new JLabel(fileEmoji(att.name));
        icon.setFont(AppFonts.EMOJI);
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

        IconButton download = new IconButton("⬇", e -> downloadFile(att));
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
            int w = src.getWidth(), h = src.getHeight();
            double scale = Math.min(1.0, Math.min(maxW / (double) w, maxH / (double) h));
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            Image scaled = src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
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
        dlg.setSize(720, 580);
        dlg.setLocationRelativeTo(owner);

        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() { return loadScaledIcon(att.url, 1200, 900); }
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

    private String fileEmoji(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".pdf")) return "📕";
        if (n.endsWith(".zip") || n.endsWith(".rar") || n.endsWith(".7z")) return "📦";
        if (n.endsWith(".doc") || n.endsWith(".docx")) return "📝";
        if (n.endsWith(".xls") || n.endsWith(".xlsx") || n.endsWith(".csv")) return "📊";
        return "📄";
    }
}
