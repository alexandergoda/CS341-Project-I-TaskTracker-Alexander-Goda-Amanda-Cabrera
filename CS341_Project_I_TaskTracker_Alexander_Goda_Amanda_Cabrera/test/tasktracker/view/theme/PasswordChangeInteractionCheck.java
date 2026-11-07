package tasktracker.view.theme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import javax.swing.SwingUtilities;
import tasktracker.controller.*;
import tasktracker.data.*;
import tasktracker.view.AppWorkspace;
import tasktracker.view.screens.*;

/** Exercises the actual account button and password form without real user data. */
public final class PasswordChangeInteractionCheck {
    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void check(boolean value, String message) {
        if (!value)
            throw new AssertionError(message);
    }

    private static PasswordChangeSurface form(DashboardSurface dashboard) throws Exception {
        return (PasswordChangeSurface) field(field(dashboard, "focusHost"), "current");
    }

    private static void choosePassword(DashboardSurface dashboard) throws Exception {
        dashboard.account.doClick(0);
        ((javax.swing.JButton) field(dashboard.account, "changePassword")).doClick(0);
    }

    public static void main(String[] args) throws Exception {
        DatabaseManager db = new DatabaseManager(
                Files.createTempDirectory("password-ui-").resolve("test.db").toString());
        db.initializeDatabase();
        AuthController auth = new AuthController(new UserDAO(db));
        auth.register("owner", "old");
        auth.login("owner", "old");
        SwingUtilities.invokeAndWait(() -> {
            try {
                AppWorkspace workspace = new AppWorkspace(auth, new TaskController(new TaskDAO(db), auth));
                Method show = AppWorkspace.class.getDeclaredMethod("showDashboard");
                show.setAccessible(true);
                show.invoke(workspace);
                DashboardSurface dashboard = (DashboardSurface) field(workspace, "dashboard");
                choosePassword(dashboard);
                PasswordChangeSurface form = form(dashboard);
                form.current.setText("wrong");
                form.replacement.setText("new");
                form.confirmation.setText("new");
                form.save.doClick(0);
                check(form.feedback.getText().contains("incorrect"), "Wrong password was not rejected");
                check(auth.isLoggedIn(), "Failure cleared session");
                form.current.setText("old");
                form.save.doClick(0);
                check(form.current.getPassword().length == 0 && form.replacement.getPassword().length == 0
                        && form.confirmation.getPassword().length == 0, "Success left passwords in fields");
                check(field(field(dashboard, "focusHost"), "current") == field(dashboard, "focusContent"),
                        "Success did not return to focus");
                check(auth.login("owner", "new"), "New password failed");
                choosePassword(dashboard);
                form = form(dashboard);
                form.current.setText("new");
                form.cancel.doClick(0);
                check(form.current.getPassword().length == 0, "Cancel left password in field");
                dashboard.newTask.doClick(0);
                InlineEditorSurface editor = (InlineEditorSurface) field(workspace, "editor");
                editor.title.setText("Unsaved task");
                choosePassword(dashboard);
                check(field(workspace, "pending") != null, "Account bypassed task draft guard");
                editor.discard.doClick(0);
                check(field(workspace, "editor") == null && form(dashboard) != null,
                        "Discard did not open account form");
                dashboard.removeNotify();
                workspace.removeNotify();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        System.out.println("PASS: account form, wrong password, success, cancel, field clearing and task draft guard.");
        System.exit(0);
    }
}
