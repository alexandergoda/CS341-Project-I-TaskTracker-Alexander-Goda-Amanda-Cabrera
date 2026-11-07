package tasktracker.view.theme;

import java.awt.image.BufferedImage;

/**
 * Restrained dusk color treatment, applied once when artwork loads. Original artwork and foreground alpha remain
 * unchanged.
 */
final class LastLightGrade {
    private LastLightGrade() {
    }

    static BufferedImage apply(BufferedImage source, boolean foreground) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getRGB(x, y);
                int alpha = pixel >>> 24;
                double red = (pixel >>> 16) & 255;
                double green = (pixel >>> 8) & 255;
                double blue = pixel & 255;
                double luminance = .2126 * red + .7152 * green + .0722 * blue;
                // Target warm colors, leaving the cool plum sky largely alone.
                double warmth = Math.max(0, Math.min(1, (red - blue) / 100.0));
                double saturation = 1 - warmth * (foreground ? .24 : .38);
                double highlights = Math.max(0, (luminance - 80) / 175.0);
                double exposure = 1 - highlights * (foreground ? .13 : .07);
                int r = channel((luminance + (red - luminance) * saturation) * exposure);
                int g = channel((luminance + (green - luminance) * saturation) * exposure);
                int b = channel((luminance + (blue - luminance) * saturation) * exposure);
                result.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    private static int channel(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }
}
