package gui.theme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Scrollbar mảnh, bo tròn, không nút mũi tên — tông navy hiện đại.
 * Dùng cho các vùng cuộn chính (chat history, danh sách thành viên).
 */
public class ThinScrollBarUI extends BasicScrollBarUI {

    private static final int THICKNESS = 8;

    /** Gắn UI mảnh + đặt độ dày cho 1 scrollpane (cả dọc & ngang). */
    public static void apply(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(THICKNESS, 0));
        scroll.getVerticalScrollBar().setOpaque(false);
    }

    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = AppColors.BG_ACTIVE;
        this.trackColor = AppColors.BG_SECONDARY;
    }

    @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
    @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }

    private JButton zeroButton() {
        JButton b = new JButton();
        Dimension zero = new Dimension(0, 0);
        b.setPreferredSize(zero);
        b.setMinimumSize(zero);
        b.setMaximumSize(zero);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        // track trong suốt — nền cuộn liền mạch
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = 2;
        g2.setColor(isThumbRollover() ? AppColors.BG_HOVER : AppColors.BG_ACTIVE);
        g2.fillRoundRect(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, 8, 8);
        g2.dispose();
    }
}
