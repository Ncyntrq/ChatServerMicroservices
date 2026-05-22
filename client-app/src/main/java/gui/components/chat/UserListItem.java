package gui.components.chat;

import gui.components.AvatarBadge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UserListItem extends JPanel {
    private final Color statusColor;
    private final AvatarBadge avatar;

    public UserListItem(String username, String customStatus, Color statusColor) {
        this.statusColor = statusColor;

        setLayout(new BorderLayout(10, 0));
        setBackground(Color.decode("#2B2D31"));
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 10));

        // --- 1. HOVER EFFECT ---
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(Color.decode("#35373C"));
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(Color.decode("#2B2D31"));
            }
        });

        // --- 2. AVATAR ---
        String initial = username.substring(0, 1).toUpperCase();
        avatar = new AvatarBadge(initial);

        JPanel avatarWrapper = new JPanel(new BorderLayout());
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatar, BorderLayout.NORTH);

        // --- 3. TEXT AREA ---
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(username);
        nameLabel.setForeground(Color.decode("#DBDEE1"));
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        textPanel.add(nameLabel);

        // Custom status
        if (customStatus != null && !customStatus.isEmpty()) {
            JLabel statusLabel = new JLabel(customStatus);
            statusLabel.setForeground(Color.decode("#80848E"));
            statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            textPanel.add(statusLabel);
        }

        add(avatarWrapper, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    // --- 4. STATUS DOT ---
    @Override
    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Point p = SwingUtilities.convertPoint(avatar.getParent(), avatar.getLocation(), this);

        int x = p.x + 27;
        int y = p.y + 27;

        g2.setColor(getBackground());
        g2.fillOval(x, y, 14, 14);

        g2.setColor(statusColor);
        g2.fillOval(x + 2, y + 2, 10, 10);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
