package tasktracker.view.theme;

import java.awt.*;
import java.awt.image.*;
import java.lang.reflect.*;
import java.util.Arrays;

/** Repeatable offscreen rendering benchmark. No database access. */
public final class CachedScenePerformanceCheck {
    public static void main(String[] args) throws Exception {

        int w = 1440, h = 900;
        Rectangle[] cards = { new Rectangle(55, 130, 565, 575), new Rectangle(650, 130, 735, 575) };
        SceneRenderer renderer = new SceneRenderer();
        CachedSceneTransition.Frame day = CachedSceneTransition.render(renderer,
                new CachedSceneTransition.Request(w, h, cards, LastLightScene.SUNRISE.weights()));
        CachedSceneTransition.Frame night = CachedSceneTransition.render(renderer,
                new CachedSceneTransition.Request(w, h, cards, LastLightScene.NIGHT.weights()));
        for (int i = 0; i < day.pixels.length; i++)
            if ((day.pixels[i] >>> 24) != (night.pixels[i] >>> 24))
                throw new AssertionError("Flower geometry changed");
        CachedSceneTransition transition = new CachedSceneTransition(() -> {
        });
        set(transition, "from", day);
        set(transition, "to", night);
        BufferedImage flower = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        set(transition, "flowerBlend", flower);
        Method blend = CachedSceneTransition.class.getDeclaredMethod("blendFlowers");
        blend.setAccessible(true);
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        long[] times = new long[60];
        for (int i = 0; i < 70; i++) {
            set(transition, "progress", (i % 60) / 59f);
            long start = System.nanoTime();
            blend.invoke(transition);
            Graphics2D g = output.createGraphics();
            transition.background(g, w, h);
            transition.foreground(g, w, h);
            g.dispose();
            if (i >= 10)
                times[i - 10] = System.nanoTime() - start;
        }
        Arrays.sort(times);
        System.out.printf("Cached scene 1440x900: median %.1f ms, p95 %.1f ms%n", times[30] / 1e6, times[57] / 1e6);
        System.out.println("PASS: stable foreground alpha; scene preparation separated from frame blending.");
        System.exit(0);
    }

    static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
