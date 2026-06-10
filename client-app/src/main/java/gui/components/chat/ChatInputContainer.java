package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class ChatInputContainer extends JPanel {
    /** Số dòng tối đa trước khi ô nhập chuyển sang cuộn trong (auto-grow trần 6 dòng). */
    private static final int MAX_ROWS = 6;

    private final JTextArea inputArea;
    private final JScrollPane inputScroll;
    private final JButton sendButton;
    private final JProgressBar uploadBar;
    private Runnable onAttach = () -> {};
    private Runnable onSend = () -> {};
    private final JPopupMenu mentionPopup = new JPopupMenu();
    private java.util.List<String> availableMentions = new java.util.ArrayList<>();

    // Hàm để ChatClientGUI truyền dữ liệu thật vào
    public void setAvailableMentions(java.util.List<String> mentions) {
        this.availableMentions = mentions != null ? mentions : new java.util.ArrayList<>();
    }

    /** Gắn handler khi bấm nút đính kèm (+). */
    public void setOnAttach(Runnable r) {
        this.onAttach = r != null ? r : () -> {};
    }

    /** Gắn handler khi gửi (Enter hoặc nút Gửi). */
    public void setOnSend(Runnable r) {
        this.onSend = r != null ? r : () -> {};
    }

    public ChatInputContainer() {
        setLayout(new BorderLayout(8, 0));
        setBackground(AppColors.BG_TERTIARY);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Make the whole container have rounded corners
        putClientProperty("JComponent.arc", 12);

        // --- 1. Left icon (Plus/Attach) ---
        IconButton plusButton = new IconButton("+", e -> onAttach.run());
        plusButton.setToolTipText("Đính kèm tệp");
        JPanel leftWrap = new JPanel(new GridBagLayout());
        leftWrap.setOpaque(false);
        leftWrap.add(plusButton);

        // --- 2. Main text area (đa dòng, auto-grow) ---
        inputArea = new JTextArea(1, 0);
        inputArea.putClientProperty("JTextField.placeholderText", "Nhập tin nhắn...");
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(AppColors.BG_TERTIARY);
        inputArea.setForeground(AppColors.TEXT_NORMAL);
        inputArea.setCaretColor(AppColors.TEXT_WHITE);
        inputArea.setFont(AppFonts.BODY);
        inputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inputArea.setSelectionColor(AppColors.BRAND_PRIMARY);

        // Enter = gửi; Shift+Enter = xuống dòng (insert-break là action mặc định của editor).
        InputMap im = inputArea.getInputMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send-message");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-break");
        inputArea.getActionMap().put("send-message", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onSend.run(); }
        });

        // Auto-grow theo nội dung (1 → MAX_ROWS dòng), vượt thì cuộn trong ô.
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { adjustInputHeight(); }
            @Override public void removeUpdate(DocumentEvent e) { adjustInputHeight(); }
            @Override public void changedUpdate(DocumentEvent e) { adjustInputHeight(); }
        });

        // --- Xử lý Tag (@) Mention ---
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { checkMention(); }
            @Override public void removeUpdate(DocumentEvent e) { checkMention(); }
            @Override public void changedUpdate(DocumentEvent e) { checkMention(); }
        });

        inputScroll = new JScrollPane(inputArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setBorder(BorderFactory.createEmptyBorder());
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);

        // --- 3. Right panel: emoji + send ---
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightPanel.setOpaque(false);

        rightPanel.add(new IconButton("🎁", e -> System.out.println("Gift menu...")));
        IconButton emojiButton = new IconButton("😀", e -> {
            JPopupMenu emojiMenu = new JPopupMenu();
            emojiMenu.setLayout(new GridLayout(3, 5, 4, 4));
            emojiMenu.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            for (Map.Entry<String, String> entry : EmojiHelper.EMOJIS.entrySet()) {
                String shortcode = entry.getKey();
                String twemojiCode = entry.getValue();

                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(36, 36));
                btn.setFocusPainted(false);
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                // Get preloaded icon or load it
                ImageIcon icon = EmojiHelper.getEmojiIcon(twemojiCode, 24);
                if (icon != null) {
                    btn.setIcon(icon);
                } else {
                    btn.setText(shortcode);
                }

                btn.addActionListener(ev -> {
                    inputArea.replaceSelection(shortcode + " ");
                    emojiMenu.setVisible(false);
                    inputArea.requestFocusInWindow();
                });

                emojiMenu.add(btn);
            }
            Component source = (Component) e.getSource();
            emojiMenu.show(source, 0, -150);
        });
        rightPanel.add(emojiButton);

        // Send button — pill shape
        sendButton = new JButton("Gửi") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        sendButton.setBackground(AppColors.BRAND_PRIMARY);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setBorderPainted(false);
        sendButton.setFont(AppFonts.CAPTION_BOLD);
        sendButton.setPreferredSize(new Dimension(56, 32));
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setToolTipText("Gửi (Enter)");

        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(AppColors.BRAND_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(AppColors.BRAND_PRIMARY);
            }
        });

        rightPanel.add(sendButton);

        JPanel rightWrap = new JPanel(new GridBagLayout());
        rightWrap.setOpaque(false);
        rightWrap.add(rightPanel);

        // --- Thanh tiến trình upload (ẩn mặc định) ---
        uploadBar = new JProgressBar();
        uploadBar.setIndeterminate(true);
        uploadBar.setVisible(false);
        uploadBar.setBorderPainted(false);
        uploadBar.setForeground(AppColors.BRAND_PRIMARY);
        uploadBar.setBackground(AppColors.BG_TERTIARY);
        uploadBar.setPreferredSize(new Dimension(0, 3));

        // --- Assemble ---
        add(uploadBar, BorderLayout.NORTH);
        add(leftWrap, BorderLayout.WEST);
        add(inputScroll, BorderLayout.CENTER);
        add(rightWrap, BorderLayout.EAST);

        adjustInputHeight(); // đặt chiều cao 1 dòng ban đầu
    }

    /** Tính lại chiều cao ô nhập theo số dòng hiển thị, kẹp trong [1 dòng, MAX_ROWS dòng]. */
    private void adjustInputHeight() {
        int lineH = inputArea.getFontMetrics(inputArea.getFont()).getHeight();
        Insets in = inputArea.getInsets();
        int oneRow = lineH + in.top + in.bottom;
        int maxH = lineH * MAX_ROWS + in.top + in.bottom;
        int pref = inputArea.getPreferredSize().height; // đã gồm các dòng wrap khi biết bề rộng
        int h = Math.max(oneRow, Math.min(pref, maxH));
        inputScroll.setPreferredSize(new Dimension(0, h));
        revalidate();
        repaint();
    }

    /** Hiện/ẩn thanh tiến trình khi đang tải tệp lên. */
    public void setUploading(boolean active) {
        uploadBar.setVisible(active);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(AppColors.BG_TERTIARY);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }

    public String getMessageText() {
        return inputArea.getText();
    }

    public void clearInput() {
        inputArea.setText(""); // DocumentListener sẽ tự thu ô về 1 dòng
        // JTextArea preferred size chưa kịp cập nhật khi line-wrap bật → ép về 1 dòng thủ công
        int lineH = inputArea.getFontMetrics(inputArea.getFont()).getHeight();
        Insets in = inputArea.getInsets();
        int oneRow = lineH + in.top + in.bottom;
        inputScroll.setPreferredSize(new Dimension(0, oneRow));
        revalidate();
        repaint();
    }

    public JButton getSendButton() {
        return sendButton;
    }

    public JTextArea getInputArea() {
        return inputArea;
    }

    private void checkMention() {
        // Dùng invokeLater để đợi Document cập nhật xong text
        SwingUtilities.invokeLater(() -> {
            try {
                int caret = inputArea.getCaretPosition();
                String text = inputArea.getText();
                if (caret == 0) { mentionPopup.setVisible(false); return; }

                // Tìm vị trí bắt đầu của từ đang gõ (dấu cách hoặc xuống dòng gần nhất)
                int startSpace = text.lastIndexOf(" ", caret - 1);
                int startNewline = text.lastIndexOf("\n", caret - 1);
                int start = Math.max(startSpace, startNewline);
                start = (start == -1) ? 0 : start + 1;

                String currentWord = text.substring(start, caret);

                // Nếu từ đang gõ bắt đầu bằng '@'
                if (currentWord.startsWith("@")) {
                    String query = currentWord.substring(1); // Lấy phần chữ sau '@'
                    showMentionPopup(query, start, caret);
                } else {
                    mentionPopup.setVisible(false);
                }
            } catch (Exception ex) {
                mentionPopup.setVisible(false);
            }
        });
    }

    private void showMentionPopup(String query, int wordStart, int caret) {
        mentionPopup.removeAll();

        boolean hasItem = false;
        // Quét trên danh sách thực tế đã được truyền từ Server vào
        for (String member : availableMentions) {
            // Lọc danh sách theo từ khoá đang gõ
            if (member.toLowerCase().startsWith(query.toLowerCase())) {
                JMenuItem item = new JMenuItem(member);
                item.setFont(AppFonts.BODY);
                item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                item.addActionListener(e -> {
                    try {
                        // Khi click, thay thế chuỗi @... đang gõ bằng @username hoàn chỉnh
                        inputArea.getDocument().remove(wordStart, inputArea.getCaretPosition() - wordStart);
                        inputArea.getDocument().insertString(wordStart, "@" + member + " ", null);
                        mentionPopup.setVisible(false);
                        inputArea.requestFocusInWindow(); // Trả lại focus cho ô nhập
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                mentionPopup.add(item);
                hasItem = true;
            }
        }

        if (hasItem) {
            try {
                // Tính toán toạ độ để hiển thị Popup ngay trên chữ @
                Rectangle rect = inputArea.modelToView2D(wordStart).getBounds();
                mentionPopup.show(inputArea, (int) rect.getX(), (int) rect.getY() - mentionPopup.getPreferredSize().height);
            } catch (Exception ex) {}
        } else {
            mentionPopup.setVisible(false);
        }
    }
}