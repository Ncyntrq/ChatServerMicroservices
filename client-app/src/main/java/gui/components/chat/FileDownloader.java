package gui.components.chat;

import network.FileApiClient;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;

/**
 * Tải file đính kèm về máy (qua gateway, JWT header) kèm hộp thoại chọn nơi lưu.
 * Dùng chung cho card file trong sidebar và dialog "Xem tất cả".
 */
public final class FileDownloader {

    private FileDownloader() {}

    public static void save(java.awt.Component anchor, String url, String name) {
        if (url == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(name != null ? name : "download"));
        if (fc.showSaveDialog(anchor) != JFileChooser.APPROVE_OPTION) return;
        File dest = fc.getSelectedFile();

        new SwingWorker<Void, Void>() {
            private Exception err;
            @Override protected Void doInBackground() {
                try {
                    byte[] bytes = new FileApiClient().download(url);
                    Files.write(dest.toPath(), bytes);
                } catch (Exception e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    err = e;
                }
                return null;
            }
            @Override protected void done() {
                if (err != null) {
                    JOptionPane.showMessageDialog(anchor, "Tải file thất bại: " + err.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
