package tester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tasktracker.data.DatabaseManager;

/**
 * JUnit tests for database initialization and connection setup.
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

    @Test
    void connectionClosesWhenForeignKeySetupFails() throws Exception {
        verifyFailedConnectionCleanup(null);
    }

    @Test
    void connectionCloseFailureDoesNotReplaceSetupFailure() throws Exception {
        verifyFailedConnectionCleanup(new SQLException("Connection close failed"));
    }

    private void verifyFailedConnectionCleanup(SQLException closeFailure) throws Exception {
        SQLException setupFailure = new SQLException("Foreign-key setup failed");
        AtomicBoolean statementClosed = new AtomicBoolean();
        AtomicBoolean connectionCloseAttempted = new AtomicBoolean();
        Statement statement = (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                new Class<?>[] {Statement.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("execute")) {
                        throw setupFailure;
                    }
                    if (method.getName().equals("close")) {
                        statementClosed.set(true);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("createStatement")) {
                        return statement;
                    }
                    if (method.getName().equals("close")) {
                        connectionCloseAttempted.set(true);
                        if (closeFailure != null) {
                            throw closeFailure;
                        }
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        // A missing parent makes the SQLite driver fall through to this test driver.
        String databaseFile = tempDir.resolve("missing-parent/setup-failure.db").toString();
        String url = "jdbc:sqlite:" + databaseFile;
        Driver driver = new Driver() {
            @Override
            public Connection connect(String candidate, Properties properties) {
                return acceptsURL(candidate) ? connection : null;
            }

            @Override
            public boolean acceptsURL(String candidate) {
                return url.equals(candidate);
            }

            @Override
            public DriverPropertyInfo[] getPropertyInfo(String candidate, Properties properties) {
                return new DriverPropertyInfo[0];
            }

            @Override
            public int getMajorVersion() {
                return 1;
            }

            @Override
            public int getMinorVersion() {
                return 0;
            }

            @Override
            public boolean jdbcCompliant() {
                return false;
            }

            @Override
            public Logger getParentLogger() {
                return Logger.getGlobal();
            }
        };
        DriverManager.registerDriver(driver);
        try {
            SQLException actual = assertThrows(SQLException.class,
                    () -> new DatabaseManager(databaseFile).getConnection());
            assertSame(setupFailure, actual);
            assertTrue(statementClosed.get(), "The setup statement should close after failure");
            assertTrue(connectionCloseAttempted.get(), "The failed connection should be closed");
            assertEquals(closeFailure == null ? 0 : 1, actual.getSuppressed().length);
            if (closeFailure != null) {
                assertSame(closeFailure, actual.getSuppressed()[0]);
            }
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }
}
