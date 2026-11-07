package tasktracker.view.theme;

import tasktracker.view.animation.ThemeMotion;

import java.awt.*;
import java.awt.image.*;
import java.util.Arrays;
import javax.swing.*;

/**
 * Keeps image preparation off Swing's event thread. During a fade, only cached full-size bitmaps and the small visible
 * flower mask are blended. Tasks and text remain live Swing controls, never captured in these images.
 */
public final class CachedSceneTransition {
    private final Runnable repaint;
    private final SceneRenderer renderer = new SceneRenderer();
    private final java.util.function.BiFunction<SceneRenderer, Request, Frame> prepareFrame;
    private Frame current, from, to;
    private BufferedImage flowerBlend;
    private Timer animation;
    private SwingWorker<Frame, Void> worker;
    private Request pending;
    private long version;
    private float progress;
    private boolean disposed;

    public CachedSceneTransition(Runnable repaint) {
        this(repaint, CachedSceneTransition::render);
    }

    /** Injectable preparation keeps failure handling testable without real assets. */
    CachedSceneTransition(Runnable repaint, java.util.function.BiFunction<SceneRenderer, Request, Frame> prepareFrame) {
        this.repaint = repaint;
        this.prepareFrame = prepareFrame;
    }

    public boolean isReady(int width, int height) {
        return current != null && current.background.getWidth() == width && current.background.getHeight() == height
                && worker == null;
    }

    public void request(int width, int height, Rectangle[] cards, double[] weights) {
        if (width <= 0 || height <= 0)
            return;
        Request next = new Request(width, height, cards, weights);
        if (pending != null && pending.same(next))
            return;
        disposed = false;
        if (animation != null) {
            animation.stop();
            current = snapshot();
            from = null;
            to = null;
            flowerBlend = null;
        }
        pending = next;
        version++;
        if (worker == null)
            prepare();
    }

    private void prepare() {
        final Request request = pending;
        final long ticket = version;
        worker = new SwingWorker<Frame, Void>() {
            protected Frame doInBackground() {
                return prepareFrame.apply(renderer, request);
            }

            protected void done() {
                worker = null;
                if (disposed)
                    return;
                if (ticket != version) {
                    prepare();
                    return;
                }
                try {
                    Frame ready = get();
                    if (current == null || current.background.getWidth() != request.width
                            || current.background.getHeight() != request.height) {
                        current = ready;
                        SceneRenderState.weights = ready.weights.clone();
                        repaint.run();
                        return;
                    }
                    from = current;
                    to = ready;
                    progress = 0;
                    flowerBlend = new BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB_PRE);
                    animation = ThemeMotion.animate(420, t -> {
                        progress = (float) t;
                        for (int i = 0; i < ready.weights.length; i++)
                            SceneRenderState.weights[i] = from.weights[i] + (ready.weights[i] - from.weights[i]) * t;
                        blendFlowers();
                        repaint.run();
                        if (t >= 1) {
                            current = to;
                            from = null;
                            to = null;
                            flowerBlend = null;
                            animation = null;
                        }
                    });
                } catch (Exception error) {
                    // A usable frame must exist even on first-load failure, otherwise
                    // the screen transition waits forever for readiness.
                    System.err.println("Scene preparation failed: " + error.getMessage());
                    current = fallback(request);
                    SceneRenderState.weights = current.weights.clone();
                    repaint.run();
                }
            }
        };
        worker.execute();
    }

    private Frame fallback(Request request) {
        BufferedImage bg = new BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_RGB);
        BufferedImage fg = new BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = bg.createGraphics();
        g.setColor(new Color(25, 34, 48));
        g.fillRect(0, 0, request.width, request.height);
        if (current != null)
            g.drawImage(current.background, 0, 0, request.width, request.height, null);
        g.dispose();
        if (current != null) {
            g = fg.createGraphics();
            g.drawImage(current.foreground, 0, 0, request.width, request.height, null);
            g.dispose();
        }
        return new Frame(bg, fg, current == null ? request.weights : current.weights);
    }

    static Frame render(SceneRenderer renderer, Request request) {
        SceneRenderState.beginRender(request.weights);
        try {
            BufferedImage background = new BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = background.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            renderer.background(g, request.width, request.height, request.weights);
            g.setColor(AdaptiveScenePalette.header());
            g.fillRect(0, 0, request.width, 60);
            for (Rectangle card : request.cards)
                renderer.glass(g, card, request.width, request.height, request.weights, true);
            g.dispose();
            BufferedImage foreground = new BufferedImage(request.width, request.height,
                    BufferedImage.TYPE_INT_ARGB_PRE);
            g = foreground.createGraphics();
            renderer.lastLightForeground(g, request.width, request.height);
            g.dispose();
            return new Frame(background, foreground, request.weights);
        } finally {
            SceneRenderState.endRender();
        }
    }

    public void background(Graphics2D g, int w, int h) {
        if (from != null) {
            draw(g, from.background, w, h, 1);
            draw(g, to.background, w, h, progress);
        } else if (current != null)
            draw(g, current.background, w, h, 1);
    }

    public void foreground(Graphics2D g, int w, int h) {
        BufferedImage image = from != null ? flowerBlend : current == null ? null : current.foreground;
        if (image != null)
            draw(g, image, w, h, 1);
    }

    private static void draw(Graphics2D g, BufferedImage image, int w, int h, float alpha) {
        Composite before = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.drawImage(image, 0, 0, w, h, null);
        g.setComposite(before);
    }

    private void blendFlowers() {
        int[] a = from.pixels, b = to.pixels, out = ((DataBufferInt) flowerBlend.getRaster().getDataBuffer()).getData();
        int mix = Math.round(progress * 256), inverse = 256 - mix;
        // Same geometry and alpha in every scene. Premultiplied interpolation
        // avoids dark halos and avoids double-compositing translucent petal edges.
        for (int i : to.visible) {
            int aa = a[i], bb = b[i], result = 0;
            for (int shift = 0; shift <= 24; shift += 8)
                result |= (((((aa >>> shift) & 255) * inverse + ((bb >>> shift) & 255) * mix + 128) >> 8) << shift);
            out[i] = result;
        }
    }

    private Frame snapshot() {
        if (from == null)
            return current;
        BufferedImage bg = new BufferedImage(from.background.getWidth(), from.background.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bg.createGraphics();
        background(g, bg.getWidth(), bg.getHeight());
        g.dispose();
        BufferedImage fg = new BufferedImage(bg.getWidth(), bg.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        g = fg.createGraphics();
        g.drawImage(flowerBlend, 0, 0, null);
        g.dispose();
        return new Frame(bg, fg, SceneRenderState.weights);
    }

    public void dispose() {
        disposed = true;
        version++;
        pending = null;
        if (animation != null)
            animation.stop();
        animation = null;
        from = null;
        to = null;
        flowerBlend = null;
    }

    static final class Frame {
        final BufferedImage background, foreground;
        final double[] weights;
        final int[] pixels, visible;

        Frame(BufferedImage bg, BufferedImage fg, double[] weights) {
            background = bg;
            foreground = fg;
            this.weights = weights.clone();
            pixels = ((DataBufferInt) fg.getRaster().getDataBuffer()).getData();
            visible = java.util.stream.IntStream.range(0, pixels.length).filter(i -> (pixels[i] >>> 24) > 0).toArray();
        }
    }

    static final class Request {
        final int width, height;
        final Rectangle[] cards;
        final double[] weights;

        Request(int w, int h, Rectangle[] cards, double[] weights) {
            width = w;
            height = h;
            this.cards = Arrays.stream(cards).map(Rectangle::new).toArray(Rectangle[]::new);
            this.weights = weights.clone();
        }

        boolean same(Request other) {
            return width == other.width && height == other.height && Arrays.equals(cards, other.cards)
                    && Arrays.equals(weights, other.weights);
        }
    }
}
