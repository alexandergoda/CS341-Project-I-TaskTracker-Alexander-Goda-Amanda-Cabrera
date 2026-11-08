package tasktracker.view.theme;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Standalone regression check using classpath artwork; no database used. */
public final class ForegroundLightingCheck {
    public static void main(String[] args) throws Exception {
        checkConcurrentColdInitialization();

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
                "PASS: concurrent cold initialization and matching renderer pixels; identical source and rendered alpha "
                        + "in all scenes at three window sizes; lighting differs.");
    }

    private static void checkConcurrentColdInitialization() throws Exception {
        Field field = SceneRenderer.class.getDeclaredField("lastLightForegrounds");
        field.setAccessible(true);
        BufferedImage[] cache = (BufferedImage[]) field.get(null);
        if (cache[0] != null)
            throw new AssertionError("Cold initialization check requires a fresh JVM");
        AtomicReference<BufferedImage> firstImage = new AtomicReference<>();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        Thread first = new Thread(() -> {
            try {
                firstImage.set(renderForeground());
            } catch (Throwable failure) {
                firstFailure.set(failure);
            }
        }, "cold-foreground-renderer");
        first.setDaemon(true);
        first.start();
        long deadline = System.nanoTime() + 15_000_000_000L;
        // Coordinate the second public call with the first published image. This
        // exercises the partial-initialization window without a timing-only sleep.
        while (cache[0] == null && first.isAlive() && System.nanoTime() < deadline)
            Thread.onSpinWait();
        BufferedImage secondImage = null;
        Throwable secondFailure = null;
        try {
            secondImage = renderForeground();
        } catch (Throwable failure) {
            secondFailure = failure;
        }
        first.join(15_000);
        if (first.isAlive())
            throw new AssertionError("Cold foreground renderer did not finish");
        if (firstFailure.get() != null)
            throw new AssertionError("First cold foreground renderer failed", firstFailure.get());
        if (secondFailure != null)
            throw new AssertionError("Concurrent foreground renderer observed an incomplete cache", secondFailure);
        int[] firstPixels = firstImage.get().getRGB(0, 0, 64, 64, null, 0, 64);
        int[] secondPixels = secondImage.getRGB(0, 0, 64, 64, null, 0, 64);
        if (!Arrays.equals(firstPixels, secondPixels))
            throw new AssertionError("Concurrent renderers produced different foreground pixels");
    }

    private static BufferedImage renderForeground() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        SceneRenderState.beginRender(LastLightScene.DUSK.weights());
        try {
            new SceneRenderer().lastLightForeground(graphics, 64, 64);
            return image;
        } finally {
            SceneRenderState.endRender();
            graphics.dispose();
        }
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
