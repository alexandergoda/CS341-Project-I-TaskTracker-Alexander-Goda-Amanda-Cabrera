package tasktracker.view.theme;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Reads artwork from the assets classpath folder or a packaged resource JAR. */
public final class ThemeAssets {
    private ThemeAssets() {
    }

    public static BufferedImage image(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_/-]+") || name.contains("..")) {
            throw new IllegalArgumentException("Invalid artwork resource name");
        }
        String resource = "/" + name + ".png";
        try (InputStream stream = ThemeAssets.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Resource not on classpath: " + resource);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null)
                throw new IOException("Invalid PNG resource: " + resource);
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load theme artwork " + resource
                    + ". Include the assets directory or artwork JAR on the classpath.", exception);
        }
    }
}
