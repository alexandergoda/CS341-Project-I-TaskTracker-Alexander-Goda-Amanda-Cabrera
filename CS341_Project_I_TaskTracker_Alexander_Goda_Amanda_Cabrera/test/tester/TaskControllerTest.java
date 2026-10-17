package tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tasktracker.controller.AuthController;
import tasktracker.controller.TaskController;
import tasktracker.data.DatabaseManager;
import tasktracker.data.TaskDAO;
import tasktracker.data.UserDAO;
import tasktracker.model.Priority;
import tasktracker.model.Task;
import tasktracker.model.User;

/**
 * JUnit tests for TaskController permissions and task operations.
 *
 * @author Alexander Goda
 */
public class TaskControllerTest {

    @TempDir
    Path tempDir;

    private AuthController authController;
    private TaskController taskController;
    private TaskDAO taskDAO;

    private User alex;
    private User amanda;
    private User charlie;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager databaseManager =
                new DatabaseManager(tempDir.resolve("task-controller-test.db").toString());

        databaseManager.initializeDatabase();

        UserDAO userDAO = new UserDAO(databaseManager);
        taskDAO = new TaskDAO(databaseManager);

        authController = new AuthController(userDAO);
        taskController = new TaskController(taskDAO, authController);

        alex = authController.register("alex", "test123");
        amanda = authController.register("amanda", "test123");
        charlie = authController.register("charlie", "test123");

        assertTrue(authController.login("alex", "test123"));
    }

    @Test
    void creatorCanEditTask() throws Exception {
        Task created = taskController.createTask(
                "Original title",
                "Original description",
                null,
                LocalDate.of(2026, 10, 20),
                Priority.MEDIUM);

        assertTrue(taskController.updateTask(
                created.getTaskId(),
                "Updated title",
                "Updated description",
                amanda,
                LocalDate.of(2026, 10, 21),
                Priority.HIGH));

        Task updated = taskDAO.findById(created.getTaskId());

        assertNotNull(updated);
        assertEquals("Updated title", updated.getTitle());
        assertEquals("Updated description", updated.getDescription());
        assertNotNull(updated.getAssignee());
        assertEquals(amanda.getUserId(), updated.getAssignee().getUserId());
        assertEquals(LocalDate.of(2026, 10, 21), updated.getDueDate());
        assertEquals(Priority.HIGH, updated.getPriority());
    }

    @Test
    void nonCreatorCannotEditTask() throws Exception {
        Task created = taskController.createTask(
                "Alex task",
                "Only Alex may edit",
                amanda,
                null,
                Priority.MEDIUM);

        authController.logout();
        assertTrue(authController.login("amanda", "test123"));

        assertThrows(
                SecurityException.class,
                () -> taskController.updateTask(
                        created.getTaskId(),
                        "Unauthorized edit",
                        "Should not save",
                        amanda,
                        null,
                        Priority.LOW));

        Task unchanged = taskDAO.findById(created.getTaskId());

        assertNotNull(unchanged);
        assertEquals("Alex task", unchanged.getTitle());
    }

    @Test
    void creatorCanDeleteTask() throws Exception {
        Task created = taskController.createTask(
                "Delete me",
                "",
                null,
                null,
                Priority.LOW);

        assertTrue(taskController.deleteTask(created.getTaskId()));
        assertNull(taskDAO.findById(created.getTaskId()));
    }

    @Test
    void nonCreatorCannotDeleteTask() throws Exception {
        Task created = taskController.createTask(
                "Protected task",
                "",
                amanda,
                null,
                Priority.MEDIUM);

        authController.logout();
        assertTrue(authController.login("amanda", "test123"));

        assertThrows(
                SecurityException.class,
                () -> taskController.deleteTask(created.getTaskId()));

        assertNotNull(taskDAO.findById(created.getTaskId()));
    }

    @Test
    void assigneeCanCompleteAssignedTask() throws Exception {
        Task created = taskController.createTask(
                "Shared task",
                "",
                amanda,
                null,
                Priority.HIGH);

        authController.logout();
        assertTrue(authController.login("amanda", "test123"));

        assertTrue(taskController.markCompleted(created.getTaskId(), true));

        Task completed = taskDAO.findById(created.getTaskId());

        assertNotNull(completed);
        assertTrue(completed.isCompleted());
    }

    @Test
    void unrelatedUserCannotCompleteTask() throws Exception {
        Task created = taskController.createTask(
                "Shared task",
                "",
                amanda,
                null,
                Priority.HIGH);

        authController.logout();
        assertTrue(authController.login("charlie", "test123"));

        assertThrows(
                SecurityException.class,
                () -> taskController.markCompleted(created.getTaskId(), true));

        Task unchanged = taskDAO.findById(created.getTaskId());

        assertNotNull(unchanged);
        assertFalse(unchanged.isCompleted());
    }
}
