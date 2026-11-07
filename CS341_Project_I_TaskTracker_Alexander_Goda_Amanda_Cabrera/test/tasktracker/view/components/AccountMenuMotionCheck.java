package tasktracker.view.components;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import javax.swing.*;

/** Exercises the mounted popover, including clicks intercepted by its overlay. */
public final class AccountMenuMotionCheck {
    private static JRootPane root;
    private static AccountMenuButton control;
    private static int selections;
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

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            try {
                root = new JRootPane();
                root.setSize(640, 480);
                root.getContentPane().setLayout(null);
                root.doLayout();
                control = new AccountMenuButton();
                control.onChangePassword(e -> selections++);
                control.setBounds(580, 20, 28, 32);
                root.getContentPane().add(control);
                control.doLayout();
                arrow = control;
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
                if (selections != 0)
                    throw new AssertionError("Opening menu triggered password action");
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
            try {
                ((JButton) field(control, "changePassword")).doClick(0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (selections != 1)
                throw new AssertionError("Menu selection did not trigger exactly one action");
            control.removeNotify();
            if (menu.isMounted())
                throw new AssertionError("Removal left overlay");
        });
        System.out.println(
                "PASS: arrow toggle, continuous reversal, animated dismissal, Escape, outside click, removal cleanup.");
        System.exit(0);
    }
}
