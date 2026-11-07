package tasktracker.view.theme;

import tasktracker.view.screens.DashboardSurface;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.lang.reflect.Field;

/** Exercises asynchronous scene preparation and interrupted toggles without SQLite. */
public final class CachedDashboardCheck {
    static DashboardSurface d;

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            d = new DashboardSurface("Preview");
            d.setSize(1440, 850);
            d.feedback.setText("3 tasks loaded.");
            for (int i = 0; i < 3; i++)
                d.model.addRow(new Object[] { i, "Review task " + (i + 1), "alex", "alex", "", "HIGH", "Open" });
            AdaptiveAppearanceCheck.layout(d);
            if (d.newTask.getX() > 450)
                throw new AssertionError("New task is not beside title");
        });
        awaitReady();
        SwingUtilities.invokeAndWait(() -> {
            AbstractButton auto = AdaptiveAppearanceCheck.find(d, "Automatic appearance");
            if (auto.isSelected())
                auto.doClick(0);
            AdaptiveAppearanceCheck.find(d, "Dark mode").doClick(0);
        });
        Thread.sleep(250);
        SwingUtilities.invokeAndWait(() -> AdaptiveAppearanceCheck.find(d, "Dark mode").doClick(0));
        awaitReady();
        SwingUtilities.invokeAndWait(() -> {
            try {
                BufferedImage image = new BufferedImage(1440, 850, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                d.printAll(g);
                g.dispose();
                if (args.length > 0)
                    ImageIO.write(image, "png", new File(args[0]));
                if (!d.table.isEnabled())
                    throw new AssertionError("Task table blocked");
                d.removeNotify();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(
                "PASS: background preparation, rapid toggle replacement, title-adjacent New task and live task controls.");
        System.exit(0);
    }

    static void awaitReady() throws Exception {
        for (int i = 0; i < 40; i++) {
            Thread.sleep(250);
            boolean[] ready = { false };
            SwingUtilities.invokeAndWait(() -> {
                try {
                    Object layer = get(d, "cachedScene");
                    ready[0] = get(layer, "current") != null && get(layer, "worker") == null
                            && get(layer, "animation") == null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if (ready[0])
                return;
        }
        throw new AssertionError("Scene did not become ready");
    }

    static Object get(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }
}
