package gui.components.friends;

import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import gui.theme.AppFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class AddFriendDialog extends JDialog {

    public AddFriendDialog(Window owner, java.util.function.Consumer<String> onSendRequest) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(440, 280);
        setLocationRelativeTo(owner);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerPanel.setOpaque(false);
        JLabel title = new JLabel("Thêm Bạn");
        title.setFont(AppFonts.HEADING_LG);
        title.setForeground(AppColors.TEXT_HEADER);
        headerPanel.add(title);
        
        // Close Button
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        closeBtn.setForeground(AppColors.TEXT_MUTED);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeBtn.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeBtn.setForeground(AppColors.TEXT_MUTED);
            }
        });
        
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.add(headerPanel, BorderLayout.CENTER);
        topRow.add(closeBtn, BorderLayout.EAST);
        
        JLabel desc = new JLabel("Bạn có thể thêm bạn bằng Username.");
        desc.setFont(AppFonts.BODY);
        desc.setForeground(AppColors.TEXT_MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        FormField usernameField = new FormField("USERNAME", "Nhập username kết bạn", false);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(AppFonts.BODY_SM);
        statusLabel.setForeground(AppColors.DANGER);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton sendBtn = new PrimaryButton("Gửi Yêu Cầu Kết Bạn", e -> {
            String uname = usernameField.getText().trim();
            if (uname.isEmpty()) {
                statusLabel.setText("Vui lòng nhập username.");
                return;
            }
            dispose();
            if (onSendRequest != null) {
                onSendRequest.accept(uname);
            }
        });
        sendBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField.onEnter(sendBtn::doClick);

        root.add(topRow);
        root.add(Box.createVerticalStrut(10));
        root.add(desc);
        root.add(Box.createVerticalStrut(24));
        root.add(usernameField);
        root.add(Box.createVerticalStrut(4));
        root.add(statusLabel);
        root.add(Box.createVerticalStrut(12));
        root.add(sendBtn);
        root.add(Box.createVerticalGlue());

        setContentPane(root);
        
        SwingUtilities.invokeLater(() -> usernameField.getField().requestFocusInWindow());
    }
}
