package tasktracker.view.animation;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** Horizontal, reversible workspace transition inside a stationary glass card. */
public final class CardTransitionHost extends JPanel {
    private static final long serialVersionUID = 1L;
    private JComponent current;
    private BufferedImage outgoing, incoming;
    private Timer motion;
    private double progress;
    private int direction = 1, animationWidth, animationHeight;

    public CardTransitionHost() {
        super(new BorderLayout());
        setOpaque(false);
    }

    public void showContent(JComponent next, boolean animate, boolean forward) {
        finish(false);
        if (current == next) {
            layoutTree(next);
            return;
        }
        BufferedImage captured = null;
        if (animate && current != null && getWidth() > 0 && getHeight() > 0)
            captured = snapshot(current);
        removeAll();
        current = next;
        add(current, BorderLayout.CENTER);
        revalidate();
        doLayout();
        layoutTree(current);
        outgoing = captured;
        incoming = null;
        direction = forward ? 1 : -1;
        progress = 0;
        if (outgoing == null) {
            current.setVisible(true);
            repaint();
            return;
        }
        animationWidth = getWidth();
        animationHeight = getHeight();
        // Capture after the caller finishes refreshing task text, without Swing's
        // screen double buffer contaminating the transparent image.
        current.setVisible(false);
        final JComponent expected = current;
        SwingUtilities.invokeLater(() -> {
            if (current == expected && outgoing != null) {
                layoutTree(current);
                incoming = snapshot(current);
                repaint();
            }
        });
        motion = ThemeMotion.animate(240, t -> {
            progress = t;
            if (t >= 1)
                finish(true);
            repaint();
        });
    }

    private BufferedImage snapshot(JComponent component) {
        double scale = getGraphicsConfiguration() == null ? 1
                : getGraphicsConfiguration().getDefaultTransform().getScaleX();
        BufferedImage image = new BufferedImage((int) Math.ceil(getWidth() * scale),
                (int) Math.ceil(getHeight() * scale), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);
        RepaintManager manager = RepaintManager.currentManager(component);
        boolean buffering = manager.isDoubleBufferingEnabled();
        try {
            manager.setDoubleBufferingEnabled(false);
            component.printAll(g);
        } finally {
            manager.setDoubleBufferingEnabled(buffering);
            g.dispose();
        }
        return image;
    }

    private static void layoutTree(Container c) {
        c.doLayout();
        for (Component child : c.getComponents())
            if (child instanceof Container)
                layoutTree((Container) child);
    }

    private void finish(boolean focus) {
        if (motion != null) {
            motion.stop();
            motion = null;
        }
        outgoing = null;
        incoming = null;
        progress = 1;
        if (current != null) {
            current.setVisible(true);
            revalidate();
            doLayout();
            layoutTree(current);
            if (focus) {
                JTextField field = firstField(current);
                if (field != null)
                    field.requestFocusInWindow();
            }
        }
    }

    private JTextField firstField(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof JTextField && child.isEnabled() && child.isVisible())
                return (JTextField) child;
            if (child instanceof Container && child.isVisible()) {
                JTextField field = firstField((Container) child);
                if (field != null)
                    return field;
            }
        }
        return null;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (outgoing != null && (getWidth() != animationWidth || getHeight() != animationHeight))
            finish(false);
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        if (outgoing == null) {
            super.paintChildren(graphics);
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.clipRect(0, 0, getWidth(), getHeight());
        // Small, sequential movement. Never drag an entire form across the card
        // or superimpose two sets of text.
        if (progress < .4) {
            double t = progress / .4;
            g.setComposite(AlphaComposite.SrcOver.derive((float) (1 - t)));
            g.drawImage(outgoing, 0, (int) (-direction * 6 * t), getWidth(), getHeight(), null);
        } else if (incoming != null) {
            double t = (progress - .4) / .6;
            g.setComposite(AlphaComposite.SrcOver.derive((float) t));
            g.drawImage(incoming, 0, (int) (direction * 12 * (1 - t)), getWidth(), getHeight(), null);
        }
        g.dispose();
    }

    @Override
    public void removeNotify() {
        finish(false);
        super.removeNotify();
    }
}
