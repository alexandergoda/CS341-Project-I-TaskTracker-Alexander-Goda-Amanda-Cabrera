package tasktracker.view.theme;

import java.awt.image.BufferedImage;

/**
 * Relights one fixed flower plate without moving or regenerating any pixels. Alpha, lens blur, petal detail, and the
 * unequal corner crops stay registered. Results are cached by SceneRenderer, not recalculated during each repaint.
 */
final class ForegroundLighting {
    private ForegroundLighting() {
    }

    static BufferedImage apply(BufferedImage master, LastLightScene scene) {
        if (scene == LastLightScene.DUSK)
            return master;
        BufferedImage result = new BufferedImage(master.getWidth(), master.getHeight(), BufferedImage.TYPE_INT_ARGB);
        boolean night = scene == LastLightScene.NIGHT;
        double saturation = night ? .32 : .65;
        double redGain = night ? .68 : 1.0;
        double greenGain = night ? .79 : .98;
        double blueGain = night ? 1.02 : 1.12;
        if (scene == LastLightScene.DAWN) {
            // Diffuse pre-sunrise skylight, without a violet cast on the petals.
            saturation = .32;
            redGain = .78;
            greenGain = .87;
            blueGain = .98;
        } else if (scene == LastLightScene.MIDDAY) {
            saturation = .48;
            redGain = 1.08;
            greenGain = 1.12;
            blueGain = 1.20;
        } else if (scene == LastLightScene.GOLDEN_HOUR) {
            saturation = .55;
            redGain = 1.0;
            greenGain = 1.0;
            blueGain = 1.10;
        }
        for (int y = 0; y < master.getHeight(); y++) {
            for (int x = 0; x < master.getWidth(); x++) {
                int pixel = master.getRGB(x, y);
                int alpha = pixel & 0xff000000;
                double red = (pixel >>> 16) & 255;
                double green = (pixel >>> 8) & 255;
                double blue = pixel & 255;
                double luminance = .2126 * red + .7152 * green + .0722 * blue;
                // Golden-hour warmth belongs on lit petals, not in every shadow.
                double highlight = scene == LastLightScene.GOLDEN_HOUR
                        ? Math.max(0, Math.min(1, (luminance - 105) / 100))
                        : 0;
                // Reduce sunset color spill while retaining the master's shading.
                // Moonlight is dim and cool; sunrise is a softer peach/lavender.
                int r = channel((luminance + (red - luminance) * saturation) * redGain + 8 * highlight);
                int g = channel((luminance + (green - luminance) * saturation) * greenGain + 3 * highlight);
                int b = channel((luminance + (blue - luminance) * saturation) * blueGain - 5 * highlight);
                result.setRGB(x, y, alpha | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    private static int channel(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }
}
