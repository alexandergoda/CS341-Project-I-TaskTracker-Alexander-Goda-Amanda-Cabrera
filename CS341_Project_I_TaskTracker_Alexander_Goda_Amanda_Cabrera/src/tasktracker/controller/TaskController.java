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

    /**
     * Creates a controller for authenticated task operations.
     *
     * @param taskDAO required persistence dependency
     * @param authController required session controller
     * @throws IllegalArgumentException if a required dependency is null
     */
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
     * Creates an open task owned by the logged-in user.
     *
     * @param title nonblank title, trimmed before storage
     * @param description optional description; null becomes empty and whitespace is trimmed
     * @param assignee persisted user, or null for unassigned
     * @param dueDate due date, or null for none
     * @param priority priority, or null for MEDIUM
     * @return persisted task with its generated ID
     * @throws IllegalStateException if no user is logged in
     * @throws IllegalArgumentException if the title is blank or a user is not persisted
     * @throws SQLException if database access fails
     */
    public Task createTask(String title, String description, User assignee,
            LocalDate dueDate, Priority priority) throws SQLException {
        User currentUser = requireCurrentUser();
        Task task = new Task(title, description, currentUser, assignee, dueDate, priority);
        return taskDAO.createTask(task);
    }

    /**
     * Loads tasks created by or assigned to the current user.
     *
     * @return tasks ordered open first, then by ID; possibly empty; self-assigned tasks occur once
     * @throws IllegalStateException if no user is logged in
     * @throws SQLException if database access fails
     */
    public List<Task> getRelevantTasks() throws SQLException {
        User currentUser = requireCurrentUser();
        return taskDAO.findRelevantTasks(currentUser.getUserId());
    }

    /**
     * Loads tasks created by the current user.
     *
     * @return tasks ordered open first, then by ID; possibly empty
     * @throws IllegalStateException if no user is logged in
     * @throws SQLException if database access fails
     */
    public List<Task> getCreatedTasks() throws SQLException {
        User currentUser = requireCurrentUser();
        return taskDAO.findCreatedTasks(currentUser.getUserId());
    }

    /**
     * Loads tasks assigned to the current user.
     *
     * @return tasks ordered open first, then by ID; possibly empty
     * @throws IllegalStateException if no user is logged in
     * @throws SQLException if database access fails
     */
    public List<Task> getAssignedTasks() throws SQLException {
        User currentUser = requireCurrentUser();
        return taskDAO.findAssignedTasks(currentUser.getUserId());
    }

    /**
     * Updates details only when the logged-in user is the creator.
     *
     * @param taskId positive ID of an existing task
     * @param title nonblank title, trimmed before storage
     * @param description optional description; null becomes empty and whitespace is trimmed
     * @param assignee persisted user, or null for unassigned
     * @param dueDate due date, or null for none
     * @param priority priority, or null for MEDIUM
     * @return true if one stored task was changed; false if no row was changed
     * @throws IllegalStateException if no user is logged in
     * @throws IllegalArgumentException if the ID is invalid or the task is missing; also if
     *         the title is blank or a user is not persisted
     * @throws SecurityException if the current user lacks permission
     * @throws SQLException if database access fails
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
     * Assigns or unassigns a task only for its creator.
     *
     * @param taskId positive ID of an existing task
     * @param assignee persisted user, or null to unassign
     * @return true if one stored task was changed; false if no row was changed
     * @throws IllegalStateException if no user is logged in
     * @throws IllegalArgumentException if the ID is invalid or the task is missing; also if
     *         the assignee is not persisted
     * @throws SecurityException if the current user lacks permission
     * @throws SQLException if database access fails
     */
    public boolean assignTask(int taskId, User assignee) throws SQLException {
        User currentUser = requireCurrentUser();
        Task task = requireTask(taskId);
        requireCreator(task, currentUser);
        return taskDAO.assignTask(taskId, assignee);
    }

    /**
     * Completes or reopens a task for its creator or assignee.
     *
     * @param taskId positive ID of an existing task
     * @param completed true to complete; false to reopen
     * @return true if one stored task was changed; false if no row was changed
     * @throws IllegalStateException if no user is logged in
     * @throws IllegalArgumentException if the ID is invalid or the task is missing
     * @throws SecurityException if the current user lacks permission
     * @throws SQLException if database access fails
     */
    public boolean markCompleted(int taskId, boolean completed) throws SQLException {
        User currentUser = requireCurrentUser();
        Task task = requireTask(taskId);
        requireCreatorOrAssignee(task, currentUser);
        return taskDAO.markCompleted(taskId, completed);
    }

    /**
     * Permanently deletes a task only for its creator.
     *
     * @param taskId positive ID of an existing task
     * @return true if one stored task was changed; false if no row was changed
     * @throws IllegalStateException if no user is logged in
     * @throws IllegalArgumentException if the ID is invalid or the task is missing
     * @throws SecurityException if the current user lacks permission
     * @throws SQLException if database access fails
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
