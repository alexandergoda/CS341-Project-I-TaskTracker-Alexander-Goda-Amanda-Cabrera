package tasktracker.view;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import tasktracker.controller.AuthController;
import tasktracker.controller.TaskController;
import tasktracker.model.Priority;
import tasktracker.model.Task;
import tasktracker.model.User;

/**
 * Task creation and editing screen.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class TaskEditorView extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AuthController authController;
    private final TaskController taskController;
    private final Task taskToEdit;
    private final Runnable saveSuccessAction;

    private JTextField titleField;
    private JTextArea descriptionArea;
    private JComboBox<UserChoice> assigneeComboBox;
    private JTextField dueDateField;
    private JComboBox<Priority> priorityComboBox;
    private JLabel feedbackLabel;
    private JButton saveButton;
    private JButton cancelButton;

    /**
     * Creates the window without showing it. Loads assignment choices and existing values
     * immediately.
     * Construct and use on the Swing event thread.
     *
     * @param authController required authentication controller
     * @param taskController required task controller
     * @param taskToEdit existing task, or null to create a new task
     * @param saveSuccessAction callback after saving and disposing, or null
     * @throws IllegalArgumentException if a required controller is null
     */
    public TaskEditorView(AuthController authController,
            TaskController taskController,
            Task taskToEdit,
            Runnable saveSuccessAction) {

        if (authController == null) {
            throw new IllegalArgumentException("authController is required");
        }
        if (taskController == null) {
            throw new IllegalArgumentException("taskController is required");
        }

        this.authController = authController;
        this.taskController = taskController;
        this.taskToEdit = taskToEdit;
        this.saveSuccessAction = saveSuccessAction;

        initialize();
        createEvents();
        loadAssignees();
        loadTaskValues();
    }

    /**
     * Builds the task editor window.
     */
    private void initialize() {
        setTitle(taskToEdit == null
                ? "Task Tracker - New Task"
                : "Task Tracker - Edit Task");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 560, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setLayout(null);

        JLabel headingLabel = new JLabel(
                taskToEdit == null ? "Create Task" : "Edit Task");

        headingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headingLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        headingLabel.setBounds(140, 20, 280, 35);
        getContentPane().add(headingLabel);

        JLabel titleLabel = new JLabel("Title:");
        titleLabel.setBounds(55, 85, 110, 25);
        getContentPane().add(titleLabel);

        titleField = new JTextField();
        titleField.setBounds(175, 85, 315, 28);
        getContentPane().add(titleField);

        JLabel descriptionLabel = new JLabel("Description:");
        descriptionLabel.setBounds(55, 130, 110, 25);
        getContentPane().add(descriptionLabel);

        descriptionArea = new JTextArea();
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        JScrollPane descriptionScrollPane =
                new JScrollPane(descriptionArea);
        descriptionScrollPane.setBounds(175, 130, 315, 115);
        getContentPane().add(descriptionScrollPane);

        JLabel assigneeLabel = new JLabel("Assignee:");
        assigneeLabel.setBounds(55, 270, 110, 25);
        getContentPane().add(assigneeLabel);

        assigneeComboBox = new JComboBox<UserChoice>();
        assigneeComboBox.setBounds(175, 270, 315, 28);
        getContentPane().add(assigneeComboBox);

        JLabel dueDateLabel = new JLabel("Due date:");
        dueDateLabel.setBounds(55, 315, 110, 25);
        getContentPane().add(dueDateLabel);

        dueDateField = new JTextField();
        dueDateField.setBounds(175, 315, 315, 28);
        getContentPane().add(dueDateField);

        JLabel dueDateHint = new JLabel("YYYY-MM-DD (optional)");
        dueDateHint.setBounds(175, 342, 230, 20);
        getContentPane().add(dueDateHint);

        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setBounds(55, 380, 110, 25);
        getContentPane().add(priorityLabel);

        priorityComboBox = new JComboBox<Priority>(Priority.values());
        priorityComboBox.setBounds(175, 380, 180, 28);
        priorityComboBox.setSelectedItem(Priority.MEDIUM);
        getContentPane().add(priorityComboBox);

        feedbackLabel = new JLabel("");
        feedbackLabel.setHorizontalAlignment(SwingConstants.CENTER);
        feedbackLabel.setBounds(45, 420, 470, 25);
        getContentPane().add(feedbackLabel);

        saveButton = new JButton(taskToEdit == null ? "Create Task" : "Save Changes");
        saveButton.setBounds(125, 465, 145, 32);
        getContentPane().add(saveButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setBounds(290, 465, 120, 32);
        getContentPane().add(cancelButton);

        getRootPane().setDefaultButton(saveButton);
    }

    /**
     * Registers task-editor event handlers.
     */
    private void createEvents() {
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveTask();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
    }

    /**
     * Loads registered users into the assignee selection.
     */
    private void loadAssignees() {
        assigneeComboBox.removeAllItems();
        assigneeComboBox.addItem(new UserChoice(null, "Unassigned"));

        try {
            List<User> users = authController.getRegisteredUsers();

            for (User user : users) {
                assigneeComboBox.addItem(
                        new UserChoice(user, user.getUsername()));
            }

        } catch (SQLException exception) {
            feedbackLabel.setText("Unable to load registered users.");
        }
    }

    /**
     * Loads existing task values when editing.
     */
    private void loadTaskValues() {
        if (taskToEdit == null) {
            return;
        }

        titleField.setText(taskToEdit.getTitle());
        descriptionArea.setText(taskToEdit.getDescription());

        if (taskToEdit.getDueDate() != null) {
            dueDateField.setText(taskToEdit.getDueDate().toString());
        }

        priorityComboBox.setSelectedItem(taskToEdit.getPriority());

        User assignee = taskToEdit.getAssignee();

        if (assignee != null) {
            for (int i = 0; i < assigneeComboBox.getItemCount(); i++) {
                UserChoice choice = assigneeComboBox.getItemAt(i);

                if (choice.getUser() != null
                        && choice.getUser().getUserId() == assignee.getUserId()) {
                    assigneeComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Creates a new task or saves edits to the selected task.
     */
    private void saveTask() {
        String title = titleField.getText();
        String description = descriptionArea.getText();

        UserChoice selectedChoice =
                (UserChoice) assigneeComboBox.getSelectedItem();

        User assignee = selectedChoice == null
                ? null
                : selectedChoice.getUser();

        LocalDate dueDate;

        try {
            dueDate = parseDueDate(dueDateField.getText());
        } catch (DateTimeParseException exception) {
            feedbackLabel.setText("Due date must use YYYY-MM-DD.");
            return;
        }

        Priority priority =
                (Priority) priorityComboBox.getSelectedItem();

        try {
            if (taskToEdit == null) {
                taskController.createTask(
                        title,
                        description,
                        assignee,
                        dueDate,
                        priority);

            } else {
                taskController.updateTask(
                        taskToEdit.getTaskId(),
                        title,
                        description,
                        assignee,
                        dueDate,
                        priority);
            }

            dispose();

            if (saveSuccessAction != null) {
                saveSuccessAction.run();
            }

        } catch (IllegalArgumentException exception) {
            feedbackLabel.setText(exception.getMessage());
        } catch (SecurityException exception) {
            feedbackLabel.setText(exception.getMessage());
        } catch (SQLException exception) {
            feedbackLabel.setText("Unable to save task.");
        }
    }

    private LocalDate parseDueDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        return LocalDate.parse(text.trim());
    }

    /**
     * Small display wrapper for the optional assignee selection.
     */
    private static class UserChoice {
        private final User user;
        private final String displayText;

        UserChoice(User user, String displayText) {
            this.user = user;
            this.displayText = displayText;
        }

        User getUser() {
            return user;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }
}
