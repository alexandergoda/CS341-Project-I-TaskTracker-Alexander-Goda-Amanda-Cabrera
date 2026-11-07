package tester;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tasktracker.controller.AuthController;
import tasktracker.data.DatabaseManager;
import tasktracker.data.UserDAO;

/** Password changes operate only on disposable test accounts. */
public class PasswordChangeTest {
    @TempDir
    Path directory;
    private AuthController auth;
    private DatabaseManager database;

    @BeforeEach
    void setup() throws Exception {
        database = new DatabaseManager(directory.resolve("passwords.db").toString());
        database.initializeDatabase();
        auth = new AuthController(new UserDAO(database));
        auth.register("owner", "old password");
        auth.register("other", "other password");
        auth.login("owner", "old password");
    }

    @Test
    void changesOnlyCurrentAccountAndPersistsAcrossSessions() throws Exception {
        int id = auth.getCurrentUser().getUserId();
        auth.changePassword("old password", "new password", "new password");
        assertEquals(id, auth.getCurrentUser().getUserId());
        AuthController fresh = new AuthController(new UserDAO(database));
        assertFalse(fresh.login("owner", "old password"));
        assertTrue(fresh.login("owner", "new password"));
        assertTrue(fresh.login("other", "other password"));
        try (java.sql.Connection connection = database.getConnection();
                java.sql.PreparedStatement statement = connection
                        .prepareStatement("SELECT password_hash FROM users WHERE user_id = ?")) {
            statement.setInt(1, id);
            try (java.sql.ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertNotEquals("new password", rows.getString(1));
                assertEquals(64, rows.getString(1).length());
            }
        }
    }

    @Test
    void wrongCurrentPasswordDoesNotChangePasswordOrSession() throws Exception {
        int id = auth.getCurrentUser().getUserId();
        assertThrows(IllegalArgumentException.class, () -> auth.changePassword("wrong", "new", "new"));
        assertEquals(id, auth.getCurrentUser().getUserId());
        assertTrue(auth.login("owner", "old password"));
    }

    @Test
    void rejectsInvalidInputWithoutChangingStoredPassword() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> auth.changePassword("", "new", "new"));
        assertThrows(IllegalArgumentException.class, () -> auth.changePassword("old password", "", ""));
        assertThrows(IllegalArgumentException.class, () -> auth.changePassword("old password", "new", "different"));
        assertThrows(IllegalArgumentException.class,
                () -> auth.changePassword("old password", "old password", "old password"));
        assertThrows(IllegalArgumentException.class, () -> auth.changePassword(null, null, null));
        assertTrue(auth.login("owner", "old password"));
    }

    @Test
    void signedOutUsersCannotChangePasswords() {
        auth.logout();
        assertThrows(IllegalStateException.class, () -> auth.changePassword("old password", "new", "new"));
    }

    @Test
    void stalePasswordCannotOverwriteNewerChange() throws Exception {
        AuthController second = new AuthController(new UserDAO(database));
        second.login("owner", "old password");
        auth.changePassword("old password", "first new", "first new");
        assertThrows(IllegalArgumentException.class,
                () -> second.changePassword("old password", "second new", "second new"));
        assertTrue(second.login("owner", "first new"));
    }
}
