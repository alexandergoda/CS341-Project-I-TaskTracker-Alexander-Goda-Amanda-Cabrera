package tasktracker.view.theme;

import tasktracker.view.components.PressFeedbackButton;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

public final class KeyboardPressCheck {
    private static PressFeedbackButton button;
    private static final AtomicInteger actions = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            button = new PressFeedbackButton("Sign in", true);
            button.addActionListener(e -> actions.incrementAndGet());
            button.clickWithFeedback();
            button.clickWithFeedback();
            if (!button.getModel().isPressed() || actions.get() != 0)
                throw new AssertionError("No deferred press");
        });
        Thread.sleep(60);
        SwingUtilities.invokeAndWait(() -> {
            try {
                Field f = PressFeedbackButton.class.getDeclaredField("pressAmount");
                f.setAccessible(true);
                if (f.getDouble(button) <= 0)
                    throw new AssertionError("Press did not animate");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(160);
        SwingUtilities.invokeAndWait(() -> {
            if (actions.get() != 1 || button.getModel().isPressed())
                throw new AssertionError("Repeated or stuck submission");
            button.setEnabled(false);
            button.clickWithFeedback();
            if (actions.get() != 1)
                throw new AssertionError("Disabled activation");
            button.setEnabled(true);
            button.clickWithFeedback();
            button.removeNotify();
        });
        Thread.sleep(160);
        SwingUtilities.invokeAndWait(() -> {
            if (actions.get() != 1)
                throw new AssertionError("Removed button submitted");
        });
        System.out.println(
                "PASS: animated nonblocking activation, duplicate suppression, disabled guard, removal cancellation.");
        System.exit(0);
    }
}
