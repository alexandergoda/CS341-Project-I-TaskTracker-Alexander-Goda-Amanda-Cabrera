package tasktracker.view.theme;

import tasktracker.view.components.SceneComboBox;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.File;
import tasktracker.model.User;

/** Headless rendering and model check. Does not open a database. */
public final class SceneComboBoxCheck {
    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            SceneComboBox<Object> combo = new SceneComboBox<>();
            User alex = new User(1, "alex"), amanda = new User(2, "amanda");
            combo.addItem("Unassigned");
            combo.addItem(alex);
            combo.addItem(amanda);
            combo.setSelectedItem(amanda);
            if (combo.getSelectedItem() != amanda)
                throw new AssertionError("User identity lost");
            if (combo.getActionMap().get("selectNext") == null)
                throw new AssertionError("Keyboard actions missing");
            BufferedImage image = new BufferedImage(440, 210, BufferedImage.TYPE_INT_RGB);
            for (LastLightScene scene : LastLightScene.values()) {
                SceneRenderState.weights = scene.weights();
                Graphics2D g = image.createGraphics();
                g.setColor(AdaptiveScenePalette.field());
                g.fillRect(0, 0, 440, 210);
                combo.setSize(420, 44);
                combo.doLayout();
                g.translate(10, 8);
                combo.paint(g);
                g.translate(0, 54);
                javax.swing.plaf.basic.BasicComboPopup popup = (javax.swing.plaf.basic.BasicComboPopup) combo.getUI()
                        .getAccessibleChild(combo, 0);
                popup.setSize(420, 132);
                layout(popup);
                popup.getList().setSelectedIndex(2);
                popup.paint(g);
                if (!(popup.getUI() instanceof javax.swing.plaf.basic.BasicPopupMenuUI))
                    throw new AssertionError("Native popup delegate remains");
                if (popup.getList().getFixedCellHeight() != 40)
                    throw new AssertionError("Row height");
                JViewport viewport = (JViewport) popup.getList().getParent();
                if (viewport.getExtentSize().height < 120)
                    throw new AssertionError("Clipped rows");
                g.dispose();
                if (!combo.getBackground().equals(AdaptiveScenePalette.field()))
                    throw new AssertionError("Scene tint mismatch");
            }
            if (args.length > 0)
                try {
                    ImageIO.write(image, "png", new File(args[0]));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            combo.removeNotify();
        });
        System.out.println("PASS: user identity, keyboard action mapping and dropdown rendering in six scenes.");
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component child : c.getComponents())
            if (child instanceof Container)
                layout((Container) child);
    }
}
