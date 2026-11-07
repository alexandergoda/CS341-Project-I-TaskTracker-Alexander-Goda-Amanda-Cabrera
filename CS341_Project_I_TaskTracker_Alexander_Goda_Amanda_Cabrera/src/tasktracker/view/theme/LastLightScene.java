package tasktracker.view.theme;

/** Lighting presets for one fixed landscape and foreground. */
public enum LastLightScene {
    DAWN("Dawn", "dawn", 4), SUNRISE("Sunrise", "sunrise", 2), MIDDAY("Midday", "midday", 3),
    GOLDEN_HOUR("Golden hour", "golden-hour", 5), DUSK("Dusk", "dusk", 0), NIGHT("Night", "night", 1);

    public final String label;
    public final int index;
    private final String assetName;

    LastLightScene(String label, String assetName, int index) {
        this.label = label;
        this.assetName = assetName;
        this.index = index;
    }

    public String asset(String layer) {
        return "scenes/" + layer + "-" + assetName;
    }

    public double[] weights() {
        double[] weights = new double[values().length];
        weights[index] = 1;
        return weights;
    }

    public static LastLightScene forIndex(int index) {
        for (LastLightScene scene : values())
            if (scene.index == index)
                return scene;
        throw new IllegalArgumentException("No Last Light artwork for scene " + index);
    }
}
