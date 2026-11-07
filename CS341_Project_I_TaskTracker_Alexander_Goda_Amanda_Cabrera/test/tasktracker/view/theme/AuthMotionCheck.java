package tasktracker.view.theme;

import tasktracker.view.screens.AuthSurface;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/** Authentication feedback regression and render cost; no accounts or database involved. */
public final class AuthMotionCheck {
    public static void main(String[] args) throws Exception {
        AuthSurface[] surface = new AuthSurface[1];
        Color[] errorColor = new Color[1];
        SwingUtilities.invokeAndWait(() -> {
            SceneTheme.mode = "Light";
            AuthSurface auth = new AuthSurface();
            auth.scenePanel.setSize(1280, 820);
            auth.scenePanel.doLayout();
            surface[0] = auth;
            errorColor[0] = auth.feedbackLabel.getForeground();
            BufferedImage frame = new BufferedImage(1280, 820, BufferedImage.TYPE_INT_RGB);
            long[] times = new long[60];
            for (int i = -5; i < 60; i++) {
                long start = System.nanoTime();
                auth.scenePanel.setCardHeight(470 + (int) (86 * Math.max(0, i) / 59.0));
                Graphics2D g = frame.createGraphics();
                auth.scenePanel.paint(g);
                g.dispose();
                if (i >= 0)
                    times[i] = System.nanoTime() - start;
            }
            Arrays.sort(times);
            System.out.printf("Auth morph rendering median %.2f ms, p95 %.2f ms%n", times[30] / 1e6, times[57] / 1e6);
        });
        AuthSurface auth = surface[0];
        try {
            transition(auth, true, false, true);
            SwingUtilities.invokeAndWait(() -> {
                if (!"Account created. Sign in to continue.".equals(auth.feedbackLabel.getText())
                        || errorColor[0].equals(auth.feedbackLabel.getForeground()))
                    throw new AssertionError("Registration success did not retain its success feedback");
                auth.showError("Invalid username or password.");
                if (!"Invalid username or password.".equals(auth.feedbackLabel.getText())
                        || !errorColor[0].equals(auth.feedbackLabel.getForeground()))
                    throw new AssertionError("Authentication error retained the registration success color");
            });
            transition(auth, true, false, true);
            transition(auth, false, true, false);
            SwingUtilities.invokeAndWait(() -> {
                if (!auth.feedbackLabel.getText().isEmpty()
                        || !errorColor[0].equals(auth.feedbackLabel.getForeground()))
                    throw new AssertionError("Changing authentication mode retained prior success feedback");
            });
            System.out.println("PASS: registration success, subsequent error color and mode-change reset");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                auth.stop();
                auth.scenePanel.removeNotify();
            });
        }
        System.exit(0);
    }

    private static void transition(AuthSurface auth, boolean from, boolean target, boolean created) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> auth.transition(from, target, created, done::countDown));
        if (!done.await(3, TimeUnit.SECONDS))
            throw new AssertionError("Authentication transition did not finish within three seconds");
    }
}
