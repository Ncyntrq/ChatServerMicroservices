package gui.components.dialogs;

import gui.components.chat.ChannelAttachment;
import gui.components.chat.FileListPanel;
import gui.theme.AppColors;
import gui.theme.ThinScrollBarUI;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Dialog "Xem tất cả" file của channel — danh sách card, click để tải xuống. */
public final class FileListDialog {

    private FileListDialog() {}

    public static void open(Component anchor, List<ChannelAttachment> files) {
        Window owner = anchor != null ? SwingUtilities.getWindowAncestor(anchor) : null;
        JDialog dlg = new JDialog(owner, "File — " + files.size(), Dialog.ModalityType.MODELESS);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(AppColors.BG_SECONDARY);
        list.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (ChannelAttachment att : files) {
            list.add(FileListPanel.fileCard(att));
            list.add(Box.createVerticalStrut(8));
        }

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(AppColors.BG_SECONDARY);
        wrap.add(list, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ThinScrollBarUI.apply(scroll);

        dlg.setContentPane(scroll);
        dlg.setSize(420, 540);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
