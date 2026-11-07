package tasktracker.view.theme;

/** Runs with either the assets directory or an artwork JAR on the classpath. */
public final class PortableAssetsCheck {
    public static void main(String[] args) {
        for (LastLightScene scene : LastLightScene.values())
            ThemeAssets.image(scene.asset("background"));
        ThemeAssets.image("scenes/foreground-dusk");
        try {
            ThemeAssets.image("../outside");
            throw new AssertionError("Traversal accepted");
        } catch (IllegalArgumentException expected) {
            // Resource names cannot escape their intended root.
        }
        try {
            ThemeAssets.image("missing-resource");
            throw new AssertionError("Missing artwork accepted");
        } catch (IllegalStateException expected) {
            if (!expected.getMessage().contains("classpath"))
                throw new AssertionError("Missing resource needs an actionable message");
        }
        System.out.println("PASS: all scene artwork loads from classpath; invalid resources fail clearly.");
    }
}
