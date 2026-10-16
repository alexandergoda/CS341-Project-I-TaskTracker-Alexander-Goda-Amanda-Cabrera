package tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tasktracker.data.DatabaseManager;
import tasktracker.data.UserDAO;
import tasktracker.model.User;

/**
 * JUnit tests for user persistence and authentication queries.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class UserDAOTest {
    @TempDir
    Path tempDir;

    private UserDAO userDAO;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager databaseManager = new DatabaseManager(tempDir.resolve("user-dao-test.db").toString());
        databaseManager.initializeDatabase();
        userDAO = new UserDAO(databaseManager);
    }

    @Test
    void createAndFindUser() throws Exception {
        User created = userDAO.createUser("alex", "hash-alex");
        User loaded = userDAO.findByUsername("alex");

        assertNotNull(loaded);
        assertEquals(created.getUserId(), loaded.getUserId());
        assertEquals("alex", loaded.getUsername());
    }

    @Test
    void duplicateUsernameIsRejectedByDatabase() throws Exception {
        userDAO.createUser("amanda", "hash-one");
        assertThrows(SQLException.class, () -> userDAO.createUser("amanda", "hash-two"));
    }

    @Test
    void authenticationReturnsUserForMatchingCredentials() throws Exception {
        userDAO.createUser("alex", "hash-alex");
        assertNotNull(userDAO.authenticate("alex", "hash-alex"));
    }

    @Test
    void authenticationReturnsNullForWrongPasswordHash() throws Exception {
        userDAO.createUser("alex", "hash-alex");
        assertNull(userDAO.authenticate("alex", "wrong-hash"));
    }

    @Test
    void findAllReturnsRegisteredUsers() throws Exception {
        userDAO.createUser("alex", "hash-a");
        userDAO.createUser("amanda", "hash-b");
        assertEquals(2, userDAO.findAll().size());
    }
}
