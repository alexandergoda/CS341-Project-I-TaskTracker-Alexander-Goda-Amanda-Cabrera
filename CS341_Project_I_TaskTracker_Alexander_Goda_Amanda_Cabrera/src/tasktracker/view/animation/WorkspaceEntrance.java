package tasktracker.view.animation;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

/** Card-only entrance. Landscape pixels never participate in the dissolve. */
final class WorkspaceEntrance {
    private final BufferedImage backdrop;
    private final Rectangle loginCard;
    private final Supplier<Rectangle[]> cards;

    WorkspaceEntrance(BufferedImage backdrop, Rectangle loginCard, Supplier<Rectangle[]> cards) {
        this.backdrop = backdrop;
        this.loginCard = new Rectangle(loginCard);
        this.cards = cards;
    }

    void paint(Graphics2D graphics, BufferedImage old, BufferedImage next, float progress, int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.scale((double) width / backdrop.getWidth(), (double) height / backdrop.getHeight());
        g.drawImage(backdrop, 0, 0, null);
        float exit = Math.min(1, progress / .55f);
        card(g, old, loginCard, 1 - exit, 1 - .025 * exit, -5 * exit);
        Rectangle[] incoming = cards.get();
        for (int i = 0; i < incoming.length; i++) {
            float enter = Math.max(0, Math.min(1, (progress - .16f - i * .06f) / (.84f - i * .06f)));
            card(g, next, incoming[i], enter, 1, 12 * (1 - enter));
        }
        // Header and footer content change independently, without fading the scenery.
        band(g, old, 0, 65, 1 - progress);
        band(g, next, 0, 65, progress);
        int bottom = backdrop.getHeight() - 45;
        band(g, old, bottom, 45, 1 - progress);
        band(g, next, bottom, 45, progress);
        g.dispose();
    }

    private void band(Graphics2D g, BufferedImage image, int y, int height, float alpha) {
        Graphics2D band = (Graphics2D) g.create();
        band.clipRect(0, y, image.getWidth(), height);
        band.setComposite(AlphaComposite.SrcOver.derive(alpha));
        band.drawImage(image, 0, 0, null);
        band.dispose();
    }

    private void card(Graphics2D g, BufferedImage image, Rectangle r, float alpha, double scale, double dy) {
        if (alpha <= 0 || r.width <= 0 || r.height <= 0)
            return;
        Graphics2D layer = (Graphics2D) g.create();
        layer.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        layer.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        layer.translate(r.getCenterX(), r.getCenterY() + dy);
        layer.scale(scale, scale);
        layer.translate(-r.getCenterX(), -r.getCenterY());
        layer.clip(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 26, 26));
        layer.setComposite(AlphaComposite.SrcOver.derive(alpha));
        layer.drawImage(image, 0, 0, null);
        layer.dispose();
    }
}
