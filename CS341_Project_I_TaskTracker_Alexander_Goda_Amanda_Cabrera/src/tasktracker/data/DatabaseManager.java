package tasktracker.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates SQLite connections and initializes the Task Tracker schema.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class DatabaseManager {
    public static final String DEFAULT_DATABASE_FILE = "tasks.db";

    private static final String CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS users ("
            + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "username TEXT NOT NULL UNIQUE, "
            + "password_hash TEXT NOT NULL"
            + ")";

    private static final String CREATE_TASKS =
            "CREATE TABLE IF NOT EXISTS tasks ("
            + "task_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "title TEXT NOT NULL, "
            + "description TEXT, "
            + "creator_id INTEGER NOT NULL, "
            + "assignee_id INTEGER, "
            + "due_date TEXT, "
            + "priority TEXT NOT NULL DEFAULT 'MEDIUM' "
            + "CHECK (priority IN ('LOW','MEDIUM','HIGH')), "
            + "completed INTEGER NOT NULL DEFAULT 0 "
            + "CHECK (completed IN (0,1)), "
            + "FOREIGN KEY (creator_id) REFERENCES users(user_id), "
            + "FOREIGN KEY (assignee_id) REFERENCES users(user_id)"
            + ")";

    private final String databaseFile;

    /**
     * Creates a manager for tasks.db relative to the working directory.
     * Construction does not open a connection or create tables.
     */
    public DatabaseManager() {
        this(DEFAULT_DATABASE_FILE);
    }

    /**
     * Creates a manager for a specified SQLite file. The overload is useful for
     * JUnit because every test can use an isolated temporary database.
     *
     * @param databaseFile local SQLite file path; relative paths use the working directory
     * @throws IllegalArgumentException if the path is null or blank
     */
    public DatabaseManager(String databaseFile) {
        if (databaseFile == null || databaseFile.trim().isEmpty()) {
            throw new IllegalArgumentException("databaseFile must not be blank");
        }
        this.databaseFile = databaseFile;
    }

    public String getDatabaseFile() {
        return databaseFile;
    }

    /**
     * Opens a SQLite connection and enables foreign-key enforcement.
     *
     * @return open JDBC connection that the caller must close
     * @throws SQLException if the driver or database connection is unavailable
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver was not found on the classpath.", e);
        }

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /**
     * Creates the production tables on first run. Calling this method again is
     * safe because both statements use IF NOT EXISTS. Connections are closed here.
     *
     * @throws SQLException if connecting or creating either table fails
     */
    public void initializeDatabase() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_USERS);
            statement.executeUpdate(CREATE_TASKS);
        }
    }
}
