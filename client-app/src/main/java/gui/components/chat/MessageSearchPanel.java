package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;
import com.chatsever.common.dto.MessageDTO;
import network.MessageSearchApiClient;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog tìm kiếm tin nhắn. Có ô nhập (debounce ~300ms), chọn phạm vi
 * (kênh/DM hiện tại vs toàn cục), danh sách kết quả highlight từ khóa.
 * Click 1 kết quả → callback onJump để cuộn tới tin nhắn trong ChatHistoryView.
 */
public class MessageSearchPanel extends JDialog {

    private static final int LIMIT = 50;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final MessageSearchApiClient api = new MessageSearchApiClient();
    private final long activeChannelId;
    private final String activePrivateUser;
    private final Consumer<MessageDTO> onJump;

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> scopeBox = new JComboBox<>();
    private final JPanel resultsPanel = new JPanel();
    private final Timer debounce;

    public MessageSearchPanel(JFrame owner, long activeChannelId, String activePrivateUser, Consumer<MessageDTO> onJump) {
        super(owner, "Tìm kiếm tin nhắn", false);
        this.activeChannelId = activeChannelId;
        this.activePrivateUser = activePrivateUser;
        this.onJump = onJump;

        this.debounce = new Timer(300, e -> runSearch());
        this.debounce.setRepeats(false);

        setSize(460, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppColors.BG_PRIMARY);

        add(buildHeader(), BorderLayout.NORTH);

        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(AppColors.BG_PRIMARY);
        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        gui.theme.ThinScrollBarUI.apply(scroll);
        add(scroll, BorderLayout.CENTER);

        showHint("Nhập ít nhất 2 ký tự để tìm kiếm…");
        SwingUtilities.invokeLater(searchField::requestFocusInWindow);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setBackground(AppColors.BG_PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));

        searchField.setFont(AppFonts.BODY);
        searchField.setBackground(AppColors.BG_TERTIARY);
        searchField.setForeground(AppColors.TEXT_NORMAL);
        searchField.setCaretColor(AppColors.TEXT_WHITE);
        searchField.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        searchField.getDocument().addDocumentListener(new SimpleDocListener(debounce::restart));
        header.add(searchField, BorderLayout.CENTER);

        // Phạm vi: chỉ hiện lựa chọn "ở đây" khi đang mở kênh/DM
        if (activeChannelId != -1) scopeBox.addItem("Trong kênh này");
        else if (activePrivateUser != null) scopeBox.addItem("Trong cuộc trò chuyện này");
        scopeBox.addItem("Tất cả tin nhắn");
        scopeBox.addActionListener(e -> debounce.restart());
        scopeBox.setFont(AppFonts.CAPTION);
        header.add(scopeBox, BorderLayout.SOUTH);

        return header;
    }

    private void runSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.length() < 2) {
            showHint("Nhập ít nhất 2 ký tự để tìm kiếm…");
            return;
        }
        boolean here = !"Tất cả tin nhắn".equals(scopeBox.getSelectedItem());
        showHint("Đang tìm…");

        new SwingWorker<List<MessageDTO>, Void>() {
            @Override protected List<MessageDTO> doInBackground() {
                if (here && activeChannelId != -1) return api.searchInChannel(activeChannelId, keyword, LIMIT);
                if (here && activePrivateUser != null) return api.searchInPrivate(activePrivateUser, keyword, LIMIT);
                return api.searchAll(keyword, LIMIT);
            }
            @Override protected void done() {
                try { renderResults(get(), keyword); }
                catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    showHint("Lỗi tìm kiếm: " + c.getMessage());
                }
            }
        }.execute();
    }

    private void renderResults(List<MessageDTO> results, String keyword) {
        resultsPanel.removeAll();
        if (results.isEmpty()) {
            showHint("Không tìm thấy tin nhắn nào khớp \"" + keyword + "\"");
            return;
        }
        for (MessageDTO m : results) resultsPanel.add(buildResultRow(m, keyword));
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JComponent buildResultRow(MessageDTO m, String keyword) {
        String time = m.getTimestamp() != null ? m.getTimestamp().format(TIME_FMT) : "";
        String snippet = highlight(m.getContent() == null ? "" : m.getContent(), keyword);
        JLabel label = new JLabel("<html><div style='width:400px;'><b>" + esc(m.getSender())
                + "</b> <span style='color:#8B92A0;'>· " + time + "</span><br>" + snippet + "</div></html>");
        label.setFont(AppFonts.BODY);
        label.setForeground(AppColors.TEXT_NORMAL);
        label.setOpaque(true);
        label.setBackground(AppColors.BG_PRIMARY);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.SEPARATOR),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { label.setBackground(AppColors.BG_MESSAGE_HOVER); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { label.setBackground(AppColors.BG_PRIMARY); }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onJump != null) onJump.accept(m);
            }
        });
        return label;
    }

    /** Bôi đậm từ khóa trong đoạn nội dung (an toàn HTML). */
    private String highlight(String content, String keyword) {
        String safe = esc(content);
        if (keyword.isBlank()) return safe;
        String safeKw = java.util.regex.Pattern.quote(esc(keyword));
        return safe.replaceAll("(?i)(" + safeKw + ")",
                "<span style='background:#5B6CFF;color:#fff;'>$1</span>");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void showHint(String text) {
        resultsPanel.removeAll();
        JLabel hint = new JLabel(text);
        hint.setFont(AppFonts.BODY);
        hint.setForeground(AppColors.TEXT_MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(16, 14, 16, 14));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultsPanel.add(hint);
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    /** DocumentListener gọn cho 3 sự kiện thay đổi text. */
    private record SimpleDocListener(Runnable onChange) implements javax.swing.event.DocumentListener {
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    }
}
