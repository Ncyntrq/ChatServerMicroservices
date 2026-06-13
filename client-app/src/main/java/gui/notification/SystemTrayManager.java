package gui.notification;

import gui.theme.AppColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Quản lý SystemTray và hiển thị thông báo hệ điều hành.
 */
public class SystemTrayManager {
    private static SystemTrayManager instance;
    private TrayIcon trayIcon;
    private JFrame mainFrame;
    private boolean isSupported;

    private SystemTrayManager() {
        this.isSupported = SystemTray.isSupported();
    }

    public static SystemTrayManager get() {
        if (instance == null) {
            instance = new SystemTrayManager();
        }
        return instance;
    }

    /** Khởi tạo TrayIcon và gắn mainFrame để có thể click khôi phục. */
    public void init(JFrame frame) {
        this.mainFrame = frame;

        if (!isSupported || trayIcon != null) {
            return; // Đã init hoặc không hỗ trợ
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image iconImage = createTrayIconImage();

            trayIcon = new TrayIcon(iconImage, "Chat App");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Chat App - Bấm để mở");

            // Khi click vào icon ở khay hệ thống hoặc popup thông báo
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        restoreApp();
                    }
                }
            });
            trayIcon.addActionListener(e -> restoreApp()); // Cho click vào popup (tùy OS)

            // Popup menu chuột phải
            PopupMenu popup = new PopupMenu();
            MenuItem openItem = new MenuItem("Mở ứng dụng");
            openItem.addActionListener(e -> restoreApp());
            MenuItem exitItem = new MenuItem("Thoát");
            exitItem.addActionListener(e -> System.exit(0));
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            tray.add(trayIcon);

            // Gắn icon lên frame nếu chưa có
            if (mainFrame.getIconImage() == null || mainFrame.getIconImages().isEmpty()) {
                mainFrame.setIconImage(iconImage);
            }
        } catch (AWTException e) {
            System.err.println("Lỗi khởi tạo SystemTray: " + e.getMessage());
            isSupported = false;
        }
    }

    /** Tạo một logo mặc định bằng Java2D nếu app chưa có ảnh logo */
    private Image createTrayIconImage() {
        try {
            java.net.URL url = getClass().getResource("/logo/logo.png");
            if (url != null) {
                return javax.imageio.ImageIO.read(url);
            }
        } catch (Exception e) {
            System.err.println("Could not load logo.png: " + e.getMessage());
        }
        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Vẽ hình tròn nền
        g2.setColor(AppColors.BRAND_PRIMARY != null ? AppColors.BRAND_PRIMARY : new Color(0, 150, 255));
        g2.fill(new Ellipse2D.Float(2, 2, size - 4, size - 4));
        
        // Vẽ chữ "C" ở giữa
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        FontMetrics fm = g2.getFontMetrics();
        String text = "C";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, x, y);
        g2.dispose();
        return img;
    }

    /**
     * Hiển thị thông báo nếu ứng dụng bị thu nhỏ hoặc mất focus.
     */
    public void notifyNewMessage(String title, String content, String avatarText, String avatarUrl, Runnable onClick) {
        if (!isSupported || trayIcon == null || mainFrame == null) return;

        // Chỉ báo khi app không focus hoặc đang bị minimize
        if (!mainFrame.isFocused() || mainFrame.getState() == Frame.ICONIFIED || !mainFrame.isVisible()) {
            CustomToastNotification.show(title, content, avatarText, avatarUrl, onClick);
        }
    }

    public void restoreApp() {
        if (mainFrame != null) {
            SwingUtilities.invokeLater(() -> {
                mainFrame.setVisible(true);
                mainFrame.setExtendedState(Frame.NORMAL);
                mainFrame.toFront();
                mainFrame.requestFocus();
            });
        }
    }
}
