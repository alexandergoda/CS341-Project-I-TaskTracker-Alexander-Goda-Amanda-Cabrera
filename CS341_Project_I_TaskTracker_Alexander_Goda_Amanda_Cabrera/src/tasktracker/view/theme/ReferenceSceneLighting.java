package tasktracker.view.theme;

import java.awt.image.BufferedImage;

/**
 * Restores authored skies and regional light without replacing terrain geometry. Sky detail may vary by time; terrain
 * detail always comes from the dusk master. Large-radius lighting fields deliberately exclude sharp reference hill
 * edges.
 */
final class ReferenceSceneLighting {
    private ReferenceSceneLighting() {
    }

    static BufferedImage apply(BufferedImage master, BufferedImage reference) {
        int w = master.getWidth(), h = master.getHeight();
        if (reference.getWidth() != w || reference.getHeight() != h)
            throw new IllegalArgumentException("Lighting references must match the master canvas");
        BufferedImage baseLight = soften(master, 40);
        BufferedImage targetLight = soften(reference, 40);
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            // End the sky blend above the highest hill in the authored plate.
            double t = Math.max(0, Math.min(1, (y / (double) h - .63) / .07));
            double terrain = t * t * (3 - 2 * t);
            for (int x = 0; x < w; x++) {
                int original = master.getRGB(x, y);
                double detail = (luminance(original) + 8) / (luminance(baseLight.getRGB(x, y)) + 8);
                detail = Math.max(.35, Math.min(2.2, detail));
                int sky = reference.getRGB(x, y), light = targetLight.getRGB(x, y);
                int rgb = original & 0xff000000;
                for (int shift : new int[] { 16, 8, 0 }) {
                    double skyChannel = (sky >>> shift) & 255;
                    double terrainChannel = ((light >>> shift) & 255) * detail;
                    int channel = (int) Math.round(skyChannel + (terrainChannel - skyChannel) * terrain);
                    rgb |= Math.max(0, Math.min(255, channel)) << shift;
                }
                result.setRGB(x, y, rgb);
            }
        }
        return result;
    }

    private static double luminance(int rgb) {
        return .2126 * ((rgb >>> 16) & 255) + .7152 * ((rgb >>> 8) & 255) + .0722 * (rgb & 255);
    }

    /** Separable box average with clamped edges, used only when loading a scene. */
    private static BufferedImage soften(BufferedImage image, int radius) {
        int w = image.getWidth(), h = image.getHeight();
        BufferedImage horizontal = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int pass = 0; pass < 2; pass++) {
            BufferedImage source = pass == 0 ? image : horizontal;
            BufferedImage target = pass == 0 ? horizontal : output;
            int lines = pass == 0 ? h : w, length = pass == 0 ? w : h;
            for (int line = 0; line < lines; line++) {
                int[] sum = new int[3];
                for (int n = -radius; n <= radius; n++)
                    accumulate(source, pass, line, Math.max(0, Math.min(length - 1, n)), sum, 1);
                for (int n = 0; n < length; n++) {
                    int count = 2 * radius + 1;
                    int rgb = (sum[0] / count << 16) | (sum[1] / count << 8) | sum[2] / count;
                    target.setRGB(pass == 0 ? n : line, pass == 0 ? line : n, rgb);
                    accumulate(source, pass, line, Math.max(0, n - radius), sum, -1);
                    accumulate(source, pass, line, Math.min(length - 1, n + radius + 1), sum, 1);
                }
            }
        }
        return output;
    }

    private static void accumulate(BufferedImage source, int pass, int line, int n, int[] sum, int sign) {
        int rgb = source.getRGB(pass == 0 ? n : line, pass == 0 ? line : n);
        sum[0] += sign * ((rgb >>> 16) & 255);
        sum[1] += sign * ((rgb >>> 8) & 255);
        sum[2] += sign * (rgb & 255);
    }
}
