package tasktracker.view.animation;

import tasktracker.view.theme.*;
import tasktracker.view.screens.AuthSurface;
import tasktracker.view.screens.DashboardSurface;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import javax.swing.*;
import tasktracker.controller.*;
import tasktracker.data.*;
import tasktracker.view.AppWorkspace;

public final class WorkspaceEntranceCheck {
    private static AppWorkspace workspace;
    private static AuthSurface login;

    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static BufferedImage solid(Color c) {
        BufferedImage i = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = i.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, 640, 480);
        g.dispose();
        return i;
    }

    public static void main(String[] args) throws Exception {

        WorkspaceEntrance effect = new WorkspaceEntrance(solid(Color.BLUE), new Rectangle(220, 100, 180, 260),
                () -> new Rectangle[] { new Rectangle(30, 100, 240, 260), new Rectangle(310, 100, 300, 260) });
        for (float t : new float[] { 0, .2f, .5f, .8f, 1 }) {
            BufferedImage frame = solid(Color.BLACK);
            Graphics2D g = frame.createGraphics();
            effect.paint(g, solid(Color.RED), solid(Color.GREEN), t, 640, 480);
            g.dispose();
            if (frame.getRGB(10, 250) != Color.BLUE.getRGB())
                throw new AssertionError("Scenery faded");
        }
        DatabaseManager db = new DatabaseManager(
                Files.createTempDirectory("entrance-check-").resolve("test.db").toString());
        db.initializeDatabase();
        AuthController auth = new AuthController(new UserDAO(db));
        auth.register("entrance-user", "test-password");
        TaskController tasks = new TaskController(new TaskDAO(db), auth);
        SwingUtilities.invokeAndWait(() -> {
            try {
                SceneTheme.mode = "Light";
                workspace = new AppWorkspace(auth, tasks);
                workspace.setSize(1280, 820);
                layout(workspace);
                login = (AuthSurface) field(workspace, "login");
                login.usernameField.setText("entrance-user");
                login.passwordField.setText("test-password");
                login.passwordField.postActionEvent();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        boolean[] seen = { false, false };
        for (int i = 0; i < 80; i++) {
            Thread.sleep(50);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    if (login.loginButton.getText().equals("Signed in \u2713"))
                        seen[0] = true;
                    Object host = field(workspace, "screens");
                    if (field(host, "entrance") != null)
                        seen[1] = true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (!seen[0] || !seen[1])
                    throw new AssertionError("Sign-in did not use confirmation and card entrance");
                DashboardSurface dashboard = (DashboardSurface) field(workspace, "dashboard");
                if (dashboard == null || !dashboard.isVisible())
                    throw new AssertionError("Dashboard not revealed");
                if (login.passwordField.getPassword().length != 0)
                    throw new AssertionError("Secret not cleared");
                if (field(field(workspace, "screens"), "entrance") != null)
                    throw new AssertionError("Entrance did not finish");
                dashboard.removeNotify();
                workspace.removeNotify();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(
                "PASS: stationary scenery, actual Enter sign-in, success confirmation, card-only entrance, final live dashboard, cleared password.");
        System.exit(0);
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component x : c.getComponents())
            if (x instanceof Container)
                layout((Container) x);
    }
}
