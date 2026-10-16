package tasktracker.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import tasktracker.model.User;

/**
 * Data-access operations for registered users.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class UserDAO {
    private final DatabaseManager databaseManager;

    public UserDAO(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("databaseManager is required");
        }
        this.databaseManager = databaseManager;
    }

    /**
     * Creates a registered user. This method accepts a password hash rather than
     * a raw password so credential transformation can remain outside persistence.
     */
    public User createUser(String username, String passwordHash) throws SQLException {
        validateCredentials(username, passwordHash);
        String sql = "INSERT INTO users(username, password_hash) VALUES(?, ?)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username.trim());
            statement.setString(2, passwordHash);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new User(keys.getInt(1), username.trim());
                }
            }
        }

        User saved = findByUsername(username);
        if (saved == null) {
            throw new SQLException("User was inserted but could not be loaded.");
        }
        return saved;
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT user_id, username FROM users WHERE user_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapUser(results) : null;
            }
        }
    }

    public User findByUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT user_id, username FROM users WHERE username = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapUser(results) : null;
            }
        }
    }

    /**
     * Returns the matching user when the supplied username and already-hashed
     * password match a stored account; otherwise returns null.
     */
    public User authenticate(String username, String passwordHash) throws SQLException {
        if (username == null || username.trim().isEmpty() || passwordHash == null) {
            return null;
        }
        String sql = "SELECT user_id, username FROM users WHERE username = ? AND password_hash = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            statement.setString(2, passwordHash);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapUser(results) : null;
            }
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        return findByUsername(username) != null;
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT user_id, username FROM users ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                users.add(mapUser(results));
            }
        }
        return users;
    }

    private void validateCredentials(String username, String passwordHash) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (passwordHash == null || passwordHash.isEmpty()) {
            throw new IllegalArgumentException("passwordHash must not be empty");
        }
    }

    private User mapUser(ResultSet results) throws SQLException {
        return new User(results.getInt("user_id"), results.getString("username"));
    }
}
