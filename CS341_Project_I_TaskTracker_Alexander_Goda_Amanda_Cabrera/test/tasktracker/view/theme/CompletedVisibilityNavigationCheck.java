package tasktracker.view.theme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import tasktracker.controller.AuthController;
import tasktracker.controller.TaskController;
import tasktracker.data.DatabaseManager;
import tasktracker.data.TaskDAO;
import tasktracker.data.UserDAO;
import tasktracker.model.Task;
import tasktracker.model.User;
import tasktracker.view.AppWorkspace;
import tasktracker.view.screens.DashboardSurface;
import tasktracker.view.screens.InlineEditorSurface;

/** Checks that completed-task visibility changes only after draft navigation is approved. */
public final class CompletedVisibilityNavigationCheck {
    private static final String NOTES = "Unsaved notes must stay with the draft.";
    private static final String VALID_DATE = "02/29/2028";

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            try {
                for (boolean editing : new boolean[] { false, true }) {
                    for (boolean initiallyVisible : new boolean[] { false, true }) {
                        Fixture fixture = new Fixture();
                        try {
                            checkOrdinaryToggle(fixture);
                            fixture.setVisibility(initiallyVisible);
                            checkKeepEditing(fixture, editing, initiallyVisible);
                            checkDiscard(fixture, editing, initiallyVisible);
                            fixture.setVisibility(initiallyVisible);
                            checkSaveAndContinue(fixture, editing, initiallyVisible, false);
                            fixture.setVisibility(initiallyVisible);
                            checkSaveAndContinue(fixture, editing, initiallyVisible, true);
                        } finally {
                            fixture.close();
                        }
                    }
                }
                checkAssigneeIdentityNavigation();
                checkIndependentDraftFields();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        System.out.println("PASS: ordinary show/hide; new and edit drafts in both directions; "
                + "keep, discard, save-and-continue, invalid save and corrected retry; "
                + "assignee identity and independent draft fields.");
        System.exit(0);
    }

    /** Display-identical assignee choices must still participate in draft protection. */
    private static void checkAssigneeIdentityNavigation() throws Exception {
        for (boolean assignNamedUser : new boolean[] { true, false }) {
            Fixture fixture = new Fixture();
            try {
                User original = assignNamedUser ? null : fixture.unassignedUser;
                User requested = assignNamedUser ? fixture.unassignedUser : null;
                check(fixture.tasks.assignTask(fixture.editId, original),
                        "Could not establish the original assignment");
                fixture.dashboard.refresh.doClick(0);
                int originalCount = fixture.tasks.getRelevantTasks().size();
                InlineEditorSurface editor = fixture.openAssigneeDraft(requested);
                fixture.dashboard.refresh.doClick(0);
                fixture.assertAssignmentPrompt(editor);
                editor.keep.doClick(0);
                check(field(fixture.workspace, "pending") == null, "Keep editing retained assignment navigation");
                check(field(fixture.workspace, "editor") == editor && editor.dirty(),
                        "Keep editing discarded the assignment-only draft");
                check(((JPanel) field(editor, "editingPage")).isVisible(),
                        "Keep editing did not restore the assignment form");
                assertAssignee(editor.assignedUser(), requested, "Kept draft assignment");
                assertAssignee(fixture.findById(fixture.editId).getAssignee(), original,
                        "Keep editing changed persisted assignment");

                fixture.dashboard.refresh.doClick(0);
                fixture.assertAssignmentPrompt(editor);
                editor.discard.doClick(0);
                check(field(fixture.workspace, "editor") == null && field(fixture.workspace, "pending") == null,
                        "Discard did not finish assignment navigation");
                assertAssignee(fixture.findById(fixture.editId).getAssignee(), original,
                        "Discard persisted the assignment-only change");

                editor = fixture.openAssigneeDraft(requested);
                fixture.dashboard.refresh.doClick(0);
                fixture.assertAssignmentPrompt(editor);
                editor.saveLeave.doClick(0);
                check(field(fixture.workspace, "editor") == null && field(fixture.workspace, "pending") == null,
                        "Save and continue did not finish assignment navigation");
                assertAssignee(fixture.findById(fixture.editId).getAssignee(), requested, "Saved assignment identity");
                check(fixture.tasks.getRelevantTasks().size() == originalCount,
                        "Saving an assignment-only edit created another task");
            } finally {
                fixture.close();
            }
        }
    }

    private static void assertAssignee(User actual, User expected, String context) {
        check(expected == null ? actual == null : actual != null && actual.getUserId() == expected.getUserId(),
                context);
    }

    /** Text from one field must never conceal a change to a different field. */
    private static void checkIndependentDraftFields() {
        InlineEditorSurface editor = new InlineEditorSurface(null, List.of());
        editor.title.setText("first\u0001middle");
        editor.notes.setText("last");
        editor.accept();
        check(!editor.dirty(), "Accepted field baseline remains dirty");
        editor.title.setText("first");
        editor.notes.setText("middle\u0001last");
        check(editor.dirty(), "Independent title and notes edits collided in draft state");
    }

    private static void checkOrdinaryToggle(Fixture fixture) throws Exception {
        fixture.assertVisibility(false);
        fixture.dashboard.completedToggle.doClick(0);
        fixture.assertVisibility(true);
        fixture.dashboard.completedToggle.doClick(0);
        fixture.assertVisibility(false);
    }

    private static void checkKeepEditing(Fixture fixture, boolean editing, boolean visible) throws Exception {
        InlineEditorSurface editor = fixture.openDraft(editing, "Keep this draft", VALID_DATE);
        List<Integer> beforeRows = fixture.rowIds();
        int beforeSelection = fixture.selectedId();
        String beforeLabel = fixture.dashboard.completedToggle.getText();
        fixture.dashboard.completedToggle.doClick(0);
        check(field(fixture.workspace, "pending") != null, "Dirty toggle bypassed confirmation");
        editor.keep.doClick(0);

        String context = (editing ? "Edit" : "New") + " draft, " + (visible ? "hide" : "show");
        check(fixture.dashboard.completedToggle.isSelected() == visible,
                context + ": Keep editing changed committed visibility");
        check(fixture.rowIds().equals(beforeRows), context + ": Keep editing changed displayed rows");
        check(fixture.selectedId() == beforeSelection, context + ": Keep editing changed selection");
        check(fixture.dashboard.completedToggle.getText().equals(beforeLabel),
                context + ": Keep editing changed the toggle label");
        fixture.assertVisibility(visible);
        fixture.assertDraft(editor, "Keep this draft", VALID_DATE);
        check(field(fixture.workspace, "pending") == null, "Keep editing retained a pending action");
        check(fixture.findByTitle("Keep this draft") == null, "Keep editing persisted the draft");

        // Leave through the real cancel/discard controls before the next scenario.
        editor.cancel.doClick(0);
        editor.discard.doClick(0);
        check(field(fixture.workspace, "editor") == null, "Cleanup did not leave the editor");
    }

    private static void checkDiscard(Fixture fixture, boolean editing, boolean visible) throws Exception {
        String originalTitle = fixture.findById(fixture.editId).getTitle();
        int originalCount = fixture.tasks.getRelevantTasks().size();
        InlineEditorSurface editor = fixture.openDraft(editing, "Discard this draft", VALID_DATE);
        int originalSelection = fixture.selectedId();
        fixture.dashboard.completedToggle.doClick(0);
        editor.discard.doClick(0);

        fixture.assertVisibility(!visible);
        check(field(fixture.workspace, "editor") == null, "Discard did not return to focus");
        check(field(fixture.workspace, "pending") == null, "Discard retained a pending action");
        check(fixture.selectedId() == originalSelection, "Discard lost the selected open task");
        check(fixture.tasks.getRelevantTasks().size() == originalCount, "Discard changed the task count");
        check(fixture.findByTitle("Discard this draft") == null, "Discard persisted draft text");
        check(fixture.findById(fixture.editId).getTitle().equals(originalTitle), "Discard changed the existing task");
    }

    private static void checkSaveAndContinue(Fixture fixture, boolean editing, boolean visible, boolean failFirst)
            throws Exception {
        String title = failFirst ? "Corrected draft" : "Saved draft";
        String date = failFirst ? "02/30/2028" : VALID_DATE;
        int originalCount = fixture.tasks.getRelevantTasks().size();
        String originalTitle = fixture.findById(fixture.editId).getTitle();
        InlineEditorSurface editor = fixture.openDraft(editing, title, date);
        List<Integer> beforeRows = fixture.rowIds();
        int beforeSelection = fixture.selectedId();
        fixture.dashboard.completedToggle.doClick(0);
        editor.saveLeave.doClick(0);

        if (failFirst) {
            fixture.assertVisibility(visible);
            check(fixture.rowIds().equals(beforeRows), "Invalid save changed displayed rows");
            check(fixture.selectedId() == beforeSelection, "Invalid save changed selection");
            fixture.assertDraft(editor, title, date);
            check(field(fixture.workspace, "pending") != null, "Invalid save lost the requested toggle");
            check(!editor.feedback.getText().trim().isEmpty(), "Invalid save gave no feedback");
            check(fixture.tasks.getRelevantTasks().size() == originalCount,
                    "Invalid save changed the persisted task count");
            check(fixture.findById(fixture.editId).getTitle().equals(originalTitle),
                    "Invalid save changed the existing task");
            check(fixture.findByTitle(title) == null, "Invalid save persisted the draft");
            editor.due.setText(VALID_DATE);
            editor.save.doClick(0);
        }

        fixture.assertVisibility(!visible);
        check(field(fixture.workspace, "editor") == null, "Successful save did not return to focus");
        check(field(fixture.workspace, "pending") == null, "Successful save retained a pending action");
        Task saved = fixture.findByTitle(title);
        check(saved != null, "Visibility changed without persisting the draft");
        check(saved.getDescription().equals(NOTES), "Saved notes differ from the draft");
        check(saved.getDueDate().equals(LocalDate.of(2028, 2, 29)), "Saved date differs from the draft");
        check(fixture.tasks.getRelevantTasks().size() == originalCount + (editing ? 0 : 1),
                "Save created the wrong number of tasks");
        check(!editing || saved.getTaskId() == fixture.editId, "Edit created a different task");
        check(fixture.selectedId() == saved.getTaskId(), "Successful save did not focus the saved task");
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    /** One disposable database and live workspace for each new/edit and show/hide combination. */
    private static final class Fixture {
        private final TaskController tasks;
        private final AppWorkspace workspace;
        private final DashboardSurface dashboard;
        private final int editId;
        private final User unassignedUser;

        Fixture() throws Exception {
            DatabaseManager db = new DatabaseManager(
                    Files.createTempDirectory("completed-navigation-").resolve("test.db").toString());
            db.initializeDatabase();
            AuthController auth = new AuthController(new UserDAO(db));
            auth.register("navigation-user", "test-password");
            unassignedUser = auth.register("Unassigned", "test-password");
            auth.login("navigation-user", "test-password");
            tasks = new TaskController(new TaskDAO(db), auth);
            tasks.createTask("First open task", "", null, null, null);
            editId = tasks.createTask("Selected open task", "Original notes", null, null, null).getTaskId();
            Task completed = tasks.createTask("Completed fixture", "", null, null, null);
            tasks.markCompleted(completed.getTaskId(), true);
            workspace = new AppWorkspace(auth, tasks);
            Method show = AppWorkspace.class.getDeclaredMethod("showDashboard");
            show.setAccessible(true);
            show.invoke(workspace);
            dashboard = (DashboardSurface) field(workspace, "dashboard");
            check(dashboard.completedToggle.isVisible(), "Fixture must expose a real completed-task toggle");
        }

        InlineEditorSurface openDraft(boolean editing, String title, String date) throws Exception {
            int row = rowIds().indexOf(editId);
            check(row >= 0, "Selected open fixture is missing");
            dashboard.table.setRowSelectionInterval(row, row);
            if (editing)
                dashboard.edit.doClick(0);
            else
                dashboard.newTask.doClick(0);
            InlineEditorSurface editor = (InlineEditorSurface) field(workspace, "editor");
            check(editor != null, "Task button did not open an editor");
            editor.title.setText(title);
            editor.notes.setText(NOTES);
            editor.due.setText(date);
            return editor;
        }

        InlineEditorSurface openAssigneeDraft(User requested) throws Exception {
            int row = rowIds().indexOf(editId);
            check(row >= 0, "Assignment fixture is missing");
            dashboard.table.setRowSelectionInterval(row, row);
            dashboard.edit.doClick(0);
            InlineEditorSurface editor = (InlineEditorSurface) field(workspace, "editor");
            check(editor != null && !editor.dirty(), "Existing assignment did not open cleanly");
            if (requested == null)
                editor.assignee.setSelectedIndex(0);
            else
                editor.assignee.setSelectedItem(requested);
            assertAssignee(editor.assignedUser(), requested, "Requested assignee was not selected");
            check(editor.dirty(), "Assignee identity change was not recognized as dirty");
            return editor;
        }

        void assertAssignmentPrompt(InlineEditorSurface editor) throws Exception {
            check(field(workspace, "editor") == editor && field(workspace, "pending") != null,
                    "Assignment-only navigation bypassed the dirty-draft confirmation");
            check(((JPanel) field(editor, "warning")).isVisible(),
                    "Assignment-only navigation did not show the confirmation");
        }

        void setVisibility(boolean visible) throws Exception {
            if (dashboard.completedToggle.isSelected() != visible)
                dashboard.completedToggle.doClick(0);
            assertVisibility(visible);
        }

        void assertVisibility(boolean visible) throws Exception {
            check(dashboard.completedToggle.isSelected() == visible, "Committed visibility is incorrect");
            check(dashboard.completedToggle.getText().equals((visible ? "Hide" : "Show") + " completed (1)"),
                    "Toggle label disagrees with committed visibility");
            Set<Integer> expected = new HashSet<>();
            for (Task task : tasks.getRelevantTasks()) {
                if (visible || !task.isCompleted())
                    expected.add(task.getTaskId());
            }
            List<Integer> actual = rowIds();
            check(actual.size() == expected.size() && new HashSet<>(actual).equals(expected),
                    "Displayed task identities disagree with committed visibility");
        }

        void assertDraft(InlineEditorSurface editor, String title, String date) throws Exception {
            check(field(workspace, "editor") == editor, "Navigation replaced the original draft");
            check(editor.dirty() && editor.title.getText().equals(title) && editor.notes.getText().equals(NOTES)
                    && editor.due.getText().equals(date), "Navigation lost draft content");
            check(((JPanel) field(editor, "editingPage")).isVisible(), "Draft form was not restored");
        }

        List<Integer> rowIds() {
            List<Integer> ids = new ArrayList<>();
            for (int row = 0; row < dashboard.model.getRowCount(); row++) {
                ids.add((Integer) dashboard.model.getValueAt(row, 0));
            }
            return ids;
        }

        int selectedId() {
            int row = dashboard.table.getSelectedRow();
            return row < 0 ? -1 : (Integer) dashboard.model.getValueAt(row, 0);
        }

        Task findById(int id) throws Exception {
            for (Task task : tasks.getRelevantTasks()) {
                if (task.getTaskId() == id)
                    return task;
            }
            throw new AssertionError("Persisted fixture is missing: " + id);
        }

        Task findByTitle(String title) throws Exception {
            for (Task task : tasks.getRelevantTasks()) {
                if (task.getTitle().equals(title))
                    return task;
            }
            return null;
        }

        void close() {
            dashboard.removeNotify();
            workspace.removeNotify();
        }
    }
}
