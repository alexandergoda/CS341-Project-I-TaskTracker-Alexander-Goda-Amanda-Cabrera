package tasktracker.view.theme;

import tasktracker.view.screens.DashboardSurface;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Headless checks, no database access. */
public final class AdaptiveAppearanceCheck {
    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            SceneTheme.mode = "Light";
            if (SceneTheme.current()[LastLightScene.MIDDAY.index] != 1)
                throw new AssertionError("Sun must use midday");
            SceneTheme.mode = "Dark";
            if (SceneTheme.current()[LastLightScene.NIGHT.index] != 1)
                throw new AssertionError("Moon must use night");
            SceneTheme.mode = "Auto";
            double hour = java.time.LocalTime.now().toSecondOfDay() / 3600.0;
            double[] expected = SceneRenderState.at(hour), actual = SceneTheme.current();
            for (int i = 0; i < actual.length; i++)
                if (Math.abs(expected[i] - actual[i]) > .001)
                    throw new AssertionError("Auto clock mismatch");
            DashboardSurface d = new DashboardSurface("Preview");
            d.feedback.setText("12 task(s) loaded.");
            AbstractButton auto = find(d, "Automatic appearance"), manual = find(d, "Dark mode");
            if (!auto.isSelected() || manual.isEnabled())
                throw new AssertionError("Auto must disable manual");
            auto.doClick(0);
            if (!manual.isEnabled())
                throw new AssertionError("Manual not restored");
            manual.doClick(0);
            if (!SceneTheme.mode.equals(manual.isSelected() ? "Dark" : "Light"))
                throw new AssertionError("Toggle state mismatch");
            auto.doClick(0);
            for (int width : new int[] { 1100, 1440 }) {
                d.setSize(width, 850);
                layout(d);
                Rectangle count = SwingUtilities.convertRectangle(d.feedback.getParent(), d.feedback.getBounds(), d);
                if (count.y < 100 || count.y > 260 || count.height < 10)
                    throw new AssertionError("Count not in header: " + count);
                if (hasSlider(d))
                    throw new AssertionError("Temporary sliders remain");
                BufferedImage image = new BufferedImage(width, 850, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                d.printAll(g);
                g.dispose();
                if (args.length > 0 && width == 1440)
                    try {
                        ImageIO.write(image, "png", new File(args[0]));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
            }
            d.removeNotify();
        });
        System.out.println(
                "PASS: midday/night, local-time Auto, toggle enablement, task count header, no temporary sliders, two render sizes.");
        System.exit(0);
    }

    static void layout(Container c) {
        c.doLayout();
        for (Component x : c.getComponents())
            if (x instanceof Container)
                layout((Container) x);
    }

    static boolean hasSlider(Container c) {
        for (Component x : c.getComponents()) {
            if (x instanceof JSlider)
                return true;
            if (x instanceof Container && hasSlider((Container) x))
                return true;
        }
        return false;
    }

    public static AbstractButton find(Container c, String name) {
        for (Component x : c.getComponents()) {
            if (x instanceof AbstractButton && name.equals(x.getAccessibleContext().getAccessibleName()))
                return (AbstractButton) x;
            if (x instanceof Container) {
                AbstractButton b = find((Container) x, name);
                if (b != null)
                    return b;
            }
        }
        return null;
    }
}
