package gui.components.chat;

import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Section sidebar có tiêu đề bấm để mở/đóng (collapsible): mũi tên ▼/▶ + tiêu đề,
 * bên dưới là panel nội dung ẩn/hiện. Dùng cho Ảnh/Video, File, Thành viên.
 */
public class CollapsibleSection extends JPanel {

    private final JComponent body;
    private final JLabel arrow;
    private final JLabel titleLabel;
    private boolean expanded;

    public CollapsibleSection(String title, JComponent body, boolean expanded) {
        this.body = body;
        this.expanded = expanded;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        arrow = new JLabel(expanded ? "▼" : "▶");
        arrow.setForeground(AppColors.TEXT_MUTED);
        arrow.setFont(new Font("SansSerif", Font.PLAIN, 9));
        arrow.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 6));

        titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setForeground(AppColors.TEXT_MUTED);
        titleLabel.setFont(AppFonts.CAPTION_BOLD);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(arrow);
        left.add(titleLabel);
        header.add(left, BorderLayout.WEST);
        header.setCursor(new Cursor(Cursor.HAND_CURSOR));

        header.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle(); }
            @Override public void mouseEntered(MouseEvent e) {
                arrow.setForeground(AppColors.TEXT_WHITE);
                titleLabel.setForeground(AppColors.TEXT_WHITE);
            }
            @Override public void mouseExited(MouseEvent e) {
                arrow.setForeground(AppColors.TEXT_MUTED);
                titleLabel.setForeground(AppColors.TEXT_MUTED);
            }
        });

        body.setVisible(expanded);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(header);
        add(body);
    }

    private void toggle() {
        expanded = !expanded;
        arrow.setText(expanded ? "▼" : "▶");
        body.setVisible(expanded);
        revalidate();
        repaint();
    }

    /** Cập nhật tiêu đề (vd "Thành viên — 5"). */
    public void setTitle(String title) {
        titleLabel.setText(title.toUpperCase());
    }
}
