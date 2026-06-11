package gui.notification;

import gui.components.AvatarBadge;
import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class CustomToastNotification extends JWindow {
    private static final int TOAST_WIDTH = 340;
    private static final int TOAST_HEIGHT = 100;
    private static final int DISPLAY_TIME_MS = 5000;
    
    private Timer fadeInTimer;
    private Timer fadeOutTimer;
    private Timer holdTimer;
    private float opacity = 0f;

    public CustomToastNotification(String titleStr, String messageStr, String avatarText, String avatarUrl, Runnable onClick) {
        setSize(TOAST_WIDTH, TOAST_HEIGHT);
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0)); // Trong suốt hoàn toàn để vẽ viền bo tròn

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppColors.BG_SECONDARY != null ? AppColors.BG_SECONDARY : new Color(43, 45, 49));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                
                // Viền nhẹ
                g2.setColor(AppColors.BG_TERTIARY != null ? AppColors.BG_TERTIARY : new Color(30, 31, 34));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // -- Header: "Chat Server" + Nút Close
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel appNameLabel = new JLabel("Chat Server");
        appNameLabel.setFont(AppFonts.CAPTION_BOLD);
        appNameLabel.setForeground(AppColors.TEXT_MUTED);
        
        JLabel closeLabel = new JLabel("×");
        closeLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        closeLabel.setForeground(AppColors.TEXT_MUTED);
        closeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeLabel.setForeground(AppColors.DANGER); }
            @Override public void mouseExited(MouseEvent e)  { closeLabel.setForeground(AppColors.TEXT_MUTED); }
            @Override public void mouseClicked(MouseEvent e) { closeToast(); }
        });
        
        headerPanel.add(appNameLabel, BorderLayout.WEST);
        headerPanel.add(closeLabel, BorderLayout.EAST);

        // -- Body: Avatar + Text
        JPanel bodyPanel = new JPanel(new BorderLayout(12, 0));
        bodyPanel.setOpaque(false);
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        
        // Cố gắng trích xuất tên gửi từ avatarText nếu có, không thì từ title
        String senderName = avatarText != null ? avatarText : titleStr.replace("Tin nhắn từ ", "");
        AvatarBadge avatar = new AvatarBadge(senderName, 40);
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            String fullAvatarUrl = avatarUrl;
            if (!fullAvatarUrl.startsWith("http")) fullAvatarUrl = network.ApiConfig.GATEWAY_HTTP + fullAvatarUrl;
            avatar.loadAvatarFromUrl(fullAvatarUrl);
        }
        
        JPanel avatarWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        avatarWrap.setOpaque(false);
        avatarWrap.add(avatar);
        bodyPanel.add(avatarWrap, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(titleStr);
        titleLabel.setFont(AppFonts.BODY_BOLD);
        titleLabel.setForeground(AppColors.TEXT_HEADER);
        
        JLabel msgLabel = new JLabel("<html><div style='width: " + (TOAST_WIDTH - 100) + "px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'>" 
                                    + messageStr + "</div></html>");
        msgLabel.setFont(AppFonts.BODY);
        msgLabel.setForeground(AppColors.TEXT_NORMAL);
        
        textPanel.add(titleLabel);
        textPanel.add(msgLabel);
        bodyPanel.add(textPanel, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(bodyPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // Sự kiện nhấp chuột vào body mở app
        MouseAdapter clickAdapter = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (onClick != null) onClick.run();
                closeToast();
            }
            @Override public void mouseEntered(MouseEvent e) {
                mainPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            @Override public void mouseExited(MouseEvent e) {
                mainPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        };
        bodyPanel.addMouseListener(clickAdapter);
        textPanel.addMouseListener(clickAdapter);
        titleLabel.addMouseListener(clickAdapter);
        msgLabel.addMouseListener(clickAdapter);

        // Tính toán vị trí góc dưới bên phải màn hình
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle screenRect = ge.getMaximumWindowBounds();
        int x = screenRect.width - TOAST_WIDTH - 15;
        int y = screenRect.height - TOAST_HEIGHT - 15;
        setLocation(x, y);
        
        setOpacity(0f);

        // Animations
        fadeInTimer = new Timer(20, e -> {
            opacity += 0.05f;
            if (opacity >= 1.0f) {
                opacity = 1.0f;
                setOpacity(opacity);
                fadeInTimer.stop();
                holdTimer.start();
            } else {
                setOpacity(opacity);
            }
        });

        holdTimer = new Timer(DISPLAY_TIME_MS, e -> {
            holdTimer.stop();
            fadeOutTimer.start();
        });
        holdTimer.setRepeats(false);

        fadeOutTimer = new Timer(20, e -> {
            opacity -= 0.05f;
            if (opacity <= 0f) {
                opacity = 0f;
                setOpacity(opacity);
                fadeOutTimer.stop();
                dispose();
            } else {
                setOpacity(opacity);
            }
        });
    }

    public void showToast() {
        setVisible(true);
        fadeInTimer.start();
    }

    private void closeToast() {
        fadeInTimer.stop();
        holdTimer.stop();
        fadeOutTimer.start();
    }

    /** 
     * Khởi tạo và hiển thị một Custom Toast Notification 
     */
    public static void show(String title, String message, String avatarText, String avatarUrl, Runnable onClick) {
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            String fullAvatarUrl = avatarUrl;
            if (!fullAvatarUrl.startsWith("http")) fullAvatarUrl = network.ApiConfig.GATEWAY_HTTP + fullAvatarUrl;
            
            gui.utils.ImageCache.loadAsync(fullAvatarUrl, 40, img -> {
                SwingUtilities.invokeLater(() -> {
                    CustomToastNotification toast = new CustomToastNotification(title, message, avatarText, avatarUrl, onClick);
                    toast.showToast();
                });
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                CustomToastNotification toast = new CustomToastNotification(title, message, avatarText, avatarUrl, onClick);
                toast.showToast();
            });
        }
    }
}
