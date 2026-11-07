package tasktracker.view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import tasktracker.controller.AuthController;
import tasktracker.controller.TaskController;
import tasktracker.model.Task;
import tasktracker.model.User;
import tasktracker.view.screens.AuthSurface;
import tasktracker.view.screens.DashboardSurface;
import tasktracker.view.presentation.DisplayDates;
import tasktracker.view.screens.InlineEditorSurface;
import tasktracker.view.components.PressFeedbackButton;
import tasktracker.view.presentation.TaskListOrder;
import tasktracker.view.theme.ThemePalette;
import tasktracker.view.animation.TransitionHost;
import tasktracker.view.theme.UiTheme;

/**
 * Coordinates the active single-window login, dashboard and inline-editor flow. Screens own rendering;
 * controllers enforce permissions and delegate persistence to DAOs. Construct and use this workspace
 * on Swing's event dispatch thread.
 */
public final class AppWorkspace extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int SIGN_IN_CONFIRMATION_MS = 150;
    private final AuthController auth;
    private final TaskController tasks;
    private final TransitionHost screens = new TransitionHost();
    private AuthSurface login;
    private DashboardSurface dashboard;
    private InlineEditorSurface editor;
    private Task editing;
    private List<Task> rows = new ArrayList<>();
    private int selectedId = -1;
    private boolean selecting;
    private boolean registration;
    private boolean authMoving;
    private boolean authSubmitting;
    private Runnable pending;
    private Timer signInConfirmation;

    public AppWorkspace(AuthController auth, TaskController tasks) {
        super(new BorderLayout());
        this.auth = auth;
        this.tasks = tasks;
        add(screens);
        showLogin(false);
    }

    private void defaultButton(JButton button) {
        JRootPane root = getRootPane();
        if (root != null)
            root.setDefaultButton(button);
    }

    /**
     * Prepares detached login artwork without blocking Swing's event thread.
     *
     * @param ready action invoked when login artwork preparation finishes
     * @return worker preparing the detached login artwork
     */
    public javax.swing.SwingWorker<?, ?> prepareStartup(Runnable ready) {
        return login.scenePanel.prepareStartup(ready);
    }

    // Authentication and screen navigation

    private void showLogin(boolean animate) {
        login = new AuthSurface();
        registration = false;
        authMoving = false;
        authSubmitting = false;
        dashboard = null;
        editor = null;
        pending = null;
        bindAuthenticationActions();
        screens.showContent(login.scenePanel, animate);
        defaultButton(login.loginButton);
        login.usernameField.requestFocusInWindow();
    }

    private void bindAuthenticationActions() {
        login.loginButton.addActionListener(e -> authenticateAfterButtonRelease());
        login.usernameField.addActionListener(e -> submitAuthWithFeedback());
        login.passwordField.addActionListener(e -> submitAuthWithFeedback());
        login.confirmPasswordField.addActionListener(e -> submitAuthWithFeedback());
        login.scenePanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"),
                "submitAuth");
        login.scenePanel.getActionMap().put("submitAuth", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                submitAuthWithFeedback();
            }
        });
        login.registerButton.addActionListener(e -> morphAuth(!registration, false));
    }

    private void authenticateAfterButtonRelease() {
        if (authMoving || authSubmitting)
            return;
        authSubmitting = true;
        ((PressFeedbackButton) login.loginButton).afterRelease(() -> {
            authSubmitting = false;
            authenticate();
        });
    }

    private void morphAuth(boolean target, boolean created) {
        if (authMoving || authSubmitting)
            return;
        authMoving = true;
        login.transition(registration, target, created, () -> {
            registration = target;
            authMoving = false;
        });
    }

    private void submitAuthWithFeedback() {
        if (!authMoving && !authSubmitting)
            ((PressFeedbackButton) login.loginButton).clickWithFeedback();
    }

    private void authenticate() {
        if (authMoving || authSubmitting)
            return;
        char[] secret = login.passwordField.getPassword(), confirm = login.confirmPasswordField.getPassword();
        try {
            if (registration) {
                if (!Arrays.equals(secret, confirm)) {
                    login.showError("Passwords do not match.");
                    return;
                }
                User user = auth.register(login.usernameField.getText(), new String(secret));
                login.usernameField.setText(user.getUsername());
                morphAuth(false, true);
            } else if (auth.login(login.usernameField.getText(), new String(secret))) {
                confirmSignIn();
            } else {
                login.showError("Invalid username or password.");
                login.passwordField.setText("");
            }
        } catch (SQLException ex) {
            login.showError("Unable to access your account. Please try again.");
        } catch (IllegalArgumentException ex) {
            login.showError(ex.getMessage());
        } finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    private void confirmSignIn() {
        login.stop();
        authSubmitting = true;
        login.loginButton.setText("Signed in \u2713");
        signInConfirmation = new Timer(SIGN_IN_CONFIRMATION_MS, e -> {
            signInConfirmation.stop();
            signInConfirmation = null;
            showDashboard();
            // Capture the outgoing card before clearing its password dots.
            login.passwordField.setText("");
            authSubmitting = false;
        });
        signInConfirmation.setRepeats(false);
        signInConfirmation.start();
    }

    // Dashboard selection and task actions

    private Task selected() {
        for (Task t : rows)
            if (t.getTaskId() == selectedId)
                return t;
        return null;
    }

    private void showDashboard() {
        dashboard = new DashboardSurface(auth.getCurrentUser().getUsername());
        selectedId = -1;
        bindDashboardActions();
        reload(-1);
        screens.showWorkspace(dashboard, dashboard::isSceneReady, login.scenePanel.captureBackdrop(),
                login.scenePanel.cardBounds(), dashboard::entranceCards);
        defaultButton(null);
    }

    private void bindDashboardActions() {
        dashboard.newTask.addActionListener(e -> guard(() -> openEditor(null)));
        dashboard.edit.addActionListener(e -> guard(() -> openEditor(selected())));
        dashboard.refresh.addActionListener(e -> guard(this::refreshDashboard));
        dashboard.logout.addActionListener(e -> guard(this::logOut));
        dashboard.account.onChangePassword(e -> guard(this::openPasswordChange));
        dashboard.complete.addActionListener(e -> completeTask(dashboard.table.getSelectedRow()));
        dashboard.onCompletionRequested(this::completeTask);
        dashboard.completedToggle.addActionListener(e -> changeCompletedVisibility());
        dashboard.delete.addActionListener(e -> confirmDelete());
        dashboard.table.getSelectionModel().addListSelectionListener(e -> {
            if (!selecting && !e.getValueIsAdjusting())
                selectTaskFromTable();
        });
    }

    /** Commits completed-task visibility only after draft navigation is allowed. */
    private void changeCompletedVisibility() {
        final boolean requested = dashboard.completedToggle.isSelected();
        // Swing toggles before this handler; retain the current view while asking.
        dashboard.completedToggle.setSelected(!requested);
        guard(() -> {
            dashboard.completedToggle.setSelected(requested);
            refreshDashboard();
        });
    }

    private void refreshDashboard() {
        endEditor();
        reload(selectedId);
    }

    private void logOut() {
        auth.logout();
        showLogin(true);
    }

    private void selectTaskFromTable() {
        int row = dashboard.table.getSelectedRow();
        if (row >= rows.size())
            return;
        final int next = row < 0 ? -1 : rows.get(row).getTaskId();
        if (next == selectedId)
            return;
        // Keep the original selection until the draft guard allows navigation.
        restoreSelection();
        guard(() -> {
            endEditor();
            selectedId = next;
            restoreSelection();
            dashboard.showTask(selected(), auth.getCurrentUser(), true);
        });
    }

    /**
     * Persist first so a failed update never looks like a successful completion.
     */
    private void completeTask(int row) {
        if (row < 0 || row >= rows.size() || !dashboard.table.isEnabled())
            return;
        Task task = rows.get(row);
        guard(() -> {
            endEditor();
            try {
                boolean completed = !task.isCompleted();
                if (!tasks.markCompleted(task.getTaskId(), completed))
                    throw new IllegalStateException("Task was not updated.");
                // Saving a draft may have reloaded and reordered the displayed tasks.
                int currentRow = -1;
                for (int i = 0; i < rows.size(); i++) {
                    if (rows.get(i).getTaskId() == task.getTaskId()) {
                        currentRow = i;
                        break;
                    }
                }
                dashboard.animateCompletion(currentRow, completed, () -> reload(task.getTaskId()));
            } catch (SQLException | IllegalArgumentException | IllegalStateException | SecurityException ex) {
                dashboard.feedback.setText(ex.getMessage());
            }
        });
    }

    private void restoreSelection() {
        boolean previouslySelecting = selecting;
        selecting = true;
        try {
            dashboard.table.clearSelection();
            for (int i = 0; i < rows.size(); i++)
                if (rows.get(i).getTaskId() == selectedId)
                    dashboard.table.setRowSelectionInterval(i, i);
        } finally {
            selecting = previouslySelecting;
        }
    }

    private void reload(int desired) {
        try {
            List<Task> loaded = new ArrayList<>(tasks.getRelevantTasks());
            loaded.sort(TaskListOrder.BY_ATTENTION);
            int total = loaded.size();
            int completed = (int) loaded.stream().filter(Task::isCompleted).count();
            dashboard.completedToggle.setText(
                    (dashboard.completedToggle.isSelected() ? "Hide" : "Show") + " completed (" + completed + ")");
            dashboard.completedToggle.setVisible(completed > 0);
            if (!dashboard.completedToggle.isSelected())
                loaded.removeIf(Task::isCompleted);
            selecting = true;
            rows = loaded;
            dashboard.model.setRowCount(0);
            populateTaskTable();
            selectedId = rows.isEmpty() ? -1 : rows.get(0).getTaskId();
            for (Task t : rows)
                if (t.getTaskId() == desired)
                    selectedId = desired;
            dashboard.updateTaskGroups();
            restoreSelection();
            dashboard.showTask(selected(), auth.getCurrentUser(), false);
            dashboard.feedback.setText(
                    total == 0 ? "No tasks yet." : (total - completed) + " open · " + completed + " completed");
        } catch (SQLException ex) {
            dashboard.feedback.setText("Unable to load tasks.");
        } finally {
            selecting = false;
        }
    }

    private void populateTaskTable() {
        for (Task task : rows) {
            dashboard.model.addRow(new Object[] { task.getTaskId(), task.getTitle(), task.getCreator().getUsername(),
                    task.getAssignee() == null ? "Unassigned" : task.getAssignee().getUsername(),
                    task.getDueDate() == null ? "" : task.getDueDate().toString(), task.getPriority(),
                    task.isCompleted() ? "Complete" : "Open" });
        }
    }

    // Inline editor and unsaved-draft navigation

    private void openEditor(Task task) {
        if (task != null && task.getCreator().getUserId() != auth.getCurrentUser().getUserId()) {
            dashboard.feedback.setText("Only the creator may edit this task.");
            return;
        }
        try {
            InlineEditorSurface next = new InlineEditorSurface(task, auth.getRegisteredUsers());
            editing = task;
            editor = next;
            pending = null;
            bindEditorActions();
            dashboard.setWorkspace(editor);
            defaultButton(editor.save);
            editor.title.requestFocusInWindow();
        } catch (SQLException ex) {
            dashboard.feedback.setText("Unable to load assignees. Please try again.");
        }
    }

    private void bindEditorActions() {
        editor.save.addActionListener(e -> {
            if (pending != null)
                saveDraftAndContinue();
            else
                saveAndReturn();
        });
        editor.cancel.addActionListener(e -> guard(this::cancelEditor));
        editor.keep.addActionListener(e -> keepDraft());
        editor.discard.addActionListener(e -> discardDraftAndContinue());
        editor.saveLeave.addActionListener(e -> saveDraftAndContinue());
    }

    private void saveAndReturn() {
        int id = saveEditor();
        if (id > 0) {
            endEditor();
            reload(id);
        }
    }

    private void cancelEditor() {
        endEditor();
        dashboard.showTask(selected(), auth.getCurrentUser(), true);
    }

    private void keepDraft() {
        pending = null;
        editor.askToLeave(false);
        defaultButton(editor.save);
    }

    private void discardDraftAndContinue() {
        Runnable action = pending;
        pending = null;
        if (action != null) {
            endEditor();
            action.run();
        }
    }

    private void saveDraftAndContinue() {
        int id = saveEditor();
        if (id < 1) {
            editor.askToLeave(false);
            defaultButton(editor.save);
            return;
        }
        Runnable action = pending;
        pending = null;
        endEditor();
        reload(id);
        if (action != null)
            action.run();
    }

    /** Save errors keep both the form and draft intact. */
    private int saveEditor() {
        if (editor == null)
            return -1;
        try {
            String date = editor.due.getText().trim();
            LocalDate due = DisplayDates.parse(date);
            int id;
            if (editing == null)
                id = tasks.createTask(editor.title.getText(), editor.notes.getText(), editor.assignedUser(), due,
                        editor.priority.getSelectedItem()).getTaskId();
            else {
                id = editing.getTaskId();
                if (!tasks.updateTask(id, editor.title.getText(), editor.notes.getText(), editor.assignedUser(), due,
                        editor.priority.getSelectedItem()))
                    throw new IllegalStateException("Task was not saved.");
            }
            editor.accept();
            return id;
        } catch (DateTimeParseException ex) {
            editor.feedback.setText("Enter a valid date as MM/DD/YYYY, for example 11/06/2026.");
        } catch (SQLException ex) {
            editor.feedback.setText("Unable to save. Your draft is still here.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            editor.feedback.setText(ex.getMessage());
        }
        return -1;
    }

    private void endEditor() {
        editor = null;
        editing = null;
        pending = null;
        dashboard.restoreFocus();
        defaultButton(null);
    }

    /** Runs navigation only after the user resolves any unsaved editor draft. */
    private void guard(Runnable action) {
        if (editor != null && editor.dirty()) {
            pending = action;
            editor.askToLeave(true);
            defaultButton(null);
        } else
            action.run();
    }

    private void openPasswordChange() {
        if (editor != null)
            endEditor();
        tasktracker.view.screens.PasswordChangeSurface form = new tasktracker.view.screens.PasswordChangeSurface();
        defaultButton(null);
        form.cancel.addActionListener(e -> {
            form.clearPasswords();
            dashboard.restoreFocus();
        });
        form.save.addActionListener(e -> {
            char[] current = form.current.getPassword();
            char[] replacement = form.replacement.getPassword();
            char[] confirmation = form.confirmation.getPassword();
            try {
                auth.changePassword(new String(current), new String(replacement), new String(confirmation));
                form.clearPasswords();
                dashboard.restoreFocus();
                dashboard.feedback.setText("Password changed successfully.");
            } catch (SQLException ex) {
                form.feedback.setText("Unable to save. Please try again.");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                form.feedback.setText(ex.getMessage());
            } finally {
                java.util.Arrays.fill(current, '\0');
                java.util.Arrays.fill(replacement, '\0');
                java.util.Arrays.fill(confirmation, '\0');
            }
        });
        dashboard.setWorkspace(form);
    }

    private void confirmDelete() {
        final Task task = selected();
        if (task == null)
            return;
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setOpaque(false);
        JLabel question = new JLabel("Delete this task?");
        question.setFont(new Font("Georgia", Font.PLAIN, 28));
        question.setForeground(ThemePalette.INK);
        panel.add(question, BorderLayout.NORTH);
        JTextArea title = new JTextArea(task.getTitle());
        title.setEditable(false);
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setOpaque(false);
        title.setForeground(ThemePalette.INK);
        panel.add(title);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        JButton yes = UiTheme.styledButton("Delete task", true), no = UiTheme.styledButton("Keep task", false);
        buttons.add(yes);
        buttons.add(no);
        panel.add(buttons, BorderLayout.SOUTH);
        no.addActionListener(e -> dashboard.restoreFocus());
        yes.addActionListener(e -> deleteTask(task));
        dashboard.setWorkspace(panel);
    }

    private void deleteTask(Task task) {
        try {
            if (!tasks.deleteTask(task.getTaskId())) {
                throw new IllegalStateException("Task was not deleted.");
            }
            dashboard.restoreFocus();
            reload(-1);
        } catch (SQLException | IllegalArgumentException | IllegalStateException | SecurityException ex) {
            dashboard.feedback.setText(ex.getMessage());
        }
    }

    /**
     * Window closing follows the same draft guard as in-app navigation.
     *
     * @param close window-disposal action to run after any dirty draft is resolved
     */
    public void requestClose(Runnable close) {
        guard(close);
    }

    @Override
    public void removeNotify() {
        if (signInConfirmation != null)
            signInConfirmation.stop();
        signInConfirmation = null;
        super.removeNotify();
    }
}
