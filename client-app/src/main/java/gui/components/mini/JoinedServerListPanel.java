package gui.components.mini;

import gui.components.chat.SidebarCategoryHeader;
import gui.theme.AppColors;
import network.ServerApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Danh sách server đã tham gia (from Server API).
 * Click 1 server → callback onServerSelected(serverId, name) để chuyển server.
 */
public class JoinedServerListPanel extends JPanel {

    private final ServerApiClient serverApi = new ServerApiClient();
    private final JPanel listPanel;
    private BiConsumer<Long, String> onServerSelected;

    public void setOnServerSelected(BiConsumer<Long, String> onServerSelected) {
        this.onServerSelected = onServerSelected;
    }

    public JoinedServerListPanel() {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_SECONDARY);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(AppColors.BG_SECONDARY);
        listPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(listPanel, BorderLayout.NORTH);
    }

    /** Tải danh sách server đã join và render. */
    public void refresh() {
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return serverApi.getMyServers();
            }

            @Override
            protected void done() {
                try {
                    render(get());
                } catch (Exception ex) {
                    render(List.of());
                }
            }
        }.execute();
    }

    private void render(List<Map<String, Object>> servers) {
        listPanel.removeAll();
        listPanel.add(new SidebarCategoryHeader("SERVER ĐÃ THAM GIA — " + servers.size()));
        listPanel.add(Box.createVerticalStrut(4));

        for (Map<String, Object> server : servers) {
            long id = asLong(server.get("id"));
            String name = str(server.get("name"));
            listPanel.add(buildRow(id, name != null ? name : "Server #" + id));
            listPanel.add(Box.createVerticalStrut(2));
        }

        if (servers.isEmpty()) {
            JLabel empty = new JLabel("Chưa tham gia server nào");
            empty.setForeground(AppColors.TEXT_MUTED);
            listPanel.add(empty);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JComponent buildRow(long id, String name) {
        JLabel row = new JLabel("#  " + name);
        row.setFont(new Font("SansSerif", Font.PLAIN, 14));
        row.setForeground(AppColors.TEXT_MUTED);
        row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                row.setForeground(AppColors.TEXT_WHITE);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                row.setForeground(AppColors.TEXT_MUTED);
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onServerSelected != null) onServerSelected.accept(id, name);
            }
        });
        return row;
    }

    private static long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o != null) {
            try { return Long.parseLong(o.toString()); } catch (NumberFormatException ignore) {}
        }
        return -1;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
