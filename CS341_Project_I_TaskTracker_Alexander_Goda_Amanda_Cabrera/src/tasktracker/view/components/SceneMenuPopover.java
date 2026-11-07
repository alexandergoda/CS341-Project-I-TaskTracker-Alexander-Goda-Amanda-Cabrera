package tasktracker.view.components;

import tasktracker.view.animation.ThemeMotion;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** In-window scene chooser with interruptible, matching open and close motion. */
final class SceneMenuPopover {
    private final JButton anchor;
    private final JPanel menu;
    private JPanel overlay;
    private JLayeredPane layer;
    private Timer motion;
    private float progress;
    private boolean open, above;
    private ComponentAdapter resize;

    SceneMenuPopover(JButton anchor, JPanel menu) {
        this.anchor = anchor;
        this.menu = menu;
    }

    boolean isMounted() {
        return overlay != null;
    }

    boolean isOpen() {
        return open;
    }

    float progress() {
        return progress;
    }

    void toggle() {
        if (overlay == null && !mount())
            return;
        animate(!open);
    }

    void close() {
        if (overlay != null)
            animate(false);
    }

    private boolean mount() {
        JRootPane root = SwingUtilities.getRootPane(anchor);
        if (root == null)
            return false;
        layer = root.getLayeredPane();
        overlay = new JPanel(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isPaintingOrigin() {
                return true;
            }

            @Override
            protected void paintChildren(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Rectangle r = menu.getBounds();
                int height = (int) Math.ceil(r.height * (.85 + .15 * progress));
                g.clipRect(r.x, above ? r.y + r.height - height : r.y, r.width, height);
                g.translate(0, (above ? 1 : -1) * 8 * (1 - progress));
                g.setComposite(AlphaComposite.SrcOver.derive(progress));
                super.paintChildren(g);
                g.dispose();
            }
        };
        overlay.setOpaque(false);
        overlay.setBounds(0, 0, layer.getWidth(), layer.getHeight());
        Point p = SwingUtilities.convertPoint(anchor, 0, anchor.getHeight(), layer);
        Dimension size = menu.getPreferredSize();
        int x = Math.max(8, Math.min(p.x + anchor.getWidth() - size.width, layer.getWidth() - size.width - 8));
        above = p.y + 8 + size.height > layer.getHeight() - 8;
        int y = above ? Math.max(8, p.y - anchor.getHeight() - 8 - size.height) : p.y + 8;
        menu.setBounds(x, y, size.width, size.height);
        menu.doLayout();
        overlay.add(menu);
        overlay.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point click = SwingUtilities.convertPoint(overlay, e.getPoint(), anchor);
                if (anchor.contains(click))
                    toggle();
                else
                    close();
            }
        });
        bind("ESCAPE", this::close);
        bind("DOWN", () -> moveFocus(1));
        bind("UP", () -> moveFocus(-1));
        bind("ENTER", () -> {
            Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (open && focus instanceof JButton && SwingUtilities.isDescendingFrom(focus, menu))
                ((JButton) focus).doClick(0);
        });
        layer.add(overlay, JLayeredPane.POPUP_LAYER);
        resize = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                dispose();
            }
        };
        layer.addComponentListener(resize);
        layer.revalidate();
        layer.repaint();
        return true;
    }

    private void bind(String key, Runnable action) {
        overlay.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key), key);
        overlay.getActionMap().put(key, new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void moveFocus(int direction) {
        Component[] items = menu.getComponents();
        if (!open || items.length == 0)
            return;
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        int current = direction > 0 ? -1 : 0;
        for (int i = 0; i < items.length; i++)
            if (items[i] == focus)
                current = i;
        items[Math.floorMod(current + direction, items.length)].requestFocusInWindow();
    }

    private void animate(boolean target) {
        if (motion != null)
            motion.stop();
        open = target;
        for (Component item : menu.getComponents())
            item.setEnabled(target);
        float start = progress, end = target ? 1 : 0;
        // Reversals start at the current frame, with duration proportional to distance.
        int duration = Math.max(1, Math.round(180 * Math.abs(end - start)));
        motion = ThemeMotion.animate(duration, t -> {
            progress = start + (end - start) * (float) t;
            if (overlay != null)
                overlay.repaint();
            anchor.repaint();
            if (t >= 1) {
                motion = null;
                if (!open)
                    dispose();
                else if (menu.getComponentCount() > 0)
                    menu.getComponent(0).requestFocusInWindow();
            }
        });
    }

    void dispose() {
        if (motion != null)
            motion.stop();
        motion = null;
        open = false;
        progress = 0;
        if (overlay != null) {
            layer.removeComponentListener(resize);
            overlay.remove(menu);
            layer.remove(overlay);
            overlay = null;
            layer.repaint();
            anchor.requestFocusInWindow();
        }
        anchor.repaint();
    }
}
