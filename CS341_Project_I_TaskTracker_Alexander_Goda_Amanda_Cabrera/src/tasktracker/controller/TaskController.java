package tasktracker.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import tasktracker.data.TaskDAO;
import tasktracker.model.Priority;
import tasktracker.model.Task;
import tasktracker.model.User;

/**
 * Coordinates task operations between the views and persistence layer.
 * Permission checks are enforced here before TaskDAO changes stored data.
 *
 * @author Amanda Cabrera
 */
public class TaskController {
    private final TaskDAO taskDAO;
    private final AuthController authController;

    public TaskController(TaskDAO taskDAO, AuthController authController) {
        if (taskDAO == null) {
            throw new IllegalArgumentException("taskDAO is required");
        }
        if (authController == null) {
            throw new IllegalArgumentException("authController is required");
        }
        this.taskDAO = taskDAO;
        this.authController = authController;
    }

    /**
     * Creates a task owned by the currently logged-in user.
     */
    public Task createTask(String title, String description, User assignee,
            LocalDate dueDate, Priority priority) throws SQLException {
        User currentUser = requireCurrentUser();
        Task task = new Task(title, description, currentUser, assignee, dueDate, priority);
        return taskDAO.createTask(task);
    }

    /**
     * Returns tasks created by or assigned to the current user.
     */
    public List<Task> getRelevantTasks() throws SQLException {
        User currentUser = requireCurrentUser();
        return taskDAO.findRelevantTasks(currentUser.getUserId());
    }

    public List<Task> getCreatedTasks() throws SQLException {
        User currentUser = requireCurrentUser();
        return taskDAO.findCreatedTasks(currentUser.getUserId());
    }

    public List<Task> getAssignedTasks() throws SQLException {
        User currentUser = requireCurrentUser();
        return taskDAO.findAssignedTasks(currentUser.getUserId());
    }

    /**
     * Updates editable task details. Only the task creator may edit a task.
     */
    public boolean updateTask(int taskId, String title, String description, User assignee,
            LocalDate dueDate, Priority priority) throws SQLException {
        User currentUser = requireCurrentUser();
        Task task = requireTask(taskId);
        requireCreator(task, currentUser);

        task.setTitle(title);
        task.setDescription(description);
        task.setAssignee(assignee);
        task.setDueDate(dueDate);
        task.setPriority(priority);
        return taskDAO.updateTask(task);
    }

    /**
     * Assigns or unassigns a task. Pass null to remove the assignee.
     */
    public boolean assignTask(int taskId, User assignee) throws SQLException {
        User currentUser = requireCurrentUser();
        Task task = requireTask(taskId);
        requireCreator(task, currentUser);
        return taskDAO.assignTask(taskId, assignee);
    }

    /**
     * Changes completion state when the current user is the creator or assignee.
     */
    public boolean markCompleted(int taskId, boolean completed) throws SQLException {
        User currentUser = requireCurrentUser();
        Task task = requireTask(taskId);
        requireCreatorOrAssignee(task, currentUser);
        return taskDAO.markCompleted(taskId, completed);
    }

    /**
     * Deletes a task when the current user is its creator.
     */
    public boolean deleteTask(int taskId) throws SQLException {
        User currentUser = requireCurrentUser();
        Task task = requireTask(taskId);
        requireCreator(task, currentUser);
        return taskDAO.deleteTask(taskId);
    }

    private User requireCurrentUser() {
        User currentUser = authController.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("A user must be logged in");
        }
        return currentUser;
    }

    private Task requireTask(int taskId) throws SQLException {
        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
        Task task = taskDAO.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("task was not found");
        }
        return task;
    }

    private void requireCreator(Task task, User currentUser) {
        if (task.getCreator().getUserId() != currentUser.getUserId()) {
            throw new SecurityException("Only the task creator may perform this action");
        }
    }

    private void requireCreatorOrAssignee(Task task, User currentUser) {
        boolean creator = task.getCreator().getUserId() == currentUser.getUserId();
        boolean assignee = task.getAssignee() != null
                && task.getAssignee().getUserId() == currentUser.getUserId();
        if (!creator && !assignee) {
            throw new SecurityException("Only the task creator or assignee may perform this action");
        }
    }
}
