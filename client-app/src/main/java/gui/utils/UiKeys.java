package gui.utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Tiện ích phím tắt cho UX: nhấn Enter để "đồng ý" thay vì phải bấm nút.
 */
public final class UiKeys {

    private UiKeys() {}

    /**
     * Nhấn Enter ở bất kỳ đâu trong dialog → chạy {@code action}.
     * Component đang focus nếu tự xử lý Enter (vd JButton) sẽ được ưu tiên trước.
     */
    public static void onEnter(RootPaneContainer dialog, Runnable action) {
        JRootPane rp = dialog.getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ui-enter-submit");
        rp.getActionMap().put("ui-enter-submit", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    /** Nhấn Enter trong 1 text field → chạy {@code action}. */
    public static void onEnter(JTextField field, Runnable action) {
        field.addActionListener(e -> action.run());
    }
}
