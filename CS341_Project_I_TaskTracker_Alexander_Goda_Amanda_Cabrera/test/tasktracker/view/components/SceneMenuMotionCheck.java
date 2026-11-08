package tasktracker.view.components;

import tasktracker.view.theme.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import javax.swing.*;

/** Exercises the mounted popover, including clicks intercepted by its overlay. */
public final class SceneMenuMotionCheck {
    private static JRootPane root;
    private static AppearanceControl control;
    private static JButton arrow;
    private static SceneMenuPopover menu;

    private static Object field(Object object, String name) throws Exception {
        Field f = object.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(object);
    }

    private static void arrowClick() throws Exception {
        JPanel overlay = (JPanel) field(menu, "overlay");
        Point p = SwingUtilities.convertPoint(arrow, arrow.getWidth() / 2, arrow.getHeight() / 2, overlay);
        overlay.dispatchEvent(
                new MouseEvent(overlay, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, p.x, p.y, 1, false));
    }

    private static void assertSelectedScene(LastLightScene expected) throws Exception {
        JPanel items = (JPanel) field(control, "sceneMenu");
        int accent = AdaptiveScenePalette.accent().getRGB() & 0xFFFFFF;
        for (Component child : items.getComponents()) {
            JButton item = (JButton) child;
            boolean selected = item.getText().equals(expected.label);
            String name = item.getText() + (selected ? ", selected" : "");
            if (!name.equals(item.getAccessibleContext().getAccessibleName()))
                throw new AssertionError("Stale accessible scene marker: " + item.getText());
            BufferedImage image = new BufferedImage(item.getWidth(), item.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            item.printAll(g);
            g.dispose();
            boolean dot = (image.getRGB(item.getWidth() - 21, 22) & 0xFFFFFF) == accent;
            if (dot != selected)
                throw new AssertionError("Stale painted scene marker: " + item.getText());
        }
    }

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            try {
                root = new JRootPane();
                root.setSize(640, 480);
                root.getContentPane().setLayout(null);
                root.doLayout();
                SceneTheme.mode = "Light";
                control = new AppearanceControl(mode -> SceneTheme.mode = mode);
                control.setBounds(380, 20, 215, 32);
                root.getContentPane().add(control);
                control.doLayout();
                arrow = (JButton) field(control, "expand");
                arrow.doClick(0);
                menu = (SceneMenuPopover) field(control, "popover");
                if (!menu.isMounted() || !menu.isOpen())
                    throw new AssertionError("Did not open");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(90);
        SwingUtilities.invokeAndWait(() -> {
            try {
                float before = menu.progress();
                arrowClick();
                if (menu.isOpen() || Math.abs(before - menu.progress()) > .001)
                    throw new AssertionError("Closing jumped");
                arrowClick();
                if (!menu.isOpen() || Math.abs(before - menu.progress()) > .001)
                    throw new AssertionError("Reversal jumped");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(240);
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (menu.progress() != 1)
                    throw new AssertionError("Did not settle open");
                assertSelectedScene(LastLightScene.MIDDAY);
                JPanel items = (JPanel) field(control, "sceneMenu");
                for (Component child : items.getComponents()) {
                    JButton item = (JButton) child;
                    if (item.getText().equals(LastLightScene.GOLDEN_HOUR.label))
                        item.doClick(0);
                }
                if (menu.isOpen() || !menu.isMounted())
                    throw new AssertionError("Scene selection did not begin animated dismissal");
                float before = menu.progress();
                arrowClick();
                if (!menu.isOpen() || Math.abs(before - menu.progress()) > .001)
                    throw new AssertionError("Reopening after selection jumped");
                if (!SceneTheme.mode.equals("Scene") || SceneTheme.selectedScene != LastLightScene.GOLDEN_HOUR)
                    throw new AssertionError("Scene selection did not update the active appearance");
                assertSelectedScene(LastLightScene.GOLDEN_HOUR);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(240);
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (menu.progress() != 1)
                    throw new AssertionError("Reopened selection did not settle open");
                assertSelectedScene(LastLightScene.GOLDEN_HOUR);
                arrowClick();
                if (!menu.isMounted())
                    throw new AssertionError("Closed abruptly without animation");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(240);
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (menu.isMounted())
                    throw new AssertionError("Close did not detach");
                arrow.doClick(0);
                JPanel overlay = (JPanel) field(menu, "overlay");
                overlay.getActionMap().get("ESCAPE").actionPerformed(new ActionEvent(overlay, 0, ""));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(240);
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (menu.isMounted())
                    throw new AssertionError("Escape did not close");
                arrow.doClick(0);
                JPanel overlay = (JPanel) field(menu, "overlay");
                overlay.dispatchEvent(new MouseEvent(overlay, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0,
                        5, 5, 1, false));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(240);
        SwingUtilities.invokeAndWait(() -> {
            if (menu.isMounted())
                throw new AssertionError("Outside click did not close");
            arrow.doClick(0);
            control.removeNotify();
            if (menu.isMounted())
                throw new AssertionError("Removal left overlay");
        });
        System.out.println(
                "PASS: arrow toggle, continuous reversal, scene selection markers after rapid reopening, animated dismissal, Escape, outside click, removal cleanup.");
        System.exit(0);
    }
}
