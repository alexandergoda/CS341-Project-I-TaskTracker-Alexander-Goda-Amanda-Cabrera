package tasktracker.view.components;

import tasktracker.view.animation.ThemeMotion;
import tasktracker.view.theme.AdaptiveScenePalette;
import tasktracker.view.theme.LastLightScene;
import tasktracker.view.theme.SceneTheme;
import tasktracker.view.theme.ThemePalette;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.function.Consumer;
import javax.swing.*;

/** Animated Swing appearance switch, separate from dashboard logic. */
public final class AppearanceControl extends JPanel {
    private static final long serialVersionUID = 1L;
    private final ModeSwitch manual = new ModeSwitch();
    private LastLightScene chosenScene;
    private final JPanel sceneMenu = new JPanel(new GridLayout(0, 1));
    private SceneMenuPopover popover;
    private final JButton expand = new JButton() {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover() || (popover != null && popover.isOpen())) {
                g.setColor(new Color(255, 220, 190, 24));
                g.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 4, 12, 12);
            }
            g.setColor(ThemePalette.INK);
            g.setStroke(new BasicStroke(1.5f));
            int x = getWidth() / 2, y = getHeight() / 2;
            g.rotate(Math.PI * (popover == null ? 0 : popover.progress()), x, y);
            g.drawLine(x - 4, y - 2, x, y + 2);
            g.drawLine(x, y + 2, x + 4, y - 2);
            g.dispose();
        }
    };
    private final JToggleButton auto = new JToggleButton("Auto") {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g.setColor(AdaptiveScenePalette.selection());
                g.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 28, 28);
                g.setColor(AdaptiveScenePalette.accent());
                g.fillOval(10, getHeight() / 2 - 3, 6, 6);
            }
            g.setColor(isSelected() ? AdaptiveScenePalette.secondaryText() : ThemePalette.MUTED);
            g.setFont(new Font("SansSerif", isSelected() ? Font.BOLD : Font.PLAIN, 13));
            g.drawString("Auto", 23, getHeight() / 2 + 5);
            if (hasFocus()) {
                g.setColor(new Color(255, 228, 201, 150));
                g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 26, 26);
            }
            g.dispose();
        }
    };
    private final Consumer<String> changed;

    public AppearanceControl(Consumer<String> changed) {
        this.changed = changed;
        setOpaque(false);
        setLayout(null);
        configure(manual);
        configure(auto);
        configure(expand);
        add(manual);
        add(expand);
        add(auto);
        expand.setVisible(true);
        expand.getAccessibleContext().setAccessibleName("Choose scene");
        sceneMenu.setBackground(new Color(32, 34, 44));
        sceneMenu.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(110, 105, 113)),
                javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        expand.addActionListener(e -> {
            if (popover == null)
                popover = new SceneMenuPopover(expand, sceneMenu);
            if (!popover.isMounted())
                buildSceneMenu();
            popover.toggle();
        });
        manual.setSelected(!SceneTheme.mode.equals("Light"));
        manual.position = manual.isSelected() ? 1 : 0;
        auto.setSelected(SceneTheme.mode.equals("Auto"));
        if (SceneTheme.mode.equals("Scene")) {
            chosenScene = SceneTheme.selectedScene;
            manual.sceneIcon = chosenScene;
            manual.setSelected(chosenScene == LastLightScene.NIGHT);
            manual.position = manual.isSelected() ? 1 : 0;
        }
        manual.setEnabled(!auto.isSelected());
        manual.getAccessibleContext().setAccessibleName("Dark mode");
        auto.getAccessibleContext().setAccessibleName("Automatic appearance");
        manual.addActionListener(e -> {
            chosenScene = null;
            manual.sceneIcon = null;
            manual.animateTo(manual.isSelected());
            changed.accept(manual.isSelected() ? "Dark" : "Light");
            refreshSceneSelection();
            updateManualCursor();
        });
        auto.addActionListener(e -> {
            manual.setEnabled(!auto.isSelected());
            manual.sceneIcon = auto.isSelected() ? null : chosenScene;
            changed.accept(auto.isSelected() ? "Auto"
                    : chosenScene != null ? "Scene" : (manual.isSelected() ? "Dark" : "Light"));
            if (!auto.isSelected() && chosenScene != null)
                manual.setSelected(chosenScene == LastLightScene.NIGHT);
            if (!auto.isSelected())
                manual.animateTo(manual.isSelected());
            refreshSceneSelection();
            updateManualCursor();
            repaint();
        });
        updateManualCursor();
    }

    private void buildSceneMenu() {
        sceneMenu.removeAll();
        for (LastLightScene scene : LastLightScene.values()) {
            JButton item = new JButton(scene.label) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(32, 34, 44));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    if (getModel().isRollover() || hasFocus() || isSelected()) {
                        g.setColor(new Color(255, 205, 170, getModel().isRollover() || hasFocus() ? 40 : 23));
                        g.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 12, 12);
                    }
                    new SceneIcon(scene, ThemePalette.INK).paintIcon(this, g, 12, 10);
                    g.setColor(ThemePalette.INK);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    g.drawString(scene.label, 48, 28);
                    if (isSelected()) {
                        g.setColor(AdaptiveScenePalette.accent());
                        g.fillOval(getWidth() - 24, 19, 6, 6);
                    }
                    g.dispose();
                }
            };
            configure(item);
            item.setPreferredSize(new Dimension(220, 44));
            item.addActionListener(e -> selectScene(scene));
            sceneMenu.add(item);
        }
        refreshSceneSelection();
    }

    private void refreshSceneSelection() {
        LastLightScene[] scenes = LastLightScene.values();
        for (int i = 0; i < sceneMenu.getComponentCount(); i++) {
            LastLightScene scene = scenes[i];
            boolean selected = !auto.isSelected() && (chosenScene == scene || chosenScene == null
                    && (manual.isSelected() ? scene == LastLightScene.NIGHT : scene == LastLightScene.MIDDAY));
            JButton item = (JButton) sceneMenu.getComponent(i);
            item.setSelected(selected);
            item.getAccessibleContext().setAccessibleName(scene.label + (selected ? ", selected" : ""));
        }
        sceneMenu.repaint();
    }

    void selectScene(LastLightScene scene) {
        chosenScene = scene;
        SceneTheme.selectedScene = scene;
        auto.setSelected(false);
        manual.setEnabled(true);
        manual.sceneIcon = scene;
        manual.setSelected(scene == LastLightScene.NIGHT);
        manual.animateTo(manual.isSelected());
        if (popover != null)
            popover.close();
        changed.accept("Scene");
        refreshSceneSelection();
        updateManualCursor();
        repaint();
    }

    private static void configure(AbstractButton button) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void updateManualCursor() {
        manual.setCursor(Cursor.getPredefinedCursor(auto.isSelected() ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
    }

    public void reflect(boolean dark) {
        if (auto.isSelected()) {
            manual.setSelected(dark);
            manual.animateTo(dark);
        }
    }

    @Override
    public void doLayout() {
        manual.setBounds(0, 0, 90, getHeight());
        expand.setBounds(94, 0, 28, getHeight());
        auto.setBounds(130, 0, 74, getHeight());
    }

    @Override
    public void removeNotify() {
        if (popover != null)
            popover.dispose();
        super.removeNotify();
    }

    private static final class ModeSwitch extends JToggleButton {
        private static final long serialVersionUID = 1L;
        private float position = 1;
        private Timer animation;
        private LastLightScene sceneIcon;

        void animateTo(boolean dark) {
            if (animation != null)
                animation.stop();
            final float from = position, to = dark ? 1 : 0;
            if (Math.abs(from - to) < .001)
                return;
            animation = ThemeMotion.animate(240, t -> {
                position = from + (to - from) * (float) t;
                repaint();
            });
        }

        @Override
        public void removeNotify() {
            if (animation != null)
                animation.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled())
                g.setComposite(AlphaComposite.SrcOver.derive(.45f));
            int y = (getHeight() - 30) / 2;
            g.setColor(new Color(38, 29, 40, 165));
            g.fillRoundRect(0, y, 90, 30, 30, 30);
            g.setColor(AdaptiveScenePalette.accent());
            g.fillRoundRect(3 + (int) (46 * position), y + 3, 38, 24, 24, 24);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(position < .5 ? new Color(63, 39, 31) : new Color(255, 227, 195));
            if (sceneIcon != null && sceneIcon != LastLightScene.NIGHT) {
                new SceneIcon(sceneIcon, position < .5 ? new Color(63, 39, 31) : ThemePalette.INK).paintIcon(this, g,
                        10, y + 3);
            } else {
                g.drawOval(17, y + 10, 10, 10);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.drawLine(22 + (int) (8 * Math.cos(a)), y + 15 + (int) (8 * Math.sin(a)),
                            22 + (int) (10 * Math.cos(a)), y + 15 + (int) (10 * Math.sin(a)));
                }
            }
            g.setColor(position > .5 ? new Color(63, 39, 31) : new Color(255, 227, 195));
            Area moon = new Area(new Ellipse2D.Double(61, y + 7, 16, 16));
            moon.subtract(new Area(new Ellipse2D.Double(67, y + 4, 14, 15)));
            g.fill(moon);
            if (hasFocus()) {
                g.setColor(new Color(255, 228, 201, 190));
                g.drawRoundRect(1, y + 1, 87, 28, 28, 28);
            }
            g.dispose();
        }
    }
}
