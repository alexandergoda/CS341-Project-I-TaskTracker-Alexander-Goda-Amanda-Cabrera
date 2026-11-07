package tasktracker.view.theme;

import tasktracker.view.components.PriorityPicker;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import tasktracker.model.Priority;

/** Checks equal sizing, text fit and mouse/keyboard selection without storage. */
public final class PriorityPickerCheck {
    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            PriorityPicker picker = new PriorityPicker();
            for (int width : new int[] { 270, 300, 520 }) {
                picker.setSize(width, 42);
                picker.doLayout();
                int first = picker.getComponent(0).getWidth();
                for (Component c : picker.getComponents()) {
                    AbstractButton b = (AbstractButton) c;
                    if (b.getWidth() != first || !b.getFont().isBold())
                        throw new AssertionError("Unequal styling");
                    if (b.getFontMetrics(b.getFont()).stringWidth(b.getText()) + 20 > b.getWidth() - 8)
                        throw new AssertionError("Label does not fit");
                }
            }
            for (int i = 0; i < 3; i++) {
                AbstractButton b = (AbstractButton) picker.getComponent(i);
                b.doClick(0);
                if (picker.getSelectedItem() != Priority.values()[i])
                    throw new AssertionError("Mouse selection");
                b.getActionMap().get("RIGHT").actionPerformed(new ActionEvent(b, 0, "RIGHT"));
                if (picker.getSelectedItem() != Priority.values()[(i + 1) % 3])
                    throw new AssertionError("Keyboard selection");
            }
            BufferedImage image = new BufferedImage(560, 210, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(new Color(21, 31, 45));
            g.fillRect(0, 0, 560, 210);
            SceneRenderState.weights = LastLightScene.NIGHT.weights();
            g.translate(20, 20);
            for (Priority priority : Priority.values()) {
                picker.setSelectedItem(priority);
                picker.printAll(g);
                g.translate(0, 62);
            }
            g.dispose();
            if (args.length > 0)
                try {
                    ImageIO.write(image, "png", new File(args[0]));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
        });
        System.out.println("PASS: three widths, bold labels fit, equal sizing, mouse selection and keyboard cycling.");
        System.exit(0);
    }
}
