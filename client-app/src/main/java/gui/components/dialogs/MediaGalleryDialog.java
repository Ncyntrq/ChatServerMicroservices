package gui.components.dialogs;

import gui.components.chat.ChannelAttachment;
import gui.components.chat.ImageViewer;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import gui.theme.ThinScrollBarUI;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Dialog "Xem tất cả" ảnh/video của channel — lưới thumbnail lớn, click xem ảnh gốc. */
public final class MediaGalleryDialog {

    private static final int COLS = 4, CELL = 110;

    private MediaGalleryDialog() {}

    public static void open(Component anchor, List<ChannelAttachment> images) {
        Window owner = anchor != null ? SwingUtilities.getWindowAncestor(anchor) : null;
        JDialog dlg = new JDialog(owner, "Ảnh & Video — " + images.size(), Dialog.ModalityType.MODELESS);

        JPanel grid = new JPanel(new GridLayout(0, COLS, 6, 6));
        grid.setBackground(AppColors.BG_SECONDARY);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (ChannelAttachment att : images) grid.add(cell(att));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(AppColors.BG_SECONDARY);
        wrap.add(grid, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ThinScrollBarUI.apply(scroll);

        dlg.setContentPane(scroll);
        dlg.setSize(560, 600);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    private static JComponent cell(ChannelAttachment att) {
        JLabel c = new JLabel("", SwingConstants.CENTER);
        c.setPreferredSize(new Dimension(CELL, CELL));
        c.setOpaque(true);
        c.setBackground(AppColors.BG_TERTIARY);
        c.setFont(AppFonts.TINY);
        c.setForeground(AppColors.TEXT_MUTED);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.setToolTipText(att.name);

        String loadUrl = att.thumbnailUrl != null ? att.thumbnailUrl : att.url;
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() { return ImageViewer.loadScaled(loadUrl, CELL, CELL); }
            @Override protected void done() {
                try { ImageIcon ic = get(); if (ic != null) c.setIcon(ic); else c.setText("?"); }
                catch (Exception e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    c.setText("?");
                }
            }
        }.execute();

        c.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                ImageViewer.open(c, att.url, att.name);
            }
        });
        return c;
    }
}
