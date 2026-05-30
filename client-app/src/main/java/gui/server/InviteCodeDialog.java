package gui.server;

import gui.components.PrimaryButton;
import gui.theme.AppColors;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Dialog hiển thị invite code đã tạo, cho phép copy vào clipboard.
 */
public class InviteCodeDialog extends JDialog {

    public InviteCodeDialog(Window owner, String inviteCode) {
        super(owner, "Mã Mời", ModalityType.APPLICATION_MODAL);
        setSize(400, 240);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JLabel title = new JLabel("Mã Mời Server");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("Chia sẻ mã này để mời người khác tham gia");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(AppColors.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(subtitle);
        root.add(Box.createVerticalStrut(16));

        JTextField codeField = new JTextField(inviteCode);
        codeField.setEditable(false);
        codeField.setBackground(AppColors.BG_TERTIARY);
        codeField.setForeground(AppColors.TEXT_WHITE);
        codeField.setFont(new Font("Monospaced", Font.BOLD, 16));
        codeField.setHorizontalAlignment(JTextField.CENTER);
        codeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BG_HOVER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        codeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        codeField.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(codeField);
        root.add(Box.createVerticalStrut(16));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(AppColors.SUCCESS);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton copyBtn = new PrimaryButton("Sao chép mã", e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(inviteCode), null);
            statusLabel.setText("Đã sao chép vào clipboard!");
        });
        copyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(copyBtn);
        root.add(Box.createVerticalStrut(8));
        root.add(statusLabel);

        setContentPane(root);
    }
}
