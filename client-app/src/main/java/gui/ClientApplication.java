package gui;

import com.formdev.flatlaf.FlatDarkLaf;
import gui.auth.AuthDialog;

import javax.swing.*;

public class ClientApplication {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Couldn't load Laf: " + e);
        }

        SwingUtilities.invokeLater(() -> {
            AuthDialog auth = new AuthDialog();
            auth.setVisible(true);
        });
    }
}
