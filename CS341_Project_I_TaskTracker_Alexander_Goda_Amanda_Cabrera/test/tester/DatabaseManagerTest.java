package tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tasktracker.data.DatabaseManager;

/**
 * JUnit tests for database initialization behavior.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class DatabaseManagerTest {
    @TempDir
    Path tempDir;

    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() throws Exception {
        databaseManager = new DatabaseManager(tempDir.resolve("database-manager-test.db").toString());
        databaseManager.initializeDatabase();
    }

    @Test
    void initializeDatabaseCreatesUsersAndTasksTables() throws Exception {
        int found = 0;
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('users','tasks')")) {
            while (results.next()) {
                found++;
            }
        }
        assertEquals(2, found, "Both required application tables should exist");
    }

    @Test
    void connectionEnablesForeignKeys() throws Exception {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(results.next());
            assertEquals(1, results.getInt(1));
        }
    }
}
