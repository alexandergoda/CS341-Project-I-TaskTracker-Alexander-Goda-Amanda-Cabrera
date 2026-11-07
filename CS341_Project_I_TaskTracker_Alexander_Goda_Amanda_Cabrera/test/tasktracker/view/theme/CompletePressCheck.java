package tasktracker.view.theme;

import tasktracker.view.components.PressFeedbackButton;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

public final class CompletePressCheck {
    private static PressFeedbackButton button;
    private static final AtomicInteger completed = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            button = new PressFeedbackButton("Sign in", true);
            button.addActionListener(e -> button.afterRelease(() -> {
                try {
                    Field f = PressFeedbackButton.class.getDeclaredField("pressAmount");
                    f.setAccessible(true);
                    if (f.getDouble(button) > .001)
                        throw new AssertionError("Action ran before return");
                    completed.incrementAndGet();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }));
            button.clickWithFeedback();
            button.clickWithFeedback();
        });
        Thread.sleep(400);
        SwingUtilities.invokeAndWait(() -> {
            if (completed.get() != 1)
                throw new AssertionError("Keyboard did not complete once");
            button.getModel().setArmed(true);
            button.getModel().setPressed(true);
            button.getModel().setPressed(false);
            button.getModel().setArmed(false);
            if (completed.get() != 1)
                throw new AssertionError("Mouse skipped return animation");
        });
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(() -> {
            if (completed.get() != 2)
                throw new AssertionError("Mouse did not complete once");
            button.clickWithFeedback();
            button.removeNotify();
        });
        Thread.sleep(300);
        if (completed.get() != 2)
            throw new AssertionError("Detached button ran callback");
        System.out.println(
                "PASS: mouse and keyboard return to resting size before action, single callback, removal cancellation.");
        System.exit(0);
    }
}
