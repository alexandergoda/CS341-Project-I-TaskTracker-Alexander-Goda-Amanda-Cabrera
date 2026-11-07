package tasktracker.view.theme;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Standalone regression check using classpath artwork; no database used. */
public final class ForegroundLightingCheck {
    public static void main(String[] args) throws Exception {

        BufferedImage master = LastLightGrade.apply(ThemeAssets.image("scenes/foreground-dusk"), true);
        for (LastLightScene scene : LastLightScene.values()) {
            BufferedImage lit = ForegroundLighting.apply(master, scene);
            sameAlpha(master, lit);
            if (scene != LastLightScene.DUSK && !differentColor(master, lit))
                throw new AssertionError("Lighting did not change: " + scene);
        }
        for (int[] size : new int[][] { { 1100, 700 }, { 1440, 810 }, { 1858, 1237 } }) {
            BufferedImage reference = null;
            SceneRenderer renderer = new SceneRenderer();
            for (LastLightScene scene : LastLightScene.values()) {
                SceneTheme.selectedScene = scene;
                BufferedImage rendered = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = rendered.createGraphics();
                renderer.lastLightForeground(g, size[0], size[1]);
                g.dispose();
                if (reference == null)
                    reference = rendered;
                else
                    sameAlpha(reference, rendered);
            }
        }
        SceneTheme.selectedScene = LastLightScene.DUSK;
        System.out.println(
                "PASS: identical source and rendered alpha in all scenes at three window sizes; lighting differs.");
    }

    private static void sameAlpha(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight())
            throw new AssertionError("Canvas changed");
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++)
                if ((a.getRGB(x, y) >>> 24) != (b.getRGB(x, y) >>> 24))
                    throw new AssertionError("Flower moved at " + x + "," + y);
    }

    private static boolean differentColor(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++)
                if ((a.getRGB(x, y) >>> 24) > 128 && a.getRGB(x, y) != b.getRGB(x, y))
                    return true;
        return false;
    }
}
