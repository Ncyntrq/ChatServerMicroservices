package gui.components.chat;

import gui.components.dialogs.FileListDialog;
import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Danh sách file (tài liệu) gần nhất cho sidebar phải — tối đa 3 card preview.
 * Click card → tải file; nút "Xem tất cả" → mở {@link FileListDialog}.
 */
public class FileListPanel extends JPanel {

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int PREVIEW = 3;

    private List<ChannelAttachment> files = List.of();

    public FileListPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        rebuild();
    }

    public void setFiles(List<ChannelAttachment> list) {
        this.files = list != null ? list : List.of();
        rebuild();
    }

    private void rebuild() {
        removeAll();
        if (files.isEmpty()) {
            add(SidebarUi.mutedLabel("Chưa có file"));
        } else {
            int n = Math.min(PREVIEW, files.size());
            for (int i = 0; i < n; i++) {
                add(fileCard(files.get(i)));
                add(Box.createVerticalStrut(6));
            }
            add(SidebarUi.linkButton("Xem tất cả (" + files.size() + ")",
                    () -> FileListDialog.open(this, files)));
        }
        revalidate();
        repaint();
    }

    /** Card file dùng chung cho sidebar & dialog: badge + tên + (size · ngày), click = tải. */
    public static JComponent fileCard(ChannelAttachment att) {
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
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
        card.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Bấm để tải xuống");

        card.add(FileBadge.make(att.name, 32), BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        JLabel name = new JLabel(att.name != null ? att.name : "tệp tin");
        name.setFont(AppFonts.BODY_SM);
        name.setForeground(AppColors.TEXT_NORMAL);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        String meta = FileBadge.humanSize(att.size);
        if (att.createdAt != null) meta = (meta.isEmpty() ? "" : meta + " · ") + att.createdAt.format(DATE_FMT);
        JLabel sub = new JLabel(meta);
        sub.setFont(AppFonts.TINY);
        sub.setForeground(AppColors.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(name);
        center.add(sub);
        card.add(center, BorderLayout.CENTER);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                FileDownloader.save(card, att.url, att.name);
            }
        });
        return card;
    }
}
