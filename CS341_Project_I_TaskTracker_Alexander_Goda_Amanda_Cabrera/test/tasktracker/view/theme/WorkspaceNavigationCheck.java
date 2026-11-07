package tasktracker.view.theme;

import tasktracker.view.screens.DashboardSurface;
import tasktracker.view.screens.InlineEditorSurface;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import tasktracker.controller.AuthController;
import tasktracker.controller.TaskController;
import tasktracker.data.DatabaseManager;
import tasktracker.data.TaskDAO;
import tasktracker.data.UserDAO;
import tasktracker.view.AppWorkspace;

/** Exercises draft guards through active screen controls, with disposable data. */
public final class WorkspaceNavigationCheck {
    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {

        DatabaseManager db = new DatabaseManager(
                Files.createTempDirectory("navigation-check-").resolve("test.db").toString());
        db.initializeDatabase();
        AuthController auth = new AuthController(new UserDAO(db));
        auth.register("navigation-user", "test-password");
        auth.login("navigation-user", "test-password");
        TaskController tasks = new TaskController(new TaskDAO(db), auth);
        SwingUtilities.invokeAndWait(() -> {
            try {
                AppWorkspace workspace = new AppWorkspace(auth, tasks);
                Method show = AppWorkspace.class.getDeclaredMethod("showDashboard");
                show.setAccessible(true);
                show.invoke(workspace);
                DashboardSurface dashboard = (DashboardSurface) field(workspace, "dashboard");

                dashboard.newTask.doClick(0);
                ((InlineEditorSurface) field(workspace, "editor")).cancel.doClick(0);
                check(field(workspace, "editor") == null, "Empty draft must cancel immediately");
                dashboard.newTask.doClick(0);
                InlineEditorSurface editor = (InlineEditorSurface) field(workspace, "editor");
                editor.title.setText("Keep my draft");
                editor.cancel.doClick(0);
                check(field(workspace, "pending") != null, "Dirty cancel bypassed guard");
                check(!((javax.swing.JPanel) field(editor, "editingPage")).isVisible()
                        && ((javax.swing.JPanel) field(editor, "warning")).isVisible(),
                        "Confirmation must replace form and Cancel controls");
                editor.keep.doClick(0);
                check(((javax.swing.JPanel) field(editor, "editingPage")).isVisible(),
                        "Keep editing did not restore live form");
                check(field(workspace, "editor") == editor && field(workspace, "pending") == null,
                        "Keep draft did not retain editor");
                check(editor.title.getText().equals("Keep my draft"), "Draft text lost");

                editor.cancel.doClick(0);
                editor.discard.doClick(0);
                check(field(workspace, "editor") == null, "Discard did not return to focus");
                check(tasks.getRelevantTasks().isEmpty(), "Discard unexpectedly persisted draft");

                dashboard.newTask.doClick(0);
                editor = (InlineEditorSurface) field(workspace, "editor");
                editor.title.setText("Save before leaving");
                editor.due.setText("02/30/2028");
                AtomicBoolean closed = new AtomicBoolean();
                workspace.requestClose(() -> closed.set(true));
                editor.saveLeave.doClick(0);
                check(!closed.get() && field(workspace, "editor") == editor, "Invalid draft closed workspace");
                check(field(workspace, "pending") != null, "Failed save lost navigation intent");
                check(((javax.swing.JPanel) field(editor, "editingPage")).isVisible(),
                        "Invalid save must return to editable fields");
                editor.due.setText("02/29/2028");
                editor.save.doClick(0);
                check(closed.get() && tasks.getRelevantTasks().size() == 1,
                        "Save and leave did not persist then continue");

                dashboard.edit.doClick(0);
                editor = (InlineEditorSurface) field(workspace, "editor");
                editor.title.setText("Unsaved logout change");
                dashboard.logout.doClick(0);
                check(auth.getCurrentUser() != null, "Logout bypassed dirty draft");
                editor.keep.doClick(0);
                dashboard.logout.doClick(0);
                editor.discard.doClick(0);
                check(auth.getCurrentUser() == null && field(workspace, "dashboard") == null,
                        "Discard and logout did not return to login");
                check(field(workspace, "login") != null, "Missing login surface");
                dashboard.removeNotify();
                workspace.removeNotify();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        System.out.println(
                "PASS: dirty cancel, keep/discard, close guard, failed save recovery, save-and-leave, guarded logout.");
        System.exit(0);
    }
}
