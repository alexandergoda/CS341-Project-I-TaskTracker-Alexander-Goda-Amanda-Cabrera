package tasktracker.view.animation;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.function.BooleanSupplier;

/** Replaces content in place, retaining the previous frame for a short dissolve. */
public final class TransitionHost extends JPanel {
    private static final long serialVersionUID = 1L;
    private BufferedImage previous, destination;
    private float remaining;
    private Timer motion, preparation;
    private WorkspaceEntrance entrance;

    public TransitionHost() {
        super(new BorderLayout());
        setOpaque(false);
    }

    public void showContent(JComponent next, boolean animate) {
        showContent(next, animate, () -> true);
    }

    /**
     * Keep a solid outgoing frame while asynchronous scene artwork is prepared.
     *
     * @param next    replacement content
     * @param animate whether to animate from the outgoing content
     * @param ready   readiness check used before an animated transition
     */
    public void showContent(JComponent next, boolean animate, BooleanSupplier ready) {
        showContent(next, animate, ready, null);
    }

    public void showWorkspace(JComponent next, BooleanSupplier ready, BufferedImage backdrop, Rectangle outgoingCard,
            java.util.function.Supplier<Rectangle[]> incomingCards) {
        showContent(next, true, ready,
                backdrop == null ? null : new WorkspaceEntrance(backdrop, outgoingCard, incomingCards));
    }

    private void showContent(JComponent next, boolean animate, BooleanSupplier ready, WorkspaceEntrance effect) {
        if (motion != null)
            motion.stop();
        if (preparation != null)
            preparation.stop();
        BufferedImage outgoing = null;
        if (animate && getWidth() > 0 && getHeight() > 0 && getComponentCount() > 0) {
            outgoing = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outgoing.createGraphics();
            paint(g);
            g.dispose();
        }
        previous = outgoing;
        destination = null;
        remaining = 1;
        entrance = effect;
        removeAll();
        add(next, BorderLayout.CENTER);
        revalidate();
        doLayout();
        repaint();
        layoutTree(next);
        if (previous != null) {
            // A child repaint can otherwise bypass this host's snapshot painting.
            // Hidden descendants cannot paint or receive clicks while we show snapshots.
            next.setVisible(false);
            preparation = new Timer(16, e -> {
                if (!ready.getAsBoolean())
                    return;
                preparation.stop();
                preparation = null;
                layoutTree(next);
                destination = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = destination.createGraphics();
                // No event can paint onscreen between these calls on the EDT.
                next.setVisible(true);
                try {
                    next.printAll(g);
                } finally {
                    next.setVisible(false);
                    g.dispose();
                }
                motion = ThemeMotion.animate(entrance == null ? 280 : 420, t -> {
                    remaining = (float) (1 - t);
                    if (t >= 1) {
                        previous = null;
                        destination = null;
                        entrance = null;
                        motion = null;
                        next.setVisible(true);
                        revalidate();
                    }
                    repaint();
                });
            });
            preparation.start();
        }
    }

    /** Route descendant dirty regions through the transition compositor. */
    @Override
    protected boolean isPaintingOrigin() {
        return true;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        // BorderLayout skips invisible children, but their scene still needs sizing.
        if (previous != null)
            for (Component child : getComponents())
                child.setBounds(0, 0, getWidth(), getHeight());
    }

    private static void layoutTree(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents())
            if (child instanceof Container)
                layoutTree((Container) child);
    }

    @Override
    public void paint(Graphics graphics) {
        if (previous == null) {
            super.paint(graphics);
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        if (destination != null && entrance != null) {
            entrance.paint(g, previous, destination, 1 - remaining, getWidth(), getHeight());
            g.dispose();
            return;
        }
        g.drawImage(previous, 0, 0, getWidth(), getHeight(), null);
        if (destination != null) {
            g.setComposite(AlphaComposite.SrcOver.derive(1 - remaining));
            g.drawImage(destination, 0, 0, getWidth(), getHeight(), null);
        }
        g.dispose();
    }

    @Override
    public void removeNotify() {
        if (motion != null)
            motion.stop();
        if (preparation != null)
            preparation.stop();
        previous = null;
        destination = null;
        entrance = null;
        for (Component child : getComponents())
            child.setVisible(true);
        super.removeNotify();
    }
}
