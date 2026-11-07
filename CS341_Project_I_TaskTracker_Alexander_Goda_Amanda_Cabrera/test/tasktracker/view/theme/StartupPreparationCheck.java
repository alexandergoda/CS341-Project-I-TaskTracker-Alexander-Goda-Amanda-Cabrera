package tasktracker.view.theme;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import tasktracker.view.screens.StartupSurface;

/** Verifies detached artwork preparation, EDT responsiveness and first-frame reuse. */
public final class StartupPreparationCheck {
    private static SunsetLoginPanel login;

    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    public static void main(String[] args) throws Exception {
        boolean missingAssets = args.length > 0 && args[0].equals("--missing-assets");
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch responsive = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            StartupSurface startup = new StartupSurface();
            startup.setSize(1100, 760);
            BufferedImage image = new BufferedImage(1100, 760, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            startup.paint(g);
            g.dispose();
            if ((image.getRGB(0, 0) & 0xffffff) == 0xffffff)
                throw new AssertionError("White startup frame");
            // Outside the centered labels and intentional footer, retain the solid initial background.
            boolean creditPainted = false;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (y >= 330 && y <= 415)
                        continue;
                    if (y >= 720 && y <= 740 && x >= 350 && x <= 750) {
                        creditPainted |= image.getRGB(x, y) != StartupSurface.BACKGROUND.getRGB();
                        continue;
                    }
                    if (image.getRGB(x, y) != StartupSurface.BACKGROUND.getRGB())
                        throw new AssertionError("Startup background changed from initial solid color");
                }
            }
            if (!creditPainted)
                throw new AssertionError("Startup creator credit is missing");
            login = new SunsetLoginPanel(new JPanel());
            login.setSize(1100, 760);
            login.doLayout();
            login.prepareStartup(() -> {
                if (!SwingUtilities.isEventDispatchThread())
                    throw new AssertionError("Publication must run on EDT");
                ready.countDown();
            });
            SwingUtilities.invokeLater(responsive::countDown);
        });
        if (!responsive.await(1, TimeUnit.SECONDS))
            throw new AssertionError("Startup blocked EDT");
        if (!ready.await(30, TimeUnit.SECONDS))
            throw new AssertionError("Startup never finished");
        // Move past an Auto clock tick; mounting must not invalidate the first frame.
        Thread.sleep(1100);
        SwingUtilities.invokeAndWait(() -> {
            try {
                Object background = field(login, "backgroundCache");
                login.addNotify();
                BufferedImage image = new BufferedImage(1100, 760, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                login.paint(g);
                g.dispose();
                if (missingAssets) {
                    if (!Boolean.TRUE.equals(field(login, "artworkFailed")))
                        throw new AssertionError("Expected usable artwork failure fallback");
                } else if (background == null || background != field(login, "backgroundCache")) {
                    throw new AssertionError("Mounting rebuilt startup artwork on EDT");
                }
                login.removeNotify();
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        });
        System.out.println("PASS: nonwhite startup, responsive EDT, completed preparation and "
                + (missingAssets ? "missing-artwork fallback." : "cached first login frame."));
        System.exit(0);
    }
}
