package gui.theme;

import javax.swing.border.Border;
import java.awt.*;

public class AppBorders {
    public static Border rounded(Color color, int radius, int paddingY, int paddingX) {
        return new Border() {
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(paddingY, paddingX, paddingY, paddingX);
            }

            @Override
            public boolean isBorderOpaque() {
                return false;
            }

            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
                g2.dispose();
            }
        };
    }
}
