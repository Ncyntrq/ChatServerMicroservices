package gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import gui.landing.LandingFrame;
import network.SessionManager;

import javax.swing.*;

public class ClientApplication {
    public static void main(String[] args) {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new java.awt.Font(FlatRobotoFont.FAMILY, java.awt.Font.PLAIN, 14));
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Couldn't load Laf: " + e);
        }

        SwingUtilities.invokeLater(() -> {
            // Có session hợp lệ → vào thẳng chat; chưa có → hiển thị landing.
            if (SessionManager.get().isAuthenticated()) {
                ChatClientGUI gui = new ChatClientGUI(SessionManager.get().getUsername());
                gui.setVisible(true);
                gui.startSession();
            } else {
                new LandingFrame().setVisible(true);
            }
        });
    }
}
