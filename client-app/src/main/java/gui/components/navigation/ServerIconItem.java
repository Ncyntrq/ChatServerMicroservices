package gui.components.navigation;

import gui.theme.AppColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ServerIconItem extends JPanel {
    private boolean isHovered = false;
    private boolean isActive = false;
    private boolean hasUnread = false;
    private int unreadCount = 0;
    private final JLabel iconLabel;

    private Runnable onClick;            // left-click → chọn server
    private Runnable onContextMenu;      // right-click → mở context menu (settings)

    public void setOnClick(Runnable onClick) { this.onClick = onClick; }
    public void setOnContextMenu(Runnable onContextMenu) { this.onContextMenu = onContextMenu; }

    public ServerIconItem(String iconSymbol) {
        setPreferredSize(new Dimension(72, 58));
        setMaximumSize(new Dimension(72, 58));
        setMinimumSize(new Dimension(72, 58)); // ADD THIS
        setAlignmentX(Component.CENTER_ALIGNMENT); // ADD THIS
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setLayout(new BorderLayout());

        iconLabel = new JLabel(iconSymbol);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setOpaque(false);

        add(iconLabel, BorderLayout.CENTER);

        updateStyles();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                updateStyles();
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                updateStyles();
                repaint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && onClick != null) {
                    onClick.run();
                }
            }
        });
    }

    public void setActive(boolean active) {
        this.isActive = active;
        updateStyles();
        repaint();
    }

    public void setUnreadCount(int count) {
        this.unreadCount = count;
        this.hasUnread = (count > 0);
        repaint();
    }

    private void updateStyles() {
        if (isActive || isHovered) {
            iconLabel.setForeground(AppColors.TEXT_WHITE);
        } else {
            iconLabel.setForeground(AppColors.TEXT_MUTED);
        }
    }

    private Image serverImage;
    private boolean isLoading = false;

    public void loadServerIconFromUrl(String urlString) {
        Image cachedImage = gui.utils.ImageCache.get(urlString);
        if (cachedImage != null) {
            this.serverImage = cachedImage;
            this.iconLabel.setVisible(false);
            repaint();
            return;
        }

        this.isLoading = true;
        this.iconLabel.setVisible(false); // Ẩn chữ đi để vẽ skeleton
        repaint();

        gui.utils.ImageCache.loadAsync(urlString, 40, img -> {
            this.isLoading = false;
            if (img != null) {
                this.serverImage = img;
                this.iconLabel.setVisible(false);
            } else {
                this.iconLabel.setVisible(true); // Tải lỗi thì hiện lại chữ
            }
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // --- 1. DETERMINE DYNAMIC LOOKS ---
        Color backgroundColor;
        int cornerRadius = 16;

        if (isActive || isHovered) {
            backgroundColor = AppColors.BRAND_PRIMARY;
        } else {
            backgroundColor = AppColors.BG_PRIMARY;
        }

        // --- 2. DRAW BUTTON BACKGROUND CONTAINER ---
        int size = 40;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        if (serverImage != null) {
            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(x, y, size, size, cornerRadius, cornerRadius));
            g2.drawImage(serverImage, x, y, size, size, this);
            g2.setClip(null);
            // Draw a subtle border so it blends well
            g2.setColor(new Color(255, 255, 255, 20));
            g2.drawRoundRect(x, y, size, size, cornerRadius, cornerRadius);
        } else if (isLoading) {
            // Skeleton mờ
            g2.setColor(AppColors.BG_TERTIARY != null ? AppColors.BG_TERTIARY : new Color(30, 31, 34));
            g2.fillRoundRect(x, y, size, size, cornerRadius, cornerRadius);
        } else {
            g2.setColor(backgroundColor);
            g2.fillRoundRect(x, y, size, size, cornerRadius, cornerRadius);
        }

        // --- 3. DRAW LEFT SIDE STATUS INDICATOR PILL ---
        g2.setColor(Color.WHITE);

        int pillOffset = -getX(); // negative of how far we are from the left

        if (isActive) {
            g2.fillRoundRect(pillOffset, (getHeight() - 40) / 2, 4, 40, 4, 4);
        } else if (isHovered) {
            g2.fillRoundRect(pillOffset, (getHeight() - 20) / 2, 4, 20, 4, 4);
        } else if (hasUnread) {
            g2.fillRoundRect(pillOffset, (getHeight() - 8) / 2, 4, 8, 4, 4);
        }

        // --- 4. DRAW RED UNREAD NOTIFICATION BADGE ---
        if (hasUnread) {
            String text = unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            
            int badgeWidth = Math.max(16, textWidth + 8);
            int badgeHeight = 16;
            
            int badgeX = x + size - badgeWidth / 2 - 4;
            int badgeY = y - 4;
            
            // Draw background stroke
            g2.setColor(AppColors.BG_TERTIARY);
            g2.fillRoundRect(badgeX - 2, badgeY - 2, badgeWidth + 4, badgeHeight + 4, badgeHeight + 4, badgeHeight + 4);
            
            // Draw red background
            g2.setColor(Color.decode("#F23F42"));
            g2.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, badgeHeight, badgeHeight);
            
            // Draw text
            g2.setColor(Color.WHITE);
            g2.drawString(text, badgeX + (badgeWidth - textWidth) / 2, badgeY + badgeHeight - 4);
        }

        g2.dispose();
    }
}