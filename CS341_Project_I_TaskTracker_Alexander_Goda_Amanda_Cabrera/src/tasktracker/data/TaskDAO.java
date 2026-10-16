package tasktracker.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import tasktracker.model.Priority;
import tasktracker.model.Task;
import tasktracker.model.User;

/**
 * Data-access operations for Task Tracker tasks.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class TaskDAO {
    private static final String TASK_SELECT =
            "SELECT t.task_id, t.title, t.description, t.creator_id, "
            + "c.username AS creator_username, t.assignee_id, "
            + "a.username AS assignee_username, t.due_date, t.priority, t.completed "
            + "FROM tasks t "
            + "JOIN users c ON c.user_id = t.creator_id "
            + "LEFT JOIN users a ON a.user_id = t.assignee_id ";

    private final DatabaseManager databaseManager;

    public TaskDAO(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("databaseManager is required");
        }
        this.databaseManager = databaseManager;
    }

    /**
     * Persists a new task and assigns its generated identifier to the object.
     */
    public Task createTask(Task task) throws SQLException {
        validatePersistableTask(task);
        String sql = "INSERT INTO tasks(title, description, creator_id, assignee_id, due_date, priority, completed) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            writeTaskValues(statement, task, false);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setTaskId(keys.getInt(1));
                    return task;
                }
            }
        }
        throw new SQLException("Task was inserted but no generated key was returned.");
    }

    public Task findById(int taskId) throws SQLException {
        String sql = TASK_SELECT + "WHERE t.task_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, taskId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapTask(results) : null;
            }
        }
    }

    /**
     * Returns all tasks created by or assigned to the specified user.
     */
    public List<Task> findRelevantTasks(int userId) throws SQLException {
        String sql = TASK_SELECT
                + "WHERE t.creator_id = ? OR t.assignee_id = ? "
                + "ORDER BY t.completed ASC, t.task_id ASC";
        return findTaskList(sql, userId, userId);
    }

    public List<Task> findCreatedTasks(int userId) throws SQLException {
        String sql = TASK_SELECT + "WHERE t.creator_id = ? ORDER BY t.completed ASC, t.task_id ASC";
        return findTaskList(sql, userId);
    }

    public List<Task> findAssignedTasks(int userId) throws SQLException {
        String sql = TASK_SELECT + "WHERE t.assignee_id = ? ORDER BY t.completed ASC, t.task_id ASC";
        return findTaskList(sql, userId);
    }

    /**
     * Updates editable task details. Creator ownership is deliberately not changed.
     */
    public boolean updateTask(Task task) throws SQLException {
        if (task == null || task.getTaskId() <= 0) {
            throw new IllegalArgumentException("A persisted task is required");
        }
        validatePersistableTask(task);
        String sql = "UPDATE tasks SET title = ?, description = ?, assignee_id = ?, due_date = ?, "
                + "priority = ?, completed = ? WHERE task_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, task.getTitle());
            statement.setString(2, task.getDescription());
            setNullableUserId(statement, 3, task.getAssignee());
            setNullableDate(statement, 4, task.getDueDate());
            statement.setString(5, task.getPriority().name());
            statement.setInt(6, task.isCompleted() ? 1 : 0);
            statement.setInt(7, task.getTaskId());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean assignTask(int taskId, User assignee) throws SQLException {
        String sql = "UPDATE tasks SET assignee_id = ? WHERE task_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableUserId(statement, 1, assignee);
            statement.setInt(2, taskId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean markCompleted(int taskId, boolean completed) throws SQLException {
        String sql = "UPDATE tasks SET completed = ? WHERE task_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, completed ? 1 : 0);
            statement.setInt(2, taskId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteTask(int taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE task_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, taskId);
            return statement.executeUpdate() == 1;
        }
    }

    private List<Task> findTaskList(String sql, int... values) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setInt(i + 1, values[i]);
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    tasks.add(mapTask(results));
                }
            }
        }
        return tasks;
    }

    private void writeTaskValues(PreparedStatement statement, Task task, boolean includeTaskId)
            throws SQLException {
        statement.setString(1, task.getTitle());
        statement.setString(2, task.getDescription());
        statement.setInt(3, task.getCreator().getUserId());
        setNullableUserId(statement, 4, task.getAssignee());
        setNullableDate(statement, 5, task.getDueDate());
        statement.setString(6, task.getPriority().name());
        statement.setInt(7, task.isCompleted() ? 1 : 0);
        if (includeTaskId) {
            statement.setInt(8, task.getTaskId());
        }
    }

    private void setNullableUserId(PreparedStatement statement, int index, User user) throws SQLException {
        if (user == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            if (user.getUserId() <= 0) {
                throw new IllegalArgumentException("assignee must be a persisted user");
            }
            statement.setInt(index, user.getUserId());
        }
    }

    private void setNullableDate(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, date.toString());
        }
    }

    private void validatePersistableTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("task is required");
        }
        if (task.getCreator().getUserId() <= 0) {
            throw new IllegalArgumentException("creator must be a persisted user");
        }
        if (task.getAssignee() != null && task.getAssignee().getUserId() <= 0) {
            throw new IllegalArgumentException("assignee must be a persisted user");
        }
    }

    private Task mapTask(ResultSet results) throws SQLException {
        User creator = new User(results.getInt("creator_id"), results.getString("creator_username"));

        int assigneeId = results.getInt("assignee_id");
        User assignee = null;
        if (!results.wasNull()) {
            assignee = new User(assigneeId, results.getString("assignee_username"));
        }

        LocalDate dueDate = null;
        String dueDateText = results.getString("due_date");
        if (dueDateText != null && !dueDateText.trim().isEmpty()) {
            try {
                dueDate = LocalDate.parse(dueDateText);
            } catch (DateTimeParseException e) {
                throw new SQLException("Invalid due_date stored for task " + results.getInt("task_id"), e);
            }
        }

        Priority priority;
        try {
            priority = Priority.valueOf(results.getString("priority"));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid priority stored for task " + results.getInt("task_id"), e);
        }

        return new Task(
                results.getInt("task_id"),
                results.getString("title"),
                results.getString("description"),
                creator,
                assignee,
                dueDate,
                priority,
                results.getInt("completed") == 1);
    }
}
