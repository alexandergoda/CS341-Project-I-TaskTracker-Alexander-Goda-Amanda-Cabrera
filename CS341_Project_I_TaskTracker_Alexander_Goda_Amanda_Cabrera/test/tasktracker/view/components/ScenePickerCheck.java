package tasktracker.view.components;

import tasktracker.view.theme.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Scene choice, automatic mode and compact control checks without SQLite. */
public final class ScenePickerCheck {
    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            try {
                SceneTheme.mode = "Auto";
                AppearanceControl control = new AppearanceControl(mode -> SceneTheme.mode = mode);
                control.setSize(215, 32);
                control.doLayout();
                AbstractButton manual = AdaptiveAppearanceCheck.find(control, "Dark mode");
                AbstractButton auto = AdaptiveAppearanceCheck.find(control, "Automatic appearance");
                Method build = AppearanceControl.class.getDeclaredMethod("buildSceneMenu");
                build.setAccessible(true);
                build.invoke(control);
                if (!SceneTheme.mode.equals("Auto"))
                    throw new AssertionError("Opening menu changes mode");
                Field menuField = AppearanceControl.class.getDeclaredField("sceneMenu");
                menuField.setAccessible(true);
                JPanel menu = (JPanel) menuField.get(control);
                if (menu.getComponentCount() != 6)
                    throw new AssertionError("Missing scenes");
                for (LastLightScene scene : LastLightScene.values()) {
                    control.selectScene(scene);
                    if (auto.isSelected() || !manual.isEnabled())
                        throw new AssertionError("Scene selection must release Auto");
                    if (SceneTheme.current()[scene.index] != 1)
                        throw new AssertionError("Wrong scene");
                }
                control.selectScene(LastLightScene.GOLDEN_HOUR);
                auto.doClick(0);
                if (!SceneTheme.mode.equals("Auto") || manual.isEnabled())
                    throw new AssertionError("Auto did not resume");
                auto.doClick(0);
                if (SceneTheme.current()[LastLightScene.GOLDEN_HOUR.index] != 1)
                    throw new AssertionError("Manual selection not remembered");
                manual.doClick(0);
                if (!SceneTheme.mode.equals("Dark"))
                    throw new AssertionError("Compact shortcut did not select night");
                control.selectScene(LastLightScene.GOLDEN_HOUR);
                build.invoke(control);
                if (args.length > 0) {
                    BufferedImage image = new BufferedImage(260, 330, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    g.setColor(new Color(24, 27, 36));
                    g.fillRect(0, 0, 260, 330);
                    g.translate(15, 10);
                    control.printAll(g);
                    g.translate(-15, 35);
                    for (Component item : menu.getComponents()) {
                        item.setSize(232, 44);
                        item.printAll(g);
                        g.translate(0, 44);
                    }
                    g.dispose();
                    ImageIO.write(image, "png", new File(args[0]));
                }
                control.removeNotify();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(
                "PASS: six scenes, opening without mutation, Auto release/resume, remembered selection, compact night shortcut.");
        System.exit(0);
    }
}
