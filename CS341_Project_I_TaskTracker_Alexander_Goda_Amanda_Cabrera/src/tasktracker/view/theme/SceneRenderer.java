package tasktracker.view.theme;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.*;

/** Shared cached scenery, frosted glass, and matching foreground lighting. */
public final class SceneRenderer {
    private static final int SCENE_COUNT = LastLightScene.values().length;
    private static final BufferedImage[] sources = new BufferedImage[SCENE_COUNT];
    private final BufferedImage[] backgrounds = new BufferedImage[SCENE_COUNT], frost = new BufferedImage[SCENE_COUNT];
    private int width, height;
    private static final BufferedImage[] lastLightForegrounds = new BufferedImage[LastLightScene.values().length];
    private static BufferedImage lastLightMasterForeground;
    private static BufferedImage lastLightMasterBackground;

    private static BufferedImage read(String name) {
        return ThemeAssets.image(name);
    }

    private void prepare(int w, int h, int index) {
        if (width != w || height != h) {
            java.util.Arrays.fill(backgrounds, null);
            java.util.Arrays.fill(frost, null);
            width = w;
            height = h;
        }
        if (backgrounds[index] != null)
            return;
        if (sources[index] == null) {
            if (lastLightMasterBackground == null)
                lastLightMasterBackground = LastLightGrade.apply(read(LastLightScene.DUSK.asset("background")), false);
            LastLightScene scene = LastLightScene.forIndex(index);
            sources[index] = scene != LastLightScene.DUSK
                    ? ReferenceSceneLighting.apply(lastLightMasterBackground, read(scene.asset("background")))
                    : lastLightMasterBackground;
        }
        BufferedImage src = sources[index], bg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double s = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
        int sw = (int) Math.ceil(s * src.getWidth()), sh = (int) Math.ceil(s * src.getHeight());
        g.drawImage(src, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
        g.setColor(new Color(25, 16, 31, 40));
        g.fillRect(0, 0, w, h);
        g.dispose();
        backgrounds[index] = bg;
        BufferedImage small = new BufferedImage(Math.max(1, w / 4), Math.max(1, h / 4), BufferedImage.TYPE_INT_RGB);
        g = small.createGraphics();
        g.drawImage(bg, 0, 0, small.getWidth(), small.getHeight(), null);
        g.dispose();
        float[] k = new float[9];
        java.util.Arrays.fill(k, 1f / 9);
        frost[index] = new ConvolveOp(new Kernel(1, 9, k), ConvolveOp.EDGE_NO_OP, null)
                .filter(new ConvolveOp(new Kernel(9, 1, k), ConvolveOp.EDGE_NO_OP, null).filter(small, null), null);
    }

    private void layers(Graphics2D g, int w, int h, double[] weights, boolean blur) {
        double total = 0;
        for (int i = 0; i < weights.length; i++)
            if (weights[i] > .00001) {
                prepare(w, h, i);
                total += weights[i];
                g.setComposite(AlphaComposite.SrcOver.derive((float) (weights[i] / total)));
                g.drawImage(blur ? frost[i] : backgrounds[i], 0, 0, w, h, null);
            }
        g.setComposite(AlphaComposite.SrcOver);
    }

    public void background(Graphics2D g, int w, int h, double[] weights) {
        if (w > 0 && h > 0)
            layers(g, w, h, weights, false);
    }

    public void glass(Graphics2D g, Rectangle r, int w, int h, double[] weights, boolean dark) {
        if (r.width <= 0 || r.height <= 0)
            return;
        Graphics2D glass = (Graphics2D) g.create();
        for (int i = 12; i > 0; i--) {
            glass.setColor(new Color(20, 12, 25, 3));
            glass.fillRoundRect(r.x - i, r.y + 8 - i, r.width + 2 * i, r.height + 2 * i, 28, 28);
        }
        Shape shape = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 26, 26);
        Shape clip = glass.getClip();
        glass.clip(shape);
        // The actual scenery is blurred behind the controls, not a flat translucent fill.
        layers(glass, w, h, weights, true);
        glass.setColor(AdaptiveScenePalette.glass());
        glass.fill(shape);
        // Broad, restrained surface reflection gives the material depth without
        // adding animated glare or moving any of the text.
        glass.setPaint(new LinearGradientPaint(r.x, r.y, r.x + r.width * .65f, r.y + r.height,
                new float[] { 0, .45f, 1 }, new Color[] { AdaptiveScenePalette.reflection(),
                        new Color(255, 244, 230, 5), new Color(24, 17, 38, dark ? 10 : 4) }));
        glass.fill(shape);
        glass.setClip(clip);
        glass.setStroke(new BasicStroke(1));
        glass.setPaint(new GradientPaint(r.x, r.y, AdaptiveScenePalette.edge(), r.x + r.width * .4f, r.y + r.height,
                new Color(255, 242, 226, 35)));
        glass.draw(shape);
        glass.dispose();
    }

    /**
     * Full-width framing plate, kept below the card actions rather than stretched over them.
     *
     * @param g destination graphics
     * @param w destination width in pixels
     * @param h destination height in pixels
     */
    public void lastLightForeground(Graphics2D g, int w, int h) {
        if (lastLightForegrounds[0] == null) {
            if (lastLightMasterForeground == null)
                lastLightMasterForeground = LastLightGrade.apply(read(LastLightScene.DUSK.asset("foreground")), true);
            for (LastLightScene scene : LastLightScene.values())
                lastLightForegrounds[scene.ordinal()] = ForegroundLighting.apply(lastLightMasterForeground, scene);
        }
        BufferedImage lastLightForeground = blendedForeground();
        int plateHeight = (int) Math
                .round(w * (double) lastLightForeground.getHeight() / lastLightForeground.getWidth());
        // Keep the authored unequal crops. Extra cropping erased the smaller left bloom.
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(lastLightForeground, 0, h - plateHeight, w, plateHeight, null);
    }

    private BufferedImage foregroundBlend;
    private double[] foregroundWeights;
    private int[][] foregroundPixels;
    private int[] visibleFlowerPixels;

    /** Blend RGB only. Identical alpha masks prevent doubling the soft petal edges. */
    private BufferedImage blendedForeground() {
        double[] weights = SceneRenderState.renderWeights();
        if (foregroundBlend == null) {
            int fw = lastLightMasterForeground.getWidth(), fh = lastLightMasterForeground.getHeight();
            foregroundBlend = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);
            foregroundPixels = new int[LastLightScene.values().length][];
            for (LastLightScene scene : LastLightScene.values())
                foregroundPixels[scene.index] = lastLightForegrounds[scene.ordinal()].getRGB(0, 0, fw, fh, null, 0, fw);
            int[] master = foregroundPixels[LastLightScene.DUSK.index];
            visibleFlowerPixels = java.util.stream.IntStream.range(0, master.length).filter(i -> (master[i] >>> 24) > 0)
                    .toArray();
        }
        if (java.util.Arrays.equals(weights, foregroundWeights))
            return foregroundBlend;
        int[] output = ((DataBufferInt) foregroundBlend.getRaster().getDataBuffer()).getData();
        for (int pixel : visibleFlowerPixels) {
            double red = 0, green = 0, blue = 0;
            for (int i = 0; i < weights.length; i++)
                if (weights[i] > 0) {
                    int rgb = foregroundPixels[i][pixel];
                    double weight = weights[i];
                    red += ((rgb >>> 16) & 255) * weight;
                    green += ((rgb >>> 8) & 255) * weight;
                    blue += (rgb & 255) * weight;
                }
            output[pixel] = (foregroundPixels[0][pixel] & 0xff000000) | ((int) Math.round(red) << 16)
                    | ((int) Math.round(green) << 8) | (int) Math.round(blue);
        }
        foregroundWeights = weights.clone();
        return foregroundBlend;
    }
}
