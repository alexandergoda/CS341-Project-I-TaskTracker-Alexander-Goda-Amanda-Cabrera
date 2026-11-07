package tasktracker.view.theme;

import tasktracker.view.animation.TransitionHost;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

/** No database or real window needed: test frame continuity during slow preparation. */
public final class ScreenTransitionCheck {
    private static TransitionHost host;
    private static final AtomicBoolean ready = new AtomicBoolean();

    private static JPanel panel(Color color) {
        JPanel p = new JPanel();
        p.setBackground(color);
        return p;
    }

    private static Color pixel() {
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        host.paint(g);
        g.dispose();
        return new Color(image.getRGB(320, 240));
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            host = new TransitionHost();
            host.setSize(640, 480);
            host.showContent(panel(Color.RED), false);
            host.showContent(panel(Color.BLUE), true, ready::get);
        });
        Thread.sleep(400);
        SwingUtilities.invokeAndWait(() -> {
            if (!pixel().equals(Color.RED))
                throw new AssertionError("Outgoing frame disappeared before readiness");
            if (host.getComponent(0).isVisible())
                throw new AssertionError("Live destination exposed during preparation");
            ready.set(true);
        });
        Thread.sleep(130);
        SwingUtilities.invokeAndWait(() -> {
            Color c = pixel();
            if (host.getComponent(0).isVisible())
                throw new AssertionError("Live destination exposed during fade");
            if (c.getRed() == 0 || c.getBlue() == 0 || c.getGreen() != 0)
                throw new AssertionError("Expected solid blended frame: " + c);
        });
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(() -> {
            if (!pixel().equals(Color.BLUE))
                throw new AssertionError("Destination not shown");
            if (!host.getComponent(0).isVisible())
                throw new AssertionError("Live destination not restored");
            host.showContent(panel(Color.GREEN), true, () -> false);
            host.removeNotify();
        });
        System.out.println(
                "PASS: delayed readiness holds old frame, opaque crossfade, final live frame, removal cleanup.");
        System.exit(0);
    }
}
