package tasktracker.view.components;

import tasktracker.view.theme.LastLightScene;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Small resolution-independent line icons, consistent with the appearance switch. */
final class SceneIcon implements Icon {
    private final LastLightScene scene;
    private final Color color;

    SceneIcon(LastLightScene scene, Color color) {
        this.scene = scene;
        this.color = color;
    }

    public int getIconWidth() {
        return 24;
    }

    public int getIconHeight() {
        return 24;
    }

    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(color);
        if (scene == LastLightScene.NIGHT) {
            Area moon = new Area(new Ellipse2D.Double(4, 3, 16, 17));
            moon.subtract(new Area(new Ellipse2D.Double(10, 0, 15, 16)));
            g.fill(moon);
        } else if (scene == LastLightScene.MIDDAY) {
            g.drawOval(8, 8, 8, 8);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                g.draw(new Line2D.Double(12 + 7 * Math.cos(a), 12 + 7 * Math.sin(a), 12 + 10 * Math.cos(a),
                        12 + 10 * Math.sin(a)));
            }
        } else {
            int horizon = scene == LastLightScene.DUSK ? 13 : 17;
            g.drawLine(3, horizon, 21, horizon);
            g.drawArc(7, horizon - 6, 10, 10, 0, 180);
            g.drawLine(12, horizon - 11, 12, horizon - 9);
            g.drawLine(4, horizon - 7, 6, horizon - 5);
            g.drawLine(18, horizon - 5, 20, horizon - 7);
            if (scene == LastLightScene.DAWN) {
                g.drawLine(6, 20, 18, 20);
            }
            if (scene == LastLightScene.SUNRISE) {
                g.drawLine(12, 22, 12, 19);
                g.drawLine(10, 21, 12, 19);
                g.drawLine(14, 21, 12, 19);
            }
            if (scene == LastLightScene.GOLDEN_HOUR) {
                g.drawLine(2, 10, 4, 10);
                g.drawLine(20, 10, 22, 10);
            }
            if (scene == LastLightScene.DUSK) {
                g.drawLine(12, 17, 12, 22);
                g.drawLine(10, 20, 12, 22);
                g.drawLine(14, 20, 12, 22);
            }
        }
        g.dispose();
    }
}
