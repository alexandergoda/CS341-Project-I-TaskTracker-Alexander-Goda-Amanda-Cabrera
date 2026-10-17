package tasktracker.view;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import tasktracker.controller.AuthController;
import tasktracker.controller.TaskController;
import tasktracker.model.Task;
import tasktracker.model.User;

/**
 * Main authenticated task-management screen.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class TaskDashboardView extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AuthController authController;
    private final TaskController taskController;
    private final Runnable logoutAction;

    private JLabel welcomeLabel;
    private JLabel feedbackLabel;
    private JTable taskTable;
    private DefaultTableModel tableModel;

    private JButton newTaskButton;
    private JButton editTaskButton;
    private JButton completeButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private JButton logoutButton;

    private List<Task> visibleTasks;
    private final List<TaskEditorView> openEditors = new ArrayList<TaskEditorView>();

    /**
     * Creates the window without showing it. Loads tasks immediately and owns editors that
     * are disposed with this dashboard.
     * Construct and use on the Swing event thread.
     *
     * @param authController required session controller
     * @param taskController required task controller
     * @param logoutAction callback after disposal and logout, or null
     * @throws IllegalArgumentException if a required controller is null
     */
    public TaskDashboardView(AuthController authController,
            TaskController taskController, Runnable logoutAction) {

        if (authController == null) {
            throw new IllegalArgumentException("authController is required");
        }
        if (taskController == null) {
            throw new IllegalArgumentException("taskController is required");
        }

        this.authController = authController;
        this.taskController = taskController;
        this.logoutAction = logoutAction;
        this.visibleTasks = new ArrayList<Task>();

        initialize();
        createEvents();
        refreshTasks();
    }

    /**
     * Builds the authenticated dashboard and task table.
     */
    private void initialize() {
        setTitle("Task Tracker - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 900, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setLayout(null);

        JLabel titleLabel = new JLabel("Task Tracker");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        titleLabel.setBounds(35, 20, 220, 35);
        getContentPane().add(titleLabel);

        User currentUser = authController.getCurrentUser();
        String username = currentUser == null ? "" : currentUser.getUsername();

        welcomeLabel = new JLabel("Logged in as: " + username);
        welcomeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        welcomeLabel.setBounds(560, 25, 290, 25);
        getContentPane().add(welcomeLabel);

        String[] columns = {
                "ID", "Title", "Creator", "Assignee",
                "Due Date", "Priority", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        taskTable = new JTable(tableModel);
        taskTable.setSelectionMode(
                javax.swing.ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setBounds(35, 75, 815, 310);
        getContentPane().add(scrollPane);

        feedbackLabel = new JLabel("");
        feedbackLabel.setBounds(35, 395, 815, 25);
        getContentPane().add(feedbackLabel);

        newTaskButton = new JButton("New Task");
        newTaskButton.setBounds(35, 445, 120, 32);
        getContentPane().add(newTaskButton);

        editTaskButton = new JButton("Edit");
        editTaskButton.setBounds(165, 445, 100, 32);
        getContentPane().add(editTaskButton);

        completeButton = new JButton("Complete");
        completeButton.setBounds(275, 445, 120, 32);
        getContentPane().add(completeButton);

        deleteButton = new JButton("Delete");
        deleteButton.setBounds(405, 445, 100, 32);
        getContentPane().add(deleteButton);

        refreshButton = new JButton("Refresh");
        refreshButton.setBounds(515, 445, 110, 32);
        getContentPane().add(refreshButton);

        logoutButton = new JButton("Logout");
        logoutButton.setBounds(730, 445, 120, 32);
        getContentPane().add(logoutButton);
    }

    /**
     * Registers dashboard button handlers.
     */
    private void createEvents() {
        newTaskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                openNewTaskEditor();
            }
        });

        editTaskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                openSelectedTaskEditor();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refreshTasks();
            }
        });

        completeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                toggleSelectedTaskCompletion();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteSelectedTask();
            }
        });

        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                logout();
            }
        });
    }

    /**
     * Reloads tasks created by or assigned to the current user into the table.
     * Reports database or missing-session errors in the feedback label.
     * Call on the Swing event thread.
     */
    public void refreshTasks() {
        try {
            visibleTasks = taskController.getRelevantTasks();

            tableModel.setRowCount(0);

            for (Task task : visibleTasks) {
                String assignee = task.getAssignee() == null
                        ? "Unassigned"
                        : task.getAssignee().getUsername();

                String dueDate = task.getDueDate() == null
                        ? ""
                        : task.getDueDate().toString();

                String status = task.isCompleted()
                        ? "Complete"
                        : "Open";

                tableModel.addRow(new Object[] {
                        task.getTaskId(),
                        task.getTitle(),
                        task.getCreator().getUsername(),
                        assignee,
                        dueDate,
                        task.getPriority(),
                        status
                });
            }

            feedbackLabel.setText(
                    visibleTasks.isEmpty()
                    ? "No tasks to display."
                    : visibleTasks.size() + " task(s) loaded.");

        } catch (SQLException exception) {
            feedbackLabel.setText("Unable to load tasks.");
        } catch (IllegalStateException exception) {
            feedbackLabel.setText(exception.getMessage());
        }
    }

    /**
     * Opens the task editor for a new task.
     */
    private void openNewTaskEditor() {
        TaskEditorView editor = new TaskEditorView(
                authController,
                taskController,
                null,
                new Runnable() {
                    @Override
                    public void run() {
                        refreshTasks();
                    }
                });

        showTaskEditor(editor);
    }

    /**
     * Opens the selected task for editing when the current user is its creator.
     */
    private void openSelectedTaskEditor() {
        Task task = getSelectedTask();

        if (task == null) {
            feedbackLabel.setText("Select a task to edit.");
            return;
        }

        User currentUser = authController.getCurrentUser();

        if (currentUser == null
                || task.getCreator().getUserId() != currentUser.getUserId()) {
            feedbackLabel.setText("Only the task creator may edit this task.");
            return;
        }

        TaskEditorView editor = new TaskEditorView(
                authController,
                taskController,
                task,
                new Runnable() {
                    @Override
                    public void run() {
                        refreshTasks();
                    }
                });

        showTaskEditor(editor);
    }

    /**
     * Tracks editors belonging to this dashboard so they cannot remain open
     * after the dashboard's authenticated session ends.
     */
    private void showTaskEditor(final TaskEditorView editor) {
        openEditors.add(editor);
        editor.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                openEditors.remove(editor);
            }
        });
        editor.setLocationRelativeTo(this);
        editor.setVisible(true);
    }

    /**
     * Closes this dashboard and every task editor opened from it. A snapshot
     * allows window-closed callbacks to remove editors safely during disposal.
     */
    @Override
    public void dispose() {
        for (TaskEditorView editor : new ArrayList<TaskEditorView>(openEditors)) {
            editor.dispose();
        }
        openEditors.clear();
        super.dispose();
    }

    /**
     * Toggles the completion state of the selected task.
     */
    private void toggleSelectedTaskCompletion() {
        Task task = getSelectedTask();

        if (task == null) {
            feedbackLabel.setText("Select a task first.");
            return;
        }

        try {
            taskController.markCompleted(
                    task.getTaskId(),
                    !task.isCompleted());

            refreshTasks();

        } catch (SecurityException exception) {
            feedbackLabel.setText(exception.getMessage());
        } catch (SQLException exception) {
            feedbackLabel.setText(
                    "Unable to update task completion.");
        }
    }

    /**
     * Deletes the selected task after user confirmation.
     */
    private void deleteSelectedTask() {
        Task task = getSelectedTask();

        if (task == null) {
            feedbackLabel.setText("Select a task first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete task \"" + task.getTitle() + "\"?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            taskController.deleteTask(task.getTaskId());
            refreshTasks();

        } catch (SecurityException exception) {
            feedbackLabel.setText(exception.getMessage());
        } catch (SQLException exception) {
            feedbackLabel.setText("Unable to delete task.");
        }
    }

    private Task getSelectedTask() {
        int selectedRow = taskTable.getSelectedRow();

        if (selectedRow < 0 || selectedRow >= visibleTasks.size()) {
            return null;
        }

        return visibleTasks.get(selectedRow);
    }

    /**
     * Logs out and returns control to the supplied login action.
     */
    private void logout() {
        // Close task editors before clearing the session or opening another login.
        dispose();
        authController.logout();

        if (logoutAction != null) {
            logoutAction.run();
        }
    }
}
