package tasktracker.view.components;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Direct precision scrolling; only the edge return is animated. */
public final class FluidScrollPane extends JScrollPane {
    private static final long serialVersionUID = 1L;
    private final Timer timer;
    private final ScrollContent content;
    private double position, stretch, pull, stretchVelocity;
    private long lastTick;
    private boolean updating;

    public FluidScrollPane(Component view) {
        super();
        content = new ScrollContent(view);
        setViewportView(content);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setViewportBorder(null);
        getViewport().setOpaque(false);
        getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        setWheelScrollingEnabled(false);
        JScrollBar bar = getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(5, 0));
        bar.setUnitIncrement(18);
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(176, 139, 125);
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            }

            @Override
            protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(thumbColor);
                g.fillRoundRect(r.x, r.y, r.width, r.height, 5, 5);
                g.dispose();
            }

            @Override
            protected JButton createDecreaseButton(int o) {
                return blank();
            }

            @Override
            protected JButton createIncreaseButton(int o) {
                return blank();
            }

            private JButton blank() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
        bar.setOpaque(false);
        timer = new Timer(16, e -> tick());
        timer.setCoalesce(true);
        bar.addAdjustmentListener(e -> {
            if (!updating) {
                timer.stop();
                position = e.getValue();
                stretch = pull = stretchVelocity = 0;
                content.offset(0);
            }
        });
        addMouseWheelListener(this::wheel);
    }

    private int maximum() {
        JScrollBar b = getVerticalScrollBar();
        return Math.max(0, b.getMaximum() - b.getVisibleAmount());
    }

    private void wheel(MouseWheelEvent e) {
        if (e.isControlDown() || e.isShiftDown())
            return;
        double delta = e.getPreciseWheelRotation()
                * (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL ? getViewport().getExtentSize().height * .85
                        : Math.max(1, e.getScrollAmount()) * 12);
        double max = maximum();
        if (delta == 0) {
            e.consume();
            return;
        }
        // Moving back first unwinds the rubber band, then scrolls any remainder.
        if (pull * delta > 0) {
            double unwind = Math.copySign(Math.min(Math.abs(pull), Math.abs(delta)), delta);
            pull -= unwind;
            delta -= unwind;
        }
        double requested = position + delta;
        position = Math.max(0, Math.min(max, requested));
        double excess = requested - position;
        // macOS already sends momentum events. Do not queue or ease them a second time.
        // Keep fractional pixels between events so slow trackpad gestures are not lost.
        if (excess != 0) {
            pull = Math.max(-480, Math.min(480, pull - excess));
        }
        // Input changes only the requested pull. The timer is the sole writer
        // of the visible stretch, avoiding event/timer tug-of-war between frames.
        if (Math.abs(pull) > .01 || Math.abs(stretch) > .01 || Math.abs(stretchVelocity) > .01) {
            if (!timer.isRunning()) {
                lastTick = System.nanoTime();
                timer.start();
            }
        } else
            timer.stop();
        applyPosition();
        e.consume();
    }

    private void tick() {
        long now = System.nanoTime();
        position = Math.max(0, Math.min(maximum(), position));
        // Continuous restoring force, with no inactivity wait or resettable
        // release timeline. Momentum events cannot postpone the return.
        double dt = Math.max(0, (now - lastTick) / 1_000_000_000.0);
        lastTick = now;
        double target = 64 * pull / (100 + Math.abs(pull));
        // Exact critically damped spring step: retain velocity across wheel
        // events rather than jumping outward then snapping inward each frame.
        double omega = 50, displacement = stretch - target;
        double c = stretchVelocity + omega * displacement;
        double decay = Math.exp(-omega * dt);
        stretch = target + (displacement + c * dt) * decay;
        stretchVelocity = (stretchVelocity - omega * c * dt) * decay;
        pull *= Math.exp(-28 * dt);
        applyPosition();
        if (Math.abs(stretch) < .25 && Math.abs(pull) < .25 && Math.abs(stretchVelocity) < 3)
            stopMotion();
    }

    private void applyPosition() {
        updating = true;
        getVerticalScrollBar().setValue((int) Math.round(position));
        // The viewport's logical scroll position never overshoots. Only the child
        // moves, so Swing cannot fight the bounce and mouse coordinates stay correct.
        content.offset((int) Math.round(stretch));
        updating = false;
        getViewport().repaint();
    }

    public void stopMotion() {
        timer.stop();
        stretch = pull = stretchVelocity = 0;
        position = getVerticalScrollBar().getValue();
        content.offset(0);
    }

    private static final class ScrollContent extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        private final Component child;
        private int offset;

        ScrollContent(Component child) {
            this.child = child;
            setLayout(null);
            setOpaque(false);
            add(child);
        }

        void offset(int y) {
            if (offset != y) {
                offset = y;
                doLayout();
                repaint();
            }
        }

        @Override
        public void doLayout() {
            child.setBounds(0, offset, getWidth(), getHeight());
        }

        @Override
        public Dimension getPreferredSize() {
            return child.getPreferredSize();
        }

        public Dimension getPreferredScrollableViewportSize() {
            return child instanceof Scrollable ? ((Scrollable) child).getPreferredScrollableViewportSize()
                    : child.getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return child instanceof Scrollable ? ((Scrollable) child).getScrollableUnitIncrement(r, o, d) : 18;
        }

        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return child instanceof Scrollable ? ((Scrollable) child).getScrollableBlockIncrement(r, o, d) : r.height;
        }

        public boolean getScrollableTracksViewportWidth() {
            return !(child instanceof Scrollable) || ((Scrollable) child).getScrollableTracksViewportWidth();
        }

        public boolean getScrollableTracksViewportHeight() {
            return child instanceof Scrollable && ((Scrollable) child).getScrollableTracksViewportHeight();
        }
    }

    @Override
    public void removeNotify() {
        stopMotion();
        super.removeNotify();
    }
}
