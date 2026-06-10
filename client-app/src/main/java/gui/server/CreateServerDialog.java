package gui.server;

import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.ApiException;
import network.ServerApiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;

public class CreateServerDialog extends JDialog {

    private String uploadedIconUrl;
    private Image previewImage;

    public CreateServerDialog(Window owner, Runnable onSuccess) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(480, 560);
        setLocationRelativeTo(owner);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));

        ServerApiClient serverApi = new ServerApiClient();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Close Button
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        closeBtn.setForeground(AppColors.TEXT_MUTED);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { closeBtn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent evt) { closeBtn.setForeground(AppColors.TEXT_MUTED); }
        });
        
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRow.setOpaque(false);
        topRow.add(closeBtn);

        JLabel title = new JLabel("Customize your server");
        title.setFont(AppFonts.HEADING_LG.deriveFont(24f));
        title.setForeground(AppColors.TEXT_HEADER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitle = new JLabel("<html><div style='text-align: center; color: #8B92A0; width: 320px;'>" 
                + "Give your new server a personality with a name and an icon. You can always change it later.</div></html>");
        subtitle.setFont(AppFonts.BODY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(AppFonts.BODY_SM);
        statusLabel.setForeground(AppColors.DANGER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Circular Uploader
        JPanel uploaderPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 76;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                
                if (previewImage != null) {
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(x, y, size, size));
                    g2.drawImage(previewImage, x, y, size, size, this);
                    g2.setClip(null);
                } else {
                    g2.setColor(AppColors.BG_TERTIARY);
                    g2.fillOval(x, y, size, size);
                    g2.setColor(AppColors.TEXT_MUTED);
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6}, 0));
                    g2.drawOval(x, y, size, size);
                    
                    g2.setFont(AppFonts.EMOJI.deriveFont(24f));
                    FontMetrics fm = g2.getFontMetrics();
                    String icon = "📷";
                    g2.drawString(icon, x + (size - fm.stringWidth(icon)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
                }
                
                // Plus badge
                g2.setColor(AppColors.BRAND_PRIMARY);
                g2.fillOval(x + size - 24, y, 24, 24);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                g2.drawString("+", x + size - 18, y + 18);
                
                g2.dispose();
            }
        };
        uploaderPanel.setPreferredSize(new Dimension(100, 100));
        uploaderPanel.setMinimumSize(new Dimension(100, 100));
        uploaderPanel.setMaximumSize(new Dimension(100, 100));
        uploaderPanel.setOpaque(false);
        uploaderPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        uploaderPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        uploaderPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images (JPEG, PNG)", "jpg", "jpeg", "png"));
                if (fc.showOpenDialog(CreateServerDialog.this) == JFileChooser.APPROVE_OPTION) {
                    java.io.File file = fc.getSelectedFile();
                    statusLabel.setForeground(AppColors.TEXT_MUTED);
                    statusLabel.setText("Uploading icon...");
                    new SwingWorker<String, Void>() {
                        @Override protected String doInBackground() {
                            return new network.FileApiClient().uploadAvatar(file);
                        }
                        @Override protected void done() {
                            try {
                                uploadedIconUrl = get();
                                Image img = javax.imageio.ImageIO.read(file);
                                previewImage = img.getScaledInstance(76, 76, Image.SCALE_SMOOTH);
                                uploaderPanel.repaint();
                                statusLabel.setText(" ");
                            } catch(Exception ex) {
                                statusLabel.setForeground(AppColors.DANGER);
                                statusLabel.setText("Upload error: " + ex.getMessage());
                            }
                        }
                    }.execute();
                }
            }
        });

        FormField nameField = new FormField("SERVER NAME", "Enter server name", false);
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel descLabel = new JLabel("DESCRIPTION (OPTIONAL)");
        descLabel.setFont(AppFonts.CAPTION_BOLD);
        descLabel.setForeground(AppColors.TEXT_MUTED);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel descRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        descRow.setOpaque(false);
        descRow.add(descLabel);

        JTextArea descArea = new JTextArea(3, 30);
        descArea.setBackground(AppColors.BG_TERTIARY);
        descArea.setForeground(AppColors.TEXT_NORMAL);
        descArea.setCaretColor(AppColors.TEXT_WHITE);
        descArea.setFont(AppFonts.BODY);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(gui.theme.AppBorders.rounded(AppColors.SEPARATOR, 8, 8, 12));
        
        // Focus glow for JTextArea
        descArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                descArea.setBorder(gui.theme.AppBorders.rounded(AppColors.BRAND_PRIMARY, 8, 8, 12));
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                descArea.setBorder(gui.theme.AppBorders.rounded(AppColors.SEPARATOR, 8, 8, 12));
            }
        });

        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(BorderFactory.createEmptyBorder());
        descScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        PrimaryButton createBtn = new PrimaryButton("Create Server", e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                statusLabel.setForeground(AppColors.DANGER);
                statusLabel.setText("Please enter a server name.");
                return;
            }
            String desc = descArea.getText().trim();
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Creating...");

            new SwingWorker<Map<String, Object>, Void>() {
                @Override
                protected Map<String, Object> doInBackground() {
                    return serverApi.createServer(name, desc.isEmpty() ? null : desc, uploadedIconUrl);
                }
                @Override
                protected void done() {
                    try {
                        get();
                        if (onSuccess != null) onSuccess.run();
                        dispose();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() instanceof ApiException ? ex.getCause() : ex;
                        statusLabel.setForeground(AppColors.DANGER);
                        statusLabel.setText("Error: " + cause.getMessage());
                    }
                }
            }.execute();
        });
        createBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        root.add(topRow);
        root.add(Box.createVerticalStrut(4));
        root.add(title);
        root.add(Box.createVerticalStrut(8));
        root.add(subtitle);
        root.add(Box.createVerticalStrut(12));
        root.add(uploaderPanel);
        root.add(Box.createVerticalStrut(4));
        root.add(statusLabel);
        root.add(Box.createVerticalStrut(4));
        root.add(nameField);
        root.add(Box.createVerticalStrut(16));
        root.add(descRow);
        root.add(Box.createVerticalStrut(8));
        root.add(descScroll);
        root.add(Box.createVerticalStrut(32));
        root.add(createBtn);
        root.add(Box.createVerticalGlue());

        JScrollPane mainScroll = new JScrollPane(root);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        gui.theme.ThinScrollBarUI.apply(mainScroll);

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(AppColors.BG_PRIMARY);
        container.add(mainScroll, BorderLayout.CENTER);

        setContentPane(container);
    }
}
