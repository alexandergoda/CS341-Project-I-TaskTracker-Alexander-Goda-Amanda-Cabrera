package tasktracker.view.theme;

import java.awt.Color;

/**
 * Scene-aware surfaces for the application. Text, action accents and priority semantics remain consistent.
 */
public final class AdaptiveScenePalette {
    private AdaptiveScenePalette() {
    }

    public static Color glass() {
        Color base = blend(false);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, Math.min(245, base.getAlpha())));
    }

    private static Color glass(LastLightScene scene) {
        switch (scene) {
        case DAWN:
            return new Color(30, 42, 54, 132);
        case SUNRISE:
            return new Color(54, 43, 51, 130);
        case MIDDAY:
            return new Color(27, 34, 42, 175);
        case GOLDEN_HOUR:
            return new Color(36, 37, 46, 150);
        case DUSK:
            return new Color(40, 32, 47, 120);
        case NIGHT:
            return new Color(21, 31, 45, 110);
        default:
            throw new AssertionError(SceneTheme.selectedScene);
        }
    }

    public static Color reflection() {
        return blend(true);
    }

    private static Color reflection(LastLightScene scene) {
        switch (scene) {
        case DAWN:
            return new Color(215, 232, 246, 24);
        case SUNRISE:
            return new Color(255, 222, 211, 25);
        case MIDDAY:
            return new Color(235, 242, 250, 16);
        case GOLDEN_HOUR:
            return new Color(247, 235, 219, 20);
        case DUSK:
            return new Color(237, 218, 234, 23);
        case NIGHT:
            return new Color(210, 227, 248, 18);
        default:
            throw new AssertionError(SceneTheme.selectedScene);
        }
    }

    public static Color edge() {
        Color light = reflection();
        return new Color(light.getRed(), light.getGreen(), light.getBlue(),
                (int) Math.round(105 - 20 * SceneRenderState.renderWeights()[LastLightScene.NIGHT.index]));
    }

    public static Color selection() {
        Color accent = accent();
        return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                (int) Math.round(38 + 16 * SceneRenderState.renderWeights()[LastLightScene.MIDDAY.index]));
    }

    /**
     * Light accents keep dark button text readable; blend light, not RGB codes.
     *
     * @return the accent color for the current scene blend
     */
    public static Color accent() {
        double[] rgb = new double[3];
        for (LastLightScene scene : LastLightScene.values()) {
            Color c;
            switch (scene) {
            case DAWN:
                c = new Color(222, 184, 201);
                break;
            case SUNRISE:
                c = new Color(242, 187, 166);
                break;
            case MIDDAY:
                c = new Color(241, 198, 160);
                break;
            case GOLDEN_HOUR:
                c = new Color(235, 199, 151);
                break;
            case DUSK:
                c = new Color(225, 179, 184);
                break;
            default:
                c = new Color(222, 211, 183);
            }
            double weight = SceneRenderState.renderWeights()[scene.index];
            rgb[0] += linear(c.getRed() / 255.0) * weight;
            rgb[1] += linear(c.getGreen() / 255.0) * weight;
            rgb[2] += linear(c.getBlue() / 255.0) * weight;
        }
        return new Color(encoded(rgb[0]), encoded(rgb[1]), encoded(rgb[2]));
    }

    public static Color buttonText() {
        return new Color(33, 29, 34);
    }

    public static Color secondaryText() {
        Color c = accent(), ink = ThemePalette.INK;
        return new Color((c.getRed() + 5 * ink.getRed()) / 6, (c.getGreen() + 5 * ink.getGreen()) / 6,
                (c.getBlue() + 5 * ink.getBlue()) / 6);
    }

    public static Color fieldEdge(boolean focused) {
        Color c = focused ? accent() : reflection();
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), focused ? 200 : 75);
    }

    private static double linear(double value) {
        return value <= .04045 ? value / 12.92 : Math.pow((value + .055) / 1.055, 2.4);
    }

    private static int encoded(double value) {
        double encoded = value <= .0031308 ? value * 12.92 : 1.055 * Math.pow(value, 1 / 2.4) - .055;
        return (int) Math.round(Math.max(0, Math.min(1, encoded)) * 255);
    }

    public static Color header() {
        Color tint = glass();
        return new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 185);
    }

    /**
     * Opaque inset surface derived from the same tint as the surrounding glass.
     *
     * @return the opaque text-field surface color for the current scene blend
     */
    public static Color field() {
        Color tint = glass(), light = reflection();
        return new Color((int) (tint.getRed() * .93 + light.getRed() * .07),
                (int) (tint.getGreen() * .93 + light.getGreen() * .07),
                (int) (tint.getBlue() * .93 + light.getBlue() * .07));
    }

    private static Color blend(boolean highlight) {
        double r = 0, g = 0, b = 0, a = 0;
        for (LastLightScene scene : LastLightScene.values()) {
            double w = SceneRenderState.renderWeights()[scene.index];
            Color c = highlight ? reflection(scene) : glass(scene);
            r += c.getRed() * w;
            g += c.getGreen() * w;
            b += c.getBlue() * w;
            a += c.getAlpha() * w;
        }
        return new Color((int) Math.round(r), (int) Math.round(g), (int) Math.round(b), (int) Math.round(a));
    }
}
