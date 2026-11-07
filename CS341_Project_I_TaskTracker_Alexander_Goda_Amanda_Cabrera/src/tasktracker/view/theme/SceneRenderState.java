package tasktracker.view.theme;

/** Current scene blend and isolated worker-render weights; never persisted to tasks. */
final class SceneRenderState {
    static double[] weights = LastLightScene.DUSK.weights();
    private static final ThreadLocal<double[]> renderingWeights = new ThreadLocal<>();

    static double[] renderWeights() {
        double[] local = renderingWeights.get();
        return local == null ? weights : local;
    }

    static void beginRender(double[] value) {
        renderingWeights.set(value);
    }

    static void endRender() {
        renderingWeights.remove();
    }

    private static final double[] HOURS = { 5, 6.5, 12, 17, 19, 23 };

    private SceneRenderState() {
    }

    static double[] at(double hour) {
        hour = ((hour % 24) + 24) % 24;
        LastLightScene[] scenes = LastLightScene.values();
        double[] result = new double[scenes.length];
        int left = scenes.length - 1;
        for (int i = 0; i < HOURS.length; i++)
            if (hour >= HOURS[i])
                left = i;
        int right = (left + 1) % scenes.length;
        double start = HOURS[left], end = HOURS[right], now = hour;
        if (right == 0) {
            end += 24;
            if (now < start)
                now += 24;
        }
        double t = (now - start) / (end - start);
        t = t * t * (3 - 2 * t);
        result[scenes[left].index] = 1 - t;
        result[scenes[right].index] = t;
        return result;
    }
}
