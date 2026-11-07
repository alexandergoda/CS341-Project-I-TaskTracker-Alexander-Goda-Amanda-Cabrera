package tasktracker.view.animation;

import javax.swing.Timer;
import java.util.function.DoubleConsumer;

/** Time-based easing shared by UI transitions. Call on the Swing event thread. */
public final class ThemeMotion {
    private ThemeMotion() {
    }

    public static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    public static Timer animate(int milliseconds, DoubleConsumer frame) {
        long start = System.nanoTime();
        Timer timer = new Timer(16, null);
        timer.addActionListener(e -> {
            double t = Math.min(1, (System.nanoTime() - start) / (milliseconds * 1000000.0));
            frame.accept(ease(t));
            if (t >= 1)
                timer.stop();
        });
        timer.setCoalesce(true);
        frame.accept(0);
        timer.start();
        return timer;
    }
}
