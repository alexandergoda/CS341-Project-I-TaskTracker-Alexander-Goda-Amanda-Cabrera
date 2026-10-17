package tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tasktracker.controller.AuthController;
import tasktracker.data.DatabaseManager;
import tasktracker.data.UserDAO;
import tasktracker.model.User;

/**
 * JUnit tests for registration, login, logout, and authenticated-user state.
 *
 * @author Alexander Goda
 */
public class AuthControllerTest {
    @TempDir
    Path tempDir;

    private AuthController authController;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager databaseManager =
                new DatabaseManager(tempDir.resolve("auth-controller-test.db").toString());
        databaseManager.initializeDatabase();

        UserDAO userDAO = new UserDAO(databaseManager);
        authController = new AuthController(userDAO);
    }

    @Test
    void registrationCreatesUserAndAllowsLogin() throws Exception {
        User created = authController.register("alex", "Password123");

        assertNotNull(created);
        assertEquals("alex", created.getUsername());

        assertTrue(authController.login("alex", "Password123"));
        assertTrue(authController.isLoggedIn());
        assertEquals(created.getUserId(), authController.getCurrentUser().getUserId());
    }

    @Test
    void invalidLoginDoesNotAuthenticate() throws Exception {
        authController.register("alex", "Password123");

        assertFalse(authController.login("alex", "WrongPassword"));
        assertFalse(authController.isLoggedIn());
        assertNull(authController.getCurrentUser());
    }

    @Test
    void logoutClearsCurrentUser() throws Exception {
        authController.register("alex", "Password123");
        assertTrue(authController.login("alex", "Password123"));

        authController.logout();

        assertFalse(authController.isLoggedIn());
        assertNull(authController.getCurrentUser());
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        authController.register("alex", "Password123");

        assertThrows(
                IllegalArgumentException.class,
                () -> authController.register("alex", "AnotherPassword"));
    }

    @Test
    void blankRegistrationIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> authController.register("   ", "Password123"));

        assertThrows(
                IllegalArgumentException.class,
                () -> authController.register("alex", ""));
    }

    @Test
    void registeredUsersAreReturned() throws Exception {
        authController.register("alex", "Password123");
        authController.register("amanda", "Password456");

        assertEquals(2, authController.getRegisteredUsers().size());
    }
}
