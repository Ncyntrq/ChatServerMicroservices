package gui.profile;

import gui.components.PresenceStatusIcon;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import gui.theme.AppFonts;
import network.PresenceApiClient;
import network.UserProfileApiClient;

import javax.swing.*;
import java.awt.*;

/**
 * Panel đổi trạng thái trong cài đặt người dùng.
 * Gọi đồng thời:
 *  1. PresenceApiClient (PUT /api/presence/status) → thay đổi real-time qua WebSocket.
 *  2. UserProfileApiClient (PUT /api/users/status) → lưu custom text vào profile DB.
 */
public class StatusPanel extends JPanel {

    private static final PresenceStatusIcon.Status[] STATUSES = {
        PresenceStatusIcon.Status.ONLINE,
        PresenceStatusIcon.Status.IDLE,
        PresenceStatusIcon.Status.AWAY,
        PresenceStatusIcon.Status.DO_NOT_DISTURB,
        PresenceStatusIcon.Status.INVISIBLE,
    };

    public StatusPanel(UserProfileApiClient profileApi) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AppColors.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // ── Title ──────────────────────────────────────────────────────
        JLabel title = new JLabel("Trạng thái hoạt động");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel("Chọn trạng thái hiển thị cho người khác");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(subtitle);
        add(Box.createVerticalStrut(24));

        // ── Status Radio Cards ─────────────────────────────────────────
        ButtonGroup group = new ButtonGroup();
        JRadioButton[] radios = new JRadioButton[STATUSES.length];

        for (int i = 0; i < STATUSES.length; i++) {
            PresenceStatusIcon.Status st = STATUSES[i];
            JPanel card = buildStatusCard(st, group, radios, i);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(card);
            add(Box.createVerticalStrut(8));
        }
        radios[0].setSelected(true); // Default: ONLINE

        add(Box.createVerticalStrut(20));

        // ── Custom Status Text ─────────────────────────────────────────
        JLabel customLabel = new JLabel("TRẠNG THÁI TÙY CHỈNH");
        customLabel.setFont(AppFonts.CAPTION_BOLD);
        customLabel.setForeground(AppColors.TEXT_MUTED);
        customLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(customLabel);
        add(Box.createVerticalStrut(6));

        JTextField customField = new JTextField();
        customField.setBackground(AppColors.BG_TERTIARY);
        customField.setForeground(AppColors.TEXT_NORMAL);
        customField.setCaretColor(AppColors.TEXT_WHITE);
        customField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        customField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BG_HOVER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        customField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        customField.setAlignmentX(Component.LEFT_ALIGNMENT);
        customField.setToolTipText("Hiện thị dòng trạng thái bên cạnh tên bạn");
        add(customField);
        add(Box.createVerticalStrut(20));

        // ── Feedback label ─────────────────────────────────────────────
        JLabel feedbackLabel = new JLabel(" ");
        feedbackLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        feedbackLabel.setForeground(AppColors.TEXT_MUTED);
        feedbackLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Update Button ──────────────────────────────────────────────
        PresenceApiClient presenceApi = new PresenceApiClient();

        PrimaryButton updateBtn = new PrimaryButton("Cập nhật trạng thái", e -> {
            String selectedStatus = group.getSelection().getActionCommand();
            String customText = customField.getText().trim();

            feedbackLabel.setForeground(AppColors.TEXT_MUTED);
            feedbackLabel.setText("Đang cập nhật...");

            new SwingWorker<Void, Void>() {
                String err = null;
                @Override
                protected Void doInBackground() {
                    try {
                        // 1. Cập nhật presence real-time (WebSocket broadcast)
                        presenceApi.updatePresenceStatus(selectedStatus);
                        // 2. Lưu custom text vào profile DB (hiển thị dưới tên)
                        if (!customText.isEmpty()) {
                            profileApi.updateStatus(customText);
                        }
                    } catch (Exception ex) {
                        err = ex.getMessage();
                    }
                    return null;
                }
                @Override
                protected void done() {
                    if (err != null) {
                        feedbackLabel.setForeground(AppColors.DANGER);
                        feedbackLabel.setText("Lỗi: " + err);
                    } else {
                        feedbackLabel.setForeground(AppColors.SUCCESS);
                        String label = PresenceStatusIcon.Status.from(selectedStatus).toVietnamese();
                        feedbackLabel.setText("✓ Đã đặt trạng thái: " + label
                                + (customText.isEmpty() ? "" : " — \"" + customText + "\""));
                    }
                }
            }.execute();
        });
        updateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        gui.utils.UiKeys.onEnter(customField, updateBtn::doClick);
        add(updateBtn);
        add(Box.createVerticalStrut(10));
        add(feedbackLabel);
    }

    /** Xây card chứa radio + icon + label tiếng Việt. */
    private JPanel buildStatusCard(PresenceStatusIcon.Status status, ButtonGroup group,
                                   JRadioButton[] radios, int idx) {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        card.setBackground(AppColors.BG_FLOATING);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.SEPARATOR, 1),
                BorderFactory.createEmptyBorder(2, 4, 2, 8)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Icon trạng thái 16px
        gui.components.PresenceStatusIcon icon = new gui.components.PresenceStatusIcon(
                status, 16, AppColors.BG_FLOATING);

        JRadioButton radio = new JRadioButton(status.toVietnamese());
        radio.setActionCommand(status.name());
        radio.setFont(new Font("SansSerif", Font.PLAIN, 14));
        radio.setForeground(AppColors.TEXT_NORMAL);
        radio.setOpaque(false);
        group.add(radio);
        radios[idx] = radio;

        // Click cả card để chọn radio
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { radio.setSelected(true); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(AppColors.BG_HOVER); card.repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(AppColors.BG_FLOATING); card.repaint(); }
        });
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.add(icon);
        card.add(radio);
        return card;
    }
}
