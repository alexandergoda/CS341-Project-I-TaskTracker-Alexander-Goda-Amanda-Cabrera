package tasktracker.view.theme;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import tasktracker.view.presentation.ProjectCredit;

/** Cached scenery and frost, with real Swing controls above and decorative petals in front. */
public final class SunsetLoginPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final SceneRenderer scenery = new SceneRenderer();
    private double[] weights = SceneTheme.current();
    private final Timer clock = new Timer(30000, e -> {
        weights = SceneTheme.current();
        repaint();
    });

    @Override
    public void addNotify() {
        super.addNotify();
        // Keep the prepared first frame, even if the clock advanced while loading.
        if (startupPrepared)
            startupPrepared = false;
        else
            weights = SceneTheme.current();
        clock.start();
    }

    @Override
    public void removeNotify() {
        clock.stop();
        super.removeNotify();
    }

    private final JPanel form;
    private int cardHeight;
    private Rectangle card = new Rectangle();
    private BufferedImage backgroundCache, foregroundCache;
    private double[] cachedWeights;
    private boolean artworkFailed;
    private boolean startupPrepared;

    /**
     * Called while the login panel is detached and cannot paint. Capture Swing state on the EDT, prepare image layers
     * on a worker, then publish on the EDT.
     *
     * @param ready callback invoked on the EDT after image preparation or fallback
     * @return the started worker, which the window can cancel during disposal
     */
    public SwingWorker<BufferedImage[], Void> prepareStartup(Runnable ready) {
        final int width = Math.max(1, getWidth()), height = Math.max(1, getHeight());
        final double[] initialWeights = weights.clone();
        SwingWorker<BufferedImage[], Void> worker = new SwingWorker<BufferedImage[], Void>() {
            @Override
            protected BufferedImage[] doInBackground() {
                SceneRenderState.beginRender(initialWeights);
                try {
                    BufferedImage background = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = background.createGraphics();
                    try {
                        scenery.background(g, width, height, initialWeights);
                    } finally {
                        g.dispose();
                    }
                    BufferedImage foreground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
                    g = foreground.createGraphics();
                    try {
                        scenery.lastLightForeground(g, width, height);
                    } finally {
                        g.dispose();
                    }
                    return new BufferedImage[] { background, foreground };
                } finally {
                    SceneRenderState.endRender();
                }
            }

            @Override
            protected void done() {
                if (isCancelled())
                    return;
                try {
                    BufferedImage[] layers = get();
                    backgroundCache = layers[0];
                    foregroundCache = layers[1];
                    cachedWeights = initialWeights;
                } catch (Exception error) {
                    artworkFailed = true;
                    System.err.println("Login artwork unavailable; using plain background.");
                }
                startupPrepared = true;
                ready.run();
            }
        };
        worker.execute();
        return worker;
    }

    public SunsetLoginPanel(JPanel form) {
        this(form, 470);
    }

    public SunsetLoginPanel(JPanel form, int cardHeight) {
        this.form = form;
        this.cardHeight = cardHeight;
        setLayout(null);
        setBackground(new Color(41, 32, 43));
        add(form);

    }

    @Override
    public void doLayout() {
        int w = Math.min(440, getWidth() - 160);
        card.setBounds((getWidth() - w) / 2, Math.max(75, (getHeight() - 556) / 2), w, cardHeight);
        form.setBounds(card.x + 36, card.y + 30, w - 72, cardHeight - 60);
    }

    public void setCardHeight(int height) {
        cardHeight = height;
        doLayout();
        repaint();
    }

    public Rectangle cardBounds() {
        return new Rectangle(card);
    }

    /**
     * Stationary scene layer for the authenticated workspace entrance.
     *
     * @return the current backdrop image, or null before the panel has a size
     */
    public BufferedImage captureBackdrop() {
        if (getWidth() <= 0 || getHeight() <= 0)
            return null;
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        if (artworkFailed) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            prepareStaticLayers();
            g.drawImage(backgroundCache, 0, 0, null);
            g.drawImage(foregroundCache, 0, 0, null);
        }
        g.dispose();
        return image;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        SceneRenderState.weights = weights.clone();
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (artworkFailed) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            prepareStaticLayers();
            g.drawImage(backgroundCache, 0, 0, null);
        }
        g.setColor(new Color(255, 245, 233));
        g.setFont(new Font("Georgia", Font.PLAIN, 26));
        g.drawString("Task Tracker", 36, 48);
        if (!artworkFailed)
            scenery.glass(g, card, getWidth(), getHeight(), weights, true);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(255, 239, 223, 210));
        g.drawString("A little room to focus.", 36, getHeight() - 26);
        g.dispose();
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        super.paintChildren(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        if (!artworkFailed) {
            prepareStaticLayers();
            g.drawImage(foregroundCache, 0, 0, null);
        }
        ProjectCredit.paint(g, getWidth(), getHeight());
        g.dispose();
    }

    /** Card motion must not rescale and relight the landscape and flowers every tick. */
    private void prepareStaticLayers() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0)
            return;
        if (backgroundCache != null && backgroundCache.getWidth() == w && backgroundCache.getHeight() == h
                && java.util.Arrays.equals(cachedWeights, weights))
            return;
        backgroundCache = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = backgroundCache.createGraphics();
        scenery.background(g, w, h, weights);
        g.dispose();
        foregroundCache = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        g = foregroundCache.createGraphics();
        scenery.lastLightForeground(g, w, h);
        g.dispose();
        cachedWeights = weights.clone();
    }
}
