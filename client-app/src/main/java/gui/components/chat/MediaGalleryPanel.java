package gui.components.chat;

import gui.components.dialogs.MediaGalleryDialog;
import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Lưới thumbnail ảnh/video (4 cột, tối đa 8 ô preview) cho sidebar phải.
 * Click ô → xem ảnh gốc; nút "Xem tất cả" → mở {@link MediaGalleryDialog}.
 */
public class MediaGalleryPanel extends JPanel {

    private static final int COLS = 4, PREVIEW = 8, CELL = 52;

    private List<ChannelAttachment> images = List.of();

    public MediaGalleryPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        rebuild();
    }

    public void setImages(List<ChannelAttachment> imgs) {
        this.images = imgs != null ? imgs : List.of();
        rebuild();
    }

    private void rebuild() {
        removeAll();
        if (images.isEmpty()) {
            add(SidebarUi.mutedLabel("Chưa có ảnh/video"));
        } else {
            JPanel grid = new JPanel(new GridLayout(0, COLS, 4, 4));
            grid.setOpaque(false);
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);
            int n = Math.min(PREVIEW, images.size());
            for (int i = 0; i < n; i++) grid.add(thumbCell(images.get(i)));
            int rows = (int) Math.ceil(n / (double) COLS);
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * (CELL + 4)));
            add(grid);
            add(Box.createVerticalStrut(6));
            add(SidebarUi.linkButton("Xem tất cả (" + images.size() + ")",
                    () -> MediaGalleryDialog.open(this, images)));
        }
        revalidate();
        repaint();
    }

    private JComponent thumbCell(ChannelAttachment att) {
        JLabel cell = new JLabel("", SwingConstants.CENTER);
        cell.setPreferredSize(new Dimension(CELL, CELL));
        cell.setOpaque(true);
        cell.setBackground(AppColors.BG_TERTIARY);
        cell.setFont(AppFonts.TINY);
        cell.setForeground(AppColors.TEXT_MUTED);
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cell.setToolTipText(att.name);

        String loadUrl = att.thumbnailUrl != null ? att.thumbnailUrl : att.url;
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() { return ImageViewer.loadScaled(loadUrl, CELL, CELL); }
            @Override protected void done() {
                try {
                    ImageIcon ic = get();
                    if (ic != null) cell.setIcon(ic); else cell.setText("?");
                } catch (Exception e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    cell.setText("?");
                }
            }
        }.execute();

        cell.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                ImageViewer.open(MediaGalleryPanel.this, att.url, att.name);
            }
        });
        return cell;
    }
}
