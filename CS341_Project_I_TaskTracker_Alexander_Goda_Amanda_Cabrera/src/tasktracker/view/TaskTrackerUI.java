package tasktracker.view;

import java.util.Objects;
import javax.swing.SwingUtilities;
import tasktracker.controller.AuthController;
import tasktracker.controller.TaskController;

/**
 * Single integration entry point for the desktop UI. The caller supplies initialized controllers; this class never
 * creates or replaces a database. Call once after successful application initialization.
 */
public final class TaskTrackerUI {
    private TaskTrackerUI() {
    }

    public static void launch(AuthController auth, TaskController tasks) {
        Objects.requireNonNull(auth, "auth controller");
        Objects.requireNonNull(tasks, "task controller");
        Runnable open = () -> new ApplicationWindow(auth, tasks).setVisible(true);
        if (SwingUtilities.isEventDispatchThread()) {
            open.run();
        } else {
            SwingUtilities.invokeLater(open);
        }
    }
}
