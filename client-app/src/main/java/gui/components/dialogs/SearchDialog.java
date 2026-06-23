package gui.components.dialogs;

import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.ChannelApiClient;
import com.chatsever.common.dto.MessageDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialog tìm kiếm tin nhắn theo từ khóa trong kênh hoặc server.
 */
public class SearchDialog extends JDialog {

    private final ChannelApiClient channelApi;
    private final Long channelId;
    private final Long serverId;
    private final JTextField searchField;
    private final JPanel resultsPanel;
    private final JLabel statusLabel;
    private final OnMessageSelectedListener listener;

    public interface OnMessageSelectedListener {
        void onMessageSelected(Long messageId);
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public SearchDialog(Window owner, ChannelApiClient channelApi, Long channelId, Long serverId, OnMessageSelectedListener listener) {
        super(owner, "Tìm kiếm tin nhắn", ModalityType.APPLICATION_MODAL);
        this.channelApi = channelApi;
        this.channelId = channelId;
        this.serverId = serverId;
        this.listener = listener;

        setSize(520, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppColors.BG_SECONDARY);

        // --- Top: search bar ---
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        topPanel.setBackground(AppColors.BG_SECONDARY);
        topPanel.setBorder(new EmptyBorder(16, 16, 12, 16));

        searchField = new JTextField();
        searchField.setFont(AppFonts.BODY);
        searchField.setBackground(AppColors.BG_ACTIVE);
        searchField.setForeground(AppColors.TEXT_NORMAL);
        searchField.setCaretColor(AppColors.TEXT_NORMAL);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BG_TERTIARY),
                new EmptyBorder(8, 12, 8, 12)));
        searchField.addActionListener(e -> doSearch());

        JButton searchBtn = new JButton("Tìm");
        searchBtn.setFont(AppFonts.BODY_BOLD);
        searchBtn.setBackground(AppColors.BRAND_PRIMARY);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.setBorder(new EmptyBorder(8, 16, 8, 16));
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchBtn.addActionListener(e -> doSearch());

        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(searchBtn, BorderLayout.EAST);

        // --- Center: results ---
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(AppColors.BG_PRIMARY);

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // --- Bottom: status ---
        statusLabel = new JLabel("Nhập từ khóa và nhấn Enter để tìm kiếm");
        statusLabel.setFont(AppFonts.TINY);
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setBorder(new EmptyBorder(8, 16, 12, 16));

        add(topPanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void doSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) return;

        statusLabel.setText("Đang tìm kiếm...");
        resultsPanel.removeAll();
        resultsPanel.revalidate();
        resultsPanel.repaint();

        new SwingWorker<List<MessageDTO>, Void>() {
            @Override
            protected List<MessageDTO> doInBackground() {
                return channelApi.searchMessages(channelId, serverId, keyword, 50);
            }

            @Override
            protected void done() {
                try {
                    List<MessageDTO> results = get();
                    resultsPanel.removeAll();

                    if (results.isEmpty()) {
                        statusLabel.setText("Không tìm thấy kết quả nào.");
                        JLabel empty = new JLabel("Không có kết quả phù hợp.");
                        empty.setFont(AppFonts.BODY);
                        empty.setForeground(AppColors.TEXT_MUTED);
                        empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                        empty.setBorder(new EmptyBorder(40, 0, 0, 0));
                        resultsPanel.add(empty);
                    } else {
                        statusLabel.setText("Tìm thấy " + results.size() + " kết quả.");
                        for (MessageDTO msg : results) {
                            resultsPanel.add(buildResultItem(msg, keyword));
                        }
                    }
                    resultsPanel.revalidate();
                    resultsPanel.repaint();
                } catch (Exception e) {
                    statusLabel.setText("Lỗi tìm kiếm: " + e.getMessage());
                }
            }
        }.execute();
    }

    private JPanel buildResultItem(MessageDTO msg, String keyword) {
        JPanel item = new JPanel(new BorderLayout(8, 4));
        item.setBackground(AppColors.BG_PRIMARY);
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BG_TERTIARY),
                new EmptyBorder(10, 16, 10, 16)));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Header: sender + time
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel senderLabel = new JLabel(msg.getSender() != null ? msg.getSender() : "???");
        senderLabel.setFont(AppFonts.BODY_BOLD);
        senderLabel.setForeground(AppColors.TEXT_HEADER);

        String timeStr = msg.getTimestamp() != null ? msg.getTimestamp().format(TIME_FMT) : "";
        JLabel timeLabel = new JLabel(timeStr);
        timeLabel.setFont(AppFonts.TINY);
        timeLabel.setForeground(AppColors.TEXT_MUTED);

        header.add(senderLabel, BorderLayout.WEST);
        header.add(timeLabel, BorderLayout.EAST);

        // Content with highlighted keyword
        String content = msg.getContent() != null ? msg.getContent() : "";
        // Truncate long messages
        if (content.length() > 150) content = content.substring(0, 150) + "…";

        JLabel contentLabel = new JLabel("<html>" + highlightKeyword(content, keyword) + "</html>");
        contentLabel.setFont(AppFonts.BODY);
        contentLabel.setForeground(AppColors.TEXT_NORMAL);

        item.add(header, BorderLayout.NORTH);
        item.add(contentLabel, BorderLayout.CENTER);

        // Hover effect
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                item.setBackground(AppColors.BG_ACTIVE);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                item.setBackground(AppColors.BG_PRIMARY);
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (listener != null) {
                    listener.onMessageSelected(msg.getMessageId());
                    dispose();
                }
            }
        });

        return item;
    }

    /** Tô đậm từ khóa trong nội dung (HTML). */
    private String highlightKeyword(String text, String keyword) {
        if (keyword == null || keyword.isEmpty()) return escapeHtml(text);
        String escaped = escapeHtml(text);
        String escapedKeyword = escapeHtml(keyword);
        return escaped.replaceAll("(?i)" + java.util.regex.Pattern.quote(escapedKeyword),
                "<b style='color:#5865F2;'>" + escapedKeyword + "</b>");
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
