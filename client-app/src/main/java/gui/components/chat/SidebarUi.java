package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Tiện ích UI nhỏ dùng chung cho các panel sidebar (label trống, nút "Xem tất cả"). */
final class SidebarUi {

    private SidebarUi() {}

    /** Label mờ canh trái — dùng cho trạng thái rỗng ("Chưa có ảnh/video"…). */
    static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppFonts.BODY_SM);
        l.setForeground(AppColors.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        return l;
    }

    /** Nút dạng link "Xem tất cả" — phẳng, màu link, canh trái. */
    static JButton linkButton(String text, Runnable onClick) {
        JButton b = new JButton(text);
        b.setFont(AppFonts.BODY_SM);
        b.setForeground(AppColors.TEXT_LINK);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setMargin(new Insets(2, 0, 2, 0));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> onClick.run());
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(AppColors.TEXT_WHITE); }
            @Override public void mouseExited(MouseEvent e) { b.setForeground(AppColors.TEXT_LINK); }
        });
        return b;
    }
}
