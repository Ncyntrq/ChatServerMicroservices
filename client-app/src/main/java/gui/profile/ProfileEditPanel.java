package gui.profile;

import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.ApiException;
import network.UserProfileApiClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.net.URL;
import java.util.Map;

public class ProfileEditPanel extends JPanel {

    private final FormField displayNameField;
    private final JTextArea bioArea;
    private final JLabel statusLabel;
    private final Runnable onProfileChanged;
    
    private Image currentAvatar;
    private JPanel avatarHolder;
    private boolean isHoveringAvatar = false;

    public ProfileEditPanel(String username, UserProfileApiClient profileApi, Runnable onProfileChanged) {
        this.onProfileChanged = onProfileChanged;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AppColors.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppFonts.BODY_SM);
        statusLabel.setForeground(AppColors.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Avatar Holder ---
        avatarHolder = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 100;
                int x = 0; // Align left
                int y = (getHeight() - size) / 2;

                if (currentAvatar != null) {
                    g2.setClip(new Ellipse2D.Float(x, y, size, size));
                    g2.drawImage(currentAvatar, x, y, size, size, this);
                    g2.setClip(null);
                } else {
                    g2.setColor(AppColors.BRAND_PRIMARY);
                    g2.fillOval(x, y, size, size);
                    g2.setColor(Color.WHITE);
                    g2.setFont(AppFonts.HEADING_LG.deriveFont(40f));
                    FontMetrics fm = g2.getFontMetrics();
                    String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
                    g2.drawString(initial, x + (size - fm.stringWidth(initial)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
                }

                if (isHoveringAvatar) {
                    g2.setColor(new Color(0, 0, 0, 160));
                    g2.fillOval(x, y, size, size);
                    g2.setColor(Color.WHITE);
                    g2.setFont(AppFonts.BODY_BOLD);
                    String edit = "EDIT";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(edit, x + (size - fm.stringWidth(edit)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
                }

                g2.dispose();
            }
        };
        avatarHolder.setPreferredSize(new Dimension(100, 100));
        avatarHolder.setMaximumSize(new Dimension(100, 100));
        avatarHolder.setOpaque(false);
        avatarHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        avatarHolder.setCursor(new Cursor(Cursor.HAND_CURSOR));

        avatarHolder.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHoveringAvatar = true;
                avatarHolder.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHoveringAvatar = false;
                avatarHolder.repaint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images (JPEG, PNG)", "jpg", "jpeg", "png"));
                if (fc.showOpenDialog(ProfileEditPanel.this) == JFileChooser.APPROVE_OPTION) {
                    java.io.File selected = fc.getSelectedFile();
                    statusLabel.setForeground(AppColors.TEXT_MUTED);
                    statusLabel.setText("Uploading avatar...");

                    new SwingWorker<Map<String, Object>, Void>() {
                        @Override
                        protected Map<String, Object> doInBackground() throws Exception {
                            profileApi.uploadAvatar(selected);
                            return profileApi.getProfile(username);
                        }
                        @Override
                        protected void done() {
                            try {
                                get();
                                statusLabel.setForeground(AppColors.SUCCESS);
                                statusLabel.setText("Avatar updated successfully!");
                                if (onProfileChanged != null) onProfileChanged.run();
                                Image img = ImageIO.read(selected);
                                currentAvatar = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                                avatarHolder.repaint();
                            } catch (Exception ex) {
                                statusLabel.setForeground(AppColors.DANGER);
                                statusLabel.setText("Error: " + ex.getMessage());
                            }
                        }
                    }.execute();
                }
            }
        });

        add(avatarHolder);
        add(Box.createVerticalStrut(30));

        // --- Display Name ---
        displayNameField = new FormField("DISPLAY NAME", "Enter display name", false);
        displayNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(displayNameField);
        add(Box.createVerticalStrut(20));

        // --- Bio ---
        JLabel bioLabel = new JLabel("ABOUT ME");
        bioLabel.setFont(AppFonts.CAPTION_BOLD);
        bioLabel.setForeground(AppColors.TEXT_MUTED);
        bioLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(bioLabel);
        add(Box.createVerticalStrut(8));

        bioArea = new JTextArea(4, 30);
        bioArea.setBackground(AppColors.BG_TERTIARY);
        bioArea.setForeground(AppColors.TEXT_NORMAL);
        bioArea.setCaretColor(AppColors.TEXT_WHITE);
        bioArea.setFont(AppFonts.BODY);
        bioArea.setLineWrap(true);
        bioArea.setWrapStyleWord(true);
        bioArea.setBorder(gui.theme.AppBorders.rounded(AppColors.SEPARATOR, 8, 8, 12));
        
        bioArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                bioArea.setBorder(gui.theme.AppBorders.rounded(AppColors.BRAND_PRIMARY, 8, 8, 12));
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                bioArea.setBorder(gui.theme.AppBorders.rounded(AppColors.SEPARATOR, 8, 8, 12));
            }
        });

        JScrollPane bioScroll = new JScrollPane(bioArea);
        bioScroll.setBorder(BorderFactory.createEmptyBorder());
        bioScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        bioScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        add(bioScroll);
        add(Box.createVerticalStrut(24));

        // --- Save Button ---
        PrimaryButton saveBtn = new PrimaryButton("Save Changes", e -> {
            String dn = displayNameField.getText().trim();
            String bio = bioArea.getText().trim();
            statusLabel.setForeground(AppColors.TEXT_MUTED);
            statusLabel.setText("Saving...");

            new SwingWorker<Map<String, Object>, Void>() {
                @Override
                protected Map<String, Object> doInBackground() throws Exception {
                    return profileApi.updateProfile(dn.isEmpty() ? null : dn, bio.isEmpty() ? null : bio);
                }
                @Override
                protected void done() {
                    try {
                        get();
                        statusLabel.setForeground(AppColors.SUCCESS);
                        statusLabel.setText("Profile saved successfully!");
                        if (onProfileChanged != null) onProfileChanged.run();
                    } catch (Exception ex) {
                        statusLabel.setForeground(AppColors.DANGER);
                        statusLabel.setText("Error: " + ex.getMessage());
                    }
                }
            }.execute();
        });
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(saveBtn);
        add(Box.createVerticalStrut(10));
        add(statusLabel);

        displayNameField.onEnter(saveBtn::doClick);

        loadProfile(username, profileApi);
    }

    private void loadProfile(String username, UserProfileApiClient profileApi) {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                return profileApi.getProfile(username);
            }
            @Override
            protected void done() {
                try {
                    Map<String, Object> profile = get();
                    Object dn = profile.get("displayName");
                    Object bio = profile.get("bio");
                    Object avUrl = profile.get("avatarUrl");
                    
                    if (dn != null) displayNameField.setText(dn.toString());
                    if (bio != null) bioArea.setText(bio.toString());
                    
                    if (avUrl != null && !avUrl.toString().isEmpty()) {
                        new SwingWorker<Image, Void>() {
                            @Override
                            protected Image doInBackground() throws Exception {
                                String fullUrl = "http://localhost:8080" + avUrl.toString();
                                return ImageIO.read(new URL(fullUrl));
                            }
                            @Override
                            protected void done() {
                                try {
                                    Image img = get();
                                    currentAvatar = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                                    avatarHolder.repaint();
                                } catch (Exception ignored) {}
                            }
                        }.execute();
                    }
                } catch (Exception ex) {
                    statusLabel.setForeground(AppColors.WARNING);
                    statusLabel.setText("Could not load profile.");
                }
            }
        }.execute();
    }
}
