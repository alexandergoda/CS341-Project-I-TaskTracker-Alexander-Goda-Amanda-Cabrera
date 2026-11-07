package tasktracker.view.theme;

import java.awt.Color;

/** Shared appearance tokens. */
public final class ThemePalette {
    private ThemePalette() {
    }

    public static final Color INK = new Color(255, 245, 233), MUTED = new Color(232, 215, 204);

    public static Color text(boolean dark) {
        return dark ? INK : new Color(48, 34, 39);
    }
}
