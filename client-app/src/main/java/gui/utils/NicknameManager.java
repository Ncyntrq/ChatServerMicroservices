package gui.utils;

import java.util.prefs.Preferences;

public class NicknameManager {
    private static final Preferences prefs = Preferences.userRoot().node("ChatAppNicknames");

    public static void setNickname(String username, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            prefs.remove(username);
        } else {
            prefs.put(username, nickname.trim());
        }
    }

    public static String getNickname(String username) {
        return prefs.get(username, null);
    }
}
