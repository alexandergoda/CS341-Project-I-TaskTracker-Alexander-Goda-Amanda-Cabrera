package tasktracker.view.theme;

import tasktracker.view.components.AppearanceControl;
import tasktracker.view.screens.DashboardSurface;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import tasktracker.controller.*;
import tasktracker.data.*;
import tasktracker.model.*;
import tasktracker.view.AppWorkspace;
import tasktracker.view.screens.InlineEditorSurface;

/** Regression checks against the real workspace, using a disposable SQLite file. */
public final class ActiveWorkspaceCheck {
    private static AppWorkspace workspace;
    private static DashboardSurface dashboard;
    private static CachedSceneTransition scene;
    private static final AtomicBoolean removedCallback = new AtomicBoolean();
    private static final AtomicInteger scenePreparations = new AtomicInteger();
    private static final Color RECOVERED_SCENE = new Color(51, 74, 99);

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
                Files.createTempDirectory("active-workspace-").resolve("test.db").toString());
        db.initializeDatabase();
        AuthController auth = new AuthController(new UserDAO(db));
        auth.register("test-user", "test-password");
        auth.login("test-user", "test-password");
        TaskController tasks = new TaskController(new TaskDAO(db), auth);
        tasks.createTask("Later", "", null, LocalDate.now().plusDays(3), Priority.LOW);
        Task earlier = tasks.createTask("Earlier", "", null, LocalDate.now(), Priority.HIGH);

        SwingUtilities.invokeAndWait(() -> {
            try {
                SceneTheme.mode = "Light";
                AppearanceControl appearance = new AppearanceControl(mode -> {
                });
                JToggleButton manual = (JToggleButton) field(appearance, "manual");
                check(!manual.isSelected() && ((Float) field(manual, "position")) == 0, "Light knob mismatch");
                appearance.removeNotify();

                workspace = new AppWorkspace(auth, tasks);
                Method show = AppWorkspace.class.getDeclaredMethod("showDashboard");
                show.setAccessible(true);
                show.invoke(workspace);
                dashboard = (DashboardSurface) field(workspace, "dashboard");
                check(dashboard.model.getValueAt(0, 1).equals("Earlier"), "Active workspace is not date sorted");
                dashboard.table.setRowSelectionInterval(0, 0);
                dashboard.table.getActionMap().get("toggleTaskCompletion")
                        .actionPerformed(new ActionEvent(dashboard.table, 0, "test"));
                check(new TaskDAO(db).findById(earlier.getTaskId()).isCompleted(), "Space did not persist completion");

                DashboardSurface removed = new DashboardSurface("removed");
                removed.model.addRow(new Object[] { 1, "Task", "test-user", "Unassigned", "", Priority.LOW, "Open" });
                removed.animateCompletion(0, true, () -> removedCallback.set(true));
                Timer timer = (Timer) field(removed, "completionMotion");
                removed.removeNotify();
                check(!timer.isRunning(), "Removed screen retained animation");

                scene = new CachedSceneTransition(() -> {
                }, (renderer, request) -> {
                    if (scenePreparations.incrementAndGet() == 1)
                        throw new IllegalStateException("Intentional transient failure injection");
                    BufferedImage background = new BufferedImage(request.width, request.height,
                            BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = background.createGraphics();
                    g.setColor(RECOVERED_SCENE);
                    g.fillRect(0, 0, request.width, request.height);
                    g.dispose();
                    return new CachedSceneTransition.Frame(background,
                            new BufferedImage(request.width, request.height, BufferedImage.TYPE_INT_ARGB_PRE),
                            request.weights);
                });
                scene.request(320, 240, new Rectangle[0], new double[] { 1, 0, 0, 0, 0, 0 });
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        });
        // Wait off the EDT so the completion animation and worker can finish.
        Thread.sleep(1000);
        SwingUtilities.invokeAndWait(() -> {
            check(!removedCallback.get(), "Removed screen fired completion callback");
            check(scene.isReady(320, 240), "Failed initial scene left entrance waiting");
            check(scenePreparations.get() == 1, "Scene failure fixture did not prepare exactly once");
            scene.request(320, 240, new Rectangle[0], new double[] { 1, 0, 0, 0, 0, 0 });
            check(!scene.isReady(320, 240), "Identical request did not retry failed scene preparation");
            check(dashboard.model.getRowCount() == 1, "Completed task was not hidden");
            check(dashboard.completedToggle.isVisible(), "Completed filter unavailable");
            dashboard.completedToggle.doClick();
            check(dashboard.model.getRowCount() == 2, "Show completed did not reload");
            check(dashboard.model.getValueAt(1, 1).equals("Earlier"), "Completed task was not last");
            dashboard.table.setRowSelectionInterval(1, 1);
            dashboard.complete.doClick();
        });
        Thread.sleep(800);
        awaitSceneRecovery();
        SwingUtilities.invokeAndWait(() -> {
            try {
                check(scenePreparations.get() == 2, "Identical scene retry did not run exactly once");
                BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                scene.background(g, 320, 240);
                g.dispose();
                check(image.getRGB(0, 0) == RECOVERED_SCENE.getRGB(),
                        "Retry retained the fallback instead of the prepared frame");
                scene.request(320, 240, new Rectangle[0], new double[] { 1, 0, 0, 0, 0, 0 });
                check(scene.isReady(320, 240) && scenePreparations.get() == 2,
                        "Successful identical scene request was not reused");
                check(!new TaskDAO(db).findById(earlier.getTaskId()).isCompleted(), "Mark open did not persist");
                check(dashboard.model.getValueAt(0, 1).equals("Earlier"), "Reopened task did not return to date order");
                scene.dispose();
                dashboard.removeNotify();
                workspace.removeNotify();
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        });
        checkCompletionAfterDraftSave(false);
        checkCompletionAfterDraftSave(true);
        checkEmptyTaskStates();
        checkDeselectionWithoutDraft();
        for (boolean editing : new boolean[] { false, true })
            for (String choice : new String[] { "Keep", "Discard", "Save" })
                checkDeselectionWithDraft(editing, choice);
        System.out.println("PASS: active workspace ordering, completion, filter, reopening, theme restoration, "
                + "removal cleanup, scene failure fallback and identical-request recovery, "
                + "correct completion identity after saving new/edit drafts, "
                + "empty-account/hidden-completed guidance, and guarded deselection/reselection with new/edit "
                + "Keep/Discard/Save and invalid-save corrected retry.");
        System.exit(0);
    }

    private static void awaitSceneRecovery() throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            boolean[] ready = { false };
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ready[0] = scene.isReady(320, 240) && field(scene, "animation") == null;
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
            if (ready[0])
                return;
            Thread.sleep(20);
        }
        throw new AssertionError("Retried scene did not finish preparing and fading");
    }

    /** Deselection must update the same logical task and action state as selection. */
    private static void checkDeselectionWithoutDraft() throws Exception {
        DatabaseManager db = new DatabaseManager(
                Files.createTempDirectory("deselect-workspace-").resolve("test.db").toString());
        db.initializeDatabase();
        AuthController auth = new AuthController(new UserDAO(db));
        auth.register("selection-user", "test-password");
        auth.login("selection-user", "test-password");
        TaskController tasks = new TaskController(new TaskDAO(db), auth);
        Task target = tasks.createTask("Selected task", "Saved notes", null, null, Priority.MEDIUM);
        SwingUtilities.invokeAndWait(() -> {
            AppWorkspace active = null;
            DashboardSurface surface = null;
            try {
                active = new AppWorkspace(auth, tasks);
                Method show = AppWorkspace.class.getDeclaredMethod("showDashboard");
                show.setAccessible(true);
                show.invoke(active);
                surface = (DashboardSurface) field(active, "dashboard");
                check(surface.table.getSelectedRow() == 0, "Deselection fixture has no selected row");
                surface.table.changeSelection(0, 0, true, false);
                checkClearedSelection(active, surface, "No draft");
                check(!new TaskDAO(db).findById(target.getTaskId()).isCompleted(),
                        "Deselection changed the persisted task");
                surface.table.setRowSelectionInterval(0, 0);
                check((Integer) field(active, "selectedId") == target.getTaskId()
                        && surface.table.getSelectedRow() == 0,
                        "Reselecting a task did not restore its logical selection");
                check(detailText(surface, "title").equals("Selected task") && surface.edit.isEnabled()
                        && surface.delete.isEnabled() && surface.complete.isEnabled(),
                        "Reselecting a task did not restore its detail/actions");
            } catch (Exception error) {
                throw new RuntimeException(error);
            } finally {
                if (surface != null)
                    surface.removeNotify();
                if (active != null)
                    active.removeNotify();
            }
        });
    }

    /** New/edit drafts must resolve Keep/Discard/Save before a deselection commits. */
    private static void checkDeselectionWithDraft(boolean editing, String choice) throws Exception {
        DatabaseManager db = new DatabaseManager(
                Files.createTempDirectory("deselect-draft-").resolve("test.db").toString());
        db.initializeDatabase();
        AuthController auth = new AuthController(new UserDAO(db));
        auth.register("draft-selection-user", "test-password");
        auth.login("draft-selection-user", "test-password");
        TaskController tasks = new TaskController(new TaskDAO(db), auth);
        Task target = tasks.createTask("Original task", "Original notes", null, null, Priority.MEDIUM);
        String context = (editing ? "Edit" : "New") + " draft / " + choice;
        String savedTitle = editing ? "Saved edited selection task" : "Saved new selection task";
        String draftNotes = "Draft notes retained through deselection.";
        SwingUtilities.invokeAndWait(() -> {
            AppWorkspace active = null;
            DashboardSurface surface = null;
            try {
                active = new AppWorkspace(auth, tasks);
                Method show = AppWorkspace.class.getDeclaredMethod("showDashboard");
                show.setAccessible(true);
                show.invoke(active);
                surface = (DashboardSurface) field(active, "dashboard");
                if (editing)
                    surface.edit.doClick(0);
                else
                    surface.newTask.doClick(0);
                InlineEditorSurface editor = (InlineEditorSurface) field(active, "editor");
                // The Save cases exercise validation before corrected retry.
                editor.title.setText(choice.equals("Save") ? "" : savedTitle);
                editor.notes.setText(draftNotes);
                editor.due.setText("01/02/2031");
                check(editor.dirty(), context + ": fixture must contain a dirty draft");
                surface.table.changeSelection(0, 0, true, false);
                check(field(active, "pending") != null && field(active, "editor") == editor,
                        context + ": deselection bypassed the dirty-draft guard");
                check((Integer) field(active, "selectedId") == target.getTaskId()
                        && surface.table.getSelectedRow() == 0,
                        context + ": selection changed before draft resolution");
                check(editor.notes.getText().equals(draftNotes) && editor.due.getText().equals("01/02/2031"),
                        context + ": deselection changed draft content");

                if (choice.equals("Keep")) {
                    editor.keep.doClick(0);
                    check(field(active, "editor") == editor && field(active, "pending") == null && editor.dirty(),
                            context + ": Keep editing lost draft or retained pending navigation");
                    check((Integer) field(active, "selectedId") == target.getTaskId()
                            && surface.table.getSelectedRow() == 0,
                            context + ": Keep editing failed to retain selection");
                    check(editor.title.getText().equals(savedTitle) && editor.notes.getText().equals(draftNotes),
                            context + ": Keep editing changed draft fields");
                } else if (choice.equals("Discard")) {
                    editor.discard.doClick(0);
                    checkClearedSelection(active, surface, context);
                } else {
                    editor.saveLeave.doClick(0);
                    check(field(active, "editor") == editor && field(active, "pending") != null,
                            context + ": invalid save lost draft or pending deselection");
                    check((Integer) field(active, "selectedId") == target.getTaskId()
                            && surface.table.getSelectedRow() == 0, context + ": invalid save committed deselection");
                    check(editor.title.getText().isEmpty() && editor.notes.getText().equals(draftNotes)
                            && !editor.feedback.getText().trim().isEmpty(),
                            context + ": invalid save lost editable fields or feedback");
                    check(tasks.getRelevantTasks().size() == 1
                            && new TaskDAO(db).findById(target.getTaskId()).getTitle().equals("Original task"),
                            context + ": invalid save changed persisted tasks");
                    editor.title.setText(savedTitle);
                    // Failed validation restores the form, where Save task retries the pending action.
                    editor.save.doClick(0);
                    checkClearedSelection(active, surface, context);
                    Task saved = tasks.getRelevantTasks().stream().filter(t -> t.getTitle().equals(savedTitle))
                            .findFirst().orElseThrow(() -> new AssertionError(context + ": corrected draft not saved"));
                    check(saved.getDescription().equals(draftNotes)
                            && saved.getDueDate().equals(LocalDate.of(2031, 1, 2)),
                            context + ": corrected draft fields changed");
                    check(!editing || saved.getTaskId() == target.getTaskId(),
                            context + ": editing created a different task");
                    check(tasks.getRelevantTasks().size() == (editing ? 1 : 2),
                            context + ": save created an unexpected number of tasks");
                }
                if (!choice.equals("Save")) {
                    Task persisted = new TaskDAO(db).findById(target.getTaskId());
                    check(tasks.getRelevantTasks().size() == 1 && persisted.getTitle().equals("Original task")
                            && persisted.getDescription().equals("Original notes") && persisted.getDueDate() == null,
                            context + ": Keep/Discard unexpectedly persisted draft changes");
                }
            } catch (Exception error) {
                throw new RuntimeException(error);
            } finally {
                if (surface != null)
                    surface.removeNotify();
                if (active != null)
                    active.removeNotify();
            }
        });
    }

    private static void checkClearedSelection(AppWorkspace active, DashboardSurface surface, String context) {
        check((Integer) fieldUnchecked(active, "selectedId") == -1 && surface.table.getSelectedRow() == -1,
                context + ": deselection left a stale logical or table selection");
        check(fieldUnchecked(active, "editor") == null && fieldUnchecked(active, "pending") == null,
                context + ": committed deselection retained editor/navigation state");
        check(detailText(surface, "metadata").equals("Select a task from your list.")
                && detailText(surface, "notes").equals("Your task details will appear here."),
                context + ": deselection retained stale task details");
        check(!surface.complete.isEnabled() && !surface.edit.isEnabled() && !surface.delete.isEnabled(),
                context + ": deselection retained enabled task actions");
    }

    /** Empty-account guidance must not appear just because completed tasks are filtered out. */
    private static void checkEmptyTaskStates() throws Exception {
        DatabaseManager db = new DatabaseManager(
                Files.createTempDirectory("empty-workspace-").resolve("test.db").toString());
        db.initializeDatabase();
        AuthController auth = new AuthController(new UserDAO(db));
        auth.register("empty-user", "test-password");
        auth.login("empty-user", "test-password");
        TaskController tasks = new TaskController(new TaskDAO(db), auth);
        AppWorkspace[] active = new AppWorkspace[1];
        DashboardSurface[] surface = new DashboardSurface[1];
        Task[] onlyTask = new Task[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    active[0] = new AppWorkspace(auth, tasks);
                    Method show = AppWorkspace.class.getDeclaredMethod("showDashboard");
                    show.setAccessible(true);
                    show.invoke(active[0]);
                    surface[0] = (DashboardSurface) field(active[0], "dashboard");
                    check(surface[0].model.getRowCount() == 0 && !surface[0].completedToggle.isVisible(),
                            "New account unexpectedly has tasks or completed filter");
                    check(detailText(surface[0], "metadata").equals("No tasks yet.")
                            && detailText(surface[0], "notes").equals("Create a task to get started."),
                            "Empty account lacks create-task guidance");
                    onlyTask[0] = tasks.createTask("Only task", "Saved notes", null, null, Priority.MEDIUM);
                    surface[0].refresh.doClick(0);
                    check(surface[0].model.getRowCount() == 1, "Created task did not appear");
                    check(detailText(surface[0], "metadata").contains("Created by empty-user")
                            && detailText(surface[0], "metadata").contains("Priority: Medium"),
                            "Workspace-selected task lacks creator/priority metadata");
                    surface[0].complete.doClick(0);
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
            awaitCompletion(surface[0]);
            SwingUtilities.invokeAndWait(() -> {
                checkHiddenCompletedGuidance(surface[0]);
                surface[0].completedToggle.doClick(0);
                check(surface[0].model.getRowCount() == 1 && surface[0].completedToggle.isSelected(),
                        "Show completed did not restore the only task");
                check(detailText(surface[0], "notes").equals("Saved notes")
                        && surface[0].complete.getText().equals("Reopen"),
                        "Shown completed task retained empty-state content");
                surface[0].completedToggle.doClick(0);
                checkHiddenCompletedGuidance(surface[0]);
                surface[0].completedToggle.doClick(0);
                surface[0].complete.doClick(0);
            });
            awaitCompletion(surface[0]);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    check(!new TaskDAO(db).findById(onlyTask[0].getTaskId()).isCompleted(),
                            "Empty-state fixture did not reopen");
                    check(surface[0].model.getRowCount() == 1 && !surface[0].completedToggle.isVisible()
                            && detailText(surface[0], "notes").equals("Saved notes"),
                            "Reopened task retained hidden-completed guidance");
                    tasks.deleteTask(onlyTask[0].getTaskId());
                    surface[0].refresh.doClick(0);
                    check(detailText(surface[0], "metadata").equals("No tasks yet.")
                            && detailText(surface[0], "notes").equals("Create a task to get started."),
                            "Truly empty account did not regain create-task guidance");
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (surface[0] != null)
                    surface[0].removeNotify();
                if (active[0] != null)
                    active[0].removeNotify();
            });
        }
    }

    private static void checkHiddenCompletedGuidance(DashboardSurface surface) {
        check(surface.model.getRowCount() == 0 && surface.completedToggle.isVisible()
                && !surface.completedToggle.isSelected(), "Only completed task was not hidden");
        check(detailText(surface, "title").equals("All caught up.")
                && detailText(surface, "metadata").equals("Your completed tasks are hidden.")
                && detailText(surface, "notes").contains("Show completed"),
                "Hidden completed task lacks Show completed guidance");
        check(!detailText(surface, "notes").contains("Create a task"),
                "Hidden completed task misrepresented as empty account");
        check(surface.completedToggle.getText().equals("Show completed (1)"),
                "Hidden completed filter count or label changed");
    }

    private static String detailText(DashboardSurface surface, String name) {
        return ((JTextArea) fieldUnchecked(surface, name)).getText();
    }

    /** Saving a new or edited draft must not redirect the queued completion animation. */
    private static void checkCompletionAfterDraftSave(boolean editing) throws Exception {
        DatabaseManager db = new DatabaseManager(
                Files.createTempDirectory("completion-draft-").resolve("test.db").toString());
        db.initializeDatabase();
        AuthController auth = new AuthController(new UserDAO(db));
        auth.register("draft-user", "test-password");
        auth.login("draft-user", "test-password");
        TaskDAO dao = new TaskDAO(db);
        TaskController tasks = new TaskController(dao, auth);
        Task target = tasks.createTask("Completion target", "", null, LocalDate.of(2030, 12, 31), Priority.MEDIUM);
        Task other = tasks.createTask("Other task", "", null, LocalDate.of(2031, 12, 31), Priority.MEDIUM);
        String draftTitle = editing ? "Saved edited target" : "Saved new draft";
        LocalDate draftDate = editing ? LocalDate.of(2032, 12, 31) : LocalDate.of(2028, 1, 1);
        String context = editing ? "Edited target" : "New draft";
        AppWorkspace[] active = new AppWorkspace[1];
        DashboardSurface[] surface = new DashboardSurface[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    active[0] = new AppWorkspace(auth, tasks);
                    Method show = AppWorkspace.class.getDeclaredMethod("showDashboard");
                    show.setAccessible(true);
                    show.invoke(active[0]);
                    surface[0] = (DashboardSurface) field(active[0], "dashboard");
                    DashboardSurface current = surface[0];
                    check((Integer) current.model.getValueAt(0, 0) == target.getTaskId(),
                            context + ": fixture target must start at row zero");
                    current.table.setRowSelectionInterval(0, 0);
                    if (editing)
                        current.edit.doClick(0);
                    else
                        current.newTask.doClick(0);
                    InlineEditorSurface editor = (InlineEditorSurface) field(active[0], "editor");
                    editor.title.setText(draftTitle);
                    editor.notes.setText("Draft notes must survive completion.");
                    editor.due.setText(editing ? "12/31/2032" : "01/01/2028");
                    check(current.table.isEnabled() && SwingUtilities.isDescendingFrom(current.table, current),
                            context + ": completion must use the attached, enabled table");
                    current.table.getActionMap().get("toggleTaskCompletion")
                            .actionPerformed(new ActionEvent(current.table, 0, "test"));
                    check(field(active[0], "pending") != null && field(active[0], "editor") == editor,
                            context + ": completion bypassed the dirty-draft guard");
                    editor.saveLeave.doClick(0);

                    // Inspect identity on the EDT before the animation timer can advance.
                    check((Integer) current.model.getValueAt(1, 0) == target.getTaskId(),
                            context + ": saved draft must move the target to row one");
                    int animatedRow = (Integer) field(current, "completingRow");
                    check(animatedRow >= 0 && (Integer) current.model.getValueAt(animatedRow, 0) == target.getTaskId(),
                            context + ": completion animated a different task after draft save");
                    check(dao.findById(target.getTaskId()).isCompleted(),
                            context + ": requested task was not completed");
                    check(!dao.findById(other.getTaskId()).isCompleted(), context + ": unrelated task was completed");
                    Task saved = tasks.getRelevantTasks().stream().filter(t -> t.getTitle().equals(draftTitle))
                            .findFirst().orElseThrow(() -> new AssertionError(context + ": draft was not saved"));
                    check(saved.getDueDate().equals(draftDate)
                            && saved.getDescription().equals("Draft notes must survive completion.")
                            && (!editing || saved.getTaskId() == target.getTaskId()),
                            context + ": saved draft fields or identity changed");
                    check(editing || !saved.isCompleted(), context + ": new draft was completed instead");
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });
            awaitCompletion(surface[0]);
            SwingUtilities.invokeAndWait(() -> {
                check(surface[0].model.getRowCount() == (editing ? 1 : 2),
                        context + ": completed task was not hidden after animation");
                for (int row = 0; row < surface[0].model.getRowCount(); row++)
                    check((Integer) surface[0].model.getValueAt(row, 0) != target.getTaskId(),
                            context + ": wrong task remained in the open list");
                check(fieldUnchecked(active[0], "editor") == null && fieldUnchecked(active[0], "pending") == null,
                        context + ": saved editor remained active");
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (surface[0] != null)
                    surface[0].removeNotify();
                if (active[0] != null)
                    active[0].removeNotify();
            });
        }
    }

    private static void awaitCompletion(DashboardSurface surface) throws Exception {
        long deadline = System.nanoTime() + 3_000_000_000L;
        boolean[] finished = new boolean[1];
        do {
            SwingUtilities.invokeAndWait(() -> finished[0] = surface.table.isEnabled());
            if (finished[0])
                return;
            Thread.sleep(20);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Completion animation did not finish within three seconds");
    }

    private static Object fieldUnchecked(Object target, String name) {
        try {
            return field(target, name);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}
