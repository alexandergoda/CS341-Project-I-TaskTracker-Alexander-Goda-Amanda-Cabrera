package tasktracker.view.presentation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Consistent team credit for startup, authentication and the task workspace. */
public final class ProjectCredit {
    /** The same attribution appears on every screen. */
    public static final String TEXT = "Created by Alexander Goda and Amanda Cabrera";
    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Color INK = new Color(232, 215, 204);
    private static final Color SHADOW = new Color(15, 20, 28, 190);

    private ProjectCredit() {
    }

    /**
     * Paints a centered footer without changing the caller's drawing state.
     *
     * @param graphics the screen's graphics context
     * @param width    the screen width
     * @param height   the screen height
     */
    public static void paint(Graphics2D graphics, int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(FONT);
        int x = (width - g.getFontMetrics().stringWidth(TEXT)) / 2;
        int baseline = height - 25;
        g.setColor(SHADOW);
        g.drawString(TEXT, x, baseline + 1);
        g.setColor(INK);
        g.drawString(TEXT, x, baseline);
        g.dispose();
    }
}
