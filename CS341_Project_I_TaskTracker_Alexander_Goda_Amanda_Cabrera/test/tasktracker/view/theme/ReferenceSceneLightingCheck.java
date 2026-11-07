package tasktracker.view.theme;

import java.awt.image.BufferedImage;

/** Checks the actual reference-lighting path without opening task storage. */
public final class ReferenceSceneLightingCheck {
    public static void main(String[] args) throws Exception {
        BufferedImage master = ThemeAssets.image("scenes/background-dusk");
        for (LastLightScene scene : new LastLightScene[] { LastLightScene.DAWN, LastLightScene.SUNRISE,
                LastLightScene.NIGHT, LastLightScene.GOLDEN_HOUR, LastLightScene.MIDDAY }) {
            BufferedImage reference = ThemeAssets.image(scene.asset("background"));
            int[] before = master.getRGB(0, 0, master.getWidth(), master.getHeight(), null, 0, master.getWidth());
            BufferedImage result = ReferenceSceneLighting.apply(master, reference);
            if (result.getWidth() != master.getWidth() || result.getHeight() != master.getHeight())
                throw new AssertionError("Canvas changed");
            for (int y = 0; y < master.getHeight(); y++) {
                for (int x = 0; x < master.getWidth(); x++) {
                    if (master.getRGB(x, y) != before[y * master.getWidth() + x])
                        throw new AssertionError("Master modified");
                    if ((result.getRGB(x, y) >>> 24) != (master.getRGB(x, y) >>> 24))
                        throw new AssertionError("Alpha changed");
                    if (y < master.getHeight() * .63
                            && (result.getRGB(x, y) & 0xffffff) != (reference.getRGB(x, y) & 0xffffff))
                        throw new AssertionError("Authored sky lost");
                }
            }
            System.out.println("PASS: " + scene + " authored sky retained; canvas, alpha, master unchanged.");
        }
    }
}
