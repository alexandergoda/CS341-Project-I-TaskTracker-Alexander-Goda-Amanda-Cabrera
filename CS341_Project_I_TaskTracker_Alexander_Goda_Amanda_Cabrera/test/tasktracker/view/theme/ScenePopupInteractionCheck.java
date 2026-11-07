package tasktracker.view.theme;

import tasktracker.view.components.SceneComboBox;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboPopup;

/** Exercises the in-window popup, not just its cell renderer. No database access. */
public final class ScenePopupInteractionCheck {
    private static SceneComboBox<String> combo;
    private static JRootPane root;
    private static BasicComboPopup popup;

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            SceneRenderState.weights = LastLightScene.NIGHT.weights();
            root = new JRootPane();
            root.setSize(520, 420);
            root.getContentPane().setLayout(null);
            root.getContentPane().setBackground(new Color(21, 31, 45));
            combo = new SceneComboBox<>();
            combo.addItem("Unassigned");
            combo.addItem("alex");
            combo.addItem("amanda");
            combo.setSelectedIndex(2);
            combo.setBounds(35, 40, 440, 44);
            root.getContentPane().add(combo);
            layout(root);
            popup = (BasicComboPopup) combo.getUI().getAccessibleChild(combo, 0);
            combo.setPopupVisible(true);
            if (!combo.isPopupVisible() || !SwingUtilities.isDescendingFrom(popup, root.getLayeredPane()))
                throw new AssertionError("Popup not inside existing window");
            if (popup.getHeight() != 132)
                throw new AssertionError("Unexpected popup height");
        });
        Thread.sleep(220);
        SwingUtilities.invokeAndWait(() -> {
            if (args.length > 0) {
                BufferedImage image = new BufferedImage(520, 420, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                root.printAll(g);
                g.dispose();
                try {
                    ImageIO.write(image, "png", new File(args[0]));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            popup.getList().setSelectedIndex(1);
            Action enter = combo.getActionMap().get("enterPressed");
            if (enter == null)
                throw new AssertionError("Enter binding absent");
            enter.actionPerformed(new ActionEvent(combo, 0, "enterPressed"));
            if (combo.isPopupVisible() || combo.getSelectedIndex() != 1)
                throw new AssertionError("Enter failed");
            for (int i = 0; i < 5; i++) {
                combo.setPopupVisible(true);
                combo.setPopupVisible(false);
            }
            if (popup.getParent() != null)
                throw new AssertionError("Detached popup retained");
            combo.setPopupVisible(true);
            JPanel overlay = (JPanel) popup.getParent();
            MouseEvent outside = new MouseEvent(overlay, MouseEvent.MOUSE_PRESSED, 0, 0, 2, 2, 1, false,
                    MouseEvent.BUTTON1);
            for (MouseListener listener : overlay.getMouseListeners())
                listener.mousePressed(outside);
            if (combo.isPopupVisible())
                throw new AssertionError("Outside click failed");
            combo.setPopupVisible(true);
            combo.getActionMap().get("hidePopup").actionPerformed(new ActionEvent(combo, 0, "hidePopup"));
            if (combo.isPopupVisible())
                throw new AssertionError("Escape failed");
            combo.setPopupVisible(true);
            combo.removeNotify();
            if (combo.isPopupVisible())
                throw new AssertionError("Removal cleanup failed");
        });
        System.out.println(
                "PASS: in-window hosting, animation settling, Enter selection, Escape, outside click, rapid reopen and removal cleanup.");
        System.exit(0);
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component child : c.getComponents())
            if (child instanceof Container)
                layout((Container) child);
    }
}
