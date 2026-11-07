package tasktracker.view.theme;

import java.time.LocalTime;

/** One appearance choice for the application session, not task data. */
public final class SceneTheme {
    private SceneTheme() {
    }

    /**
     * Updates renderer weights without exposing the mutable backing array.
     *
     * @param weights scene blend weights to copy for subsequent rendering
     */
    public static void setRenderWeights(double[] weights) {
        SceneRenderState.weights = weights.clone();
    }

    public static String mode = "Auto";
    public static LastLightScene selectedScene = LastLightScene.DUSK;

    /**
     * Controls use dark glass even when the landscape shows daylight.
     *
     * @return true because the application uses dark glass control surfaces
     */
    public static boolean usesDarkGlass() {
        return true;
    }

    public static double[] current() {
        if (mode.equals("Scene"))
            return selectedScene.weights();
        if (mode.equals("Dark"))
            return LastLightScene.NIGHT.weights();
        if (mode.equals("Light"))
            return LastLightScene.MIDDAY.weights();
        return SceneRenderState.at(LocalTime.now().toSecondOfDay() / 3600.0);
    }
}
