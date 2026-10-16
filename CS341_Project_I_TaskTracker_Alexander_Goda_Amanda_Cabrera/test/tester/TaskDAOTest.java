package tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tasktracker.data.DatabaseManager;
import tasktracker.data.TaskDAO;
import tasktracker.data.UserDAO;
import tasktracker.model.Priority;
import tasktracker.model.Task;
import tasktracker.model.User;

/**
 * JUnit tests for task persistence and the core Task Tracker data behavior.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class TaskDAOTest {
    @TempDir
    Path tempDir;

    private TaskDAO taskDAO;
    private User alex;
    private User amanda;
    private User thirdUser;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager databaseManager = new DatabaseManager(tempDir.resolve("task-dao-test.db").toString());
        databaseManager.initializeDatabase();
        UserDAO userDAO = new UserDAO(databaseManager);
        taskDAO = new TaskDAO(databaseManager);

        alex = userDAO.createUser("alex", "hash-a");
        amanda = userDAO.createUser("amanda", "hash-b");
        thirdUser = userDAO.createUser("third", "hash-c");
    }

    @Test
    void createTaskPersistsCoreFieldsDueDateAndPriority() throws Exception {
        Task task = new Task("Finish UML", "Review class relationships", alex, amanda,
                LocalDate.of(2026, 10, 22), Priority.HIGH);

        taskDAO.createTask(task);
        Task loaded = taskDAO.findById(task.getTaskId());

        assertNotNull(loaded);
        assertEquals("Finish UML", loaded.getTitle());
        assertEquals("amanda", loaded.getAssignee().getUsername());
        assertEquals(LocalDate.of(2026, 10, 22), loaded.getDueDate());
        assertEquals(Priority.HIGH, loaded.getPriority());
        assertFalse(loaded.isCompleted());
    }

    @Test
    void relevantTasksIncludeCreatedAndAssignedTasksButNotUnrelatedTasks() throws Exception {
        taskDAO.createTask(new Task("Created by Alex", "", alex, null, null, Priority.MEDIUM));
        taskDAO.createTask(new Task("Assigned to Alex", "", amanda, alex, null, Priority.LOW));
        taskDAO.createTask(new Task("Unrelated", "", amanda, thirdUser, null, Priority.HIGH));

        List<Task> relevant = taskDAO.findRelevantTasks(alex.getUserId());
        assertEquals(2, relevant.size());
    }

    @Test
    void updateTaskChangesEditableValues() throws Exception {
        Task task = taskDAO.createTask(new Task("Old title", "Old description", alex, null,
                null, Priority.LOW));
        task.setTitle("New title");
        task.setDescription("New description");
        task.setAssignee(amanda);
        task.setDueDate(LocalDate.of(2026, 11, 4));
        task.setPriority(Priority.HIGH);

        assertTrue(taskDAO.updateTask(task));
        Task loaded = taskDAO.findById(task.getTaskId());
        assertEquals("New title", loaded.getTitle());
        assertEquals("amanda", loaded.getAssignee().getUsername());
        assertEquals(Priority.HIGH, loaded.getPriority());
    }

    @Test
    void markCompletedPersistsCompletionState() throws Exception {
        Task task = taskDAO.createTask(new Task("Test completion", "", alex, amanda,
                null, Priority.MEDIUM));
        assertTrue(taskDAO.markCompleted(task.getTaskId(), true));
        assertTrue(taskDAO.findById(task.getTaskId()).isCompleted());
    }

    @Test
    void assignTaskCanAssignAndUnassignRegisteredUser() throws Exception {
        Task task = taskDAO.createTask(new Task("Assign me", "", alex, null,
                null, Priority.MEDIUM));

        assertTrue(taskDAO.assignTask(task.getTaskId(), amanda));
        assertEquals("amanda", taskDAO.findById(task.getTaskId()).getAssignee().getUsername());

        assertTrue(taskDAO.assignTask(task.getTaskId(), null));
        assertNull(taskDAO.findById(task.getTaskId()).getAssignee());
    }

    @Test
    void deleteTaskRemovesTask() throws Exception {
        Task task = taskDAO.createTask(new Task("Delete me", "", alex, null,
                null, Priority.MEDIUM));
        assertTrue(taskDAO.deleteTask(task.getTaskId()));
        assertNull(taskDAO.findById(task.getTaskId()));
    }
}
