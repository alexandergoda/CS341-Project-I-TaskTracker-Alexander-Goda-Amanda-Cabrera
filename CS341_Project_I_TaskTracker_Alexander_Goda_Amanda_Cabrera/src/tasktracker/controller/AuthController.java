package tasktracker.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import tasktracker.data.UserDAO;
import tasktracker.model.User;

/**
 * Coordinates user registration, login, logout, and current-user state.
 *
 * @author Alexander Goda
 */
public class AuthController {
    private final UserDAO userDAO;
    private User currentUser;

    /**
     * Creates a controller with no authenticated session.
     *
     * @param userDAO required persistence dependency
     * @throws IllegalArgumentException if a required dependency is null
     */
    public AuthController(UserDAO userDAO) {
        if (userDAO == null) {
            throw new IllegalArgumentException("userDAO is required");
        }
        this.userDAO = userDAO;
    }

    /**
     * Registers a local account after hashing the password with SHA-256.
     *
     * @param username nonblank username; surrounding whitespace is removed
     * @param password nonempty raw password; whitespace is preserved
     * @return newly persisted user; does not log the user in
     * @throws IllegalArgumentException if username is blank, password is null or empty,
     *         or the username is taken
     * @throws SQLException if database access fails
     */
    public User register(String username, String password) throws SQLException {
        validateUsernameAndPassword(username, password);
        if (userDAO.usernameExists(username)) {
            throw new IllegalArgumentException("username is already registered");
        }
        return userDAO.createUser(username, hashPassword(password));
    }

    /**
     * Authenticates credentials and replaces the current session user.
     * Invalid or unmatched credentials clear the session; database errors propagate.
     *
     * @param username account username, trimmed for lookup
     * @param password raw password to hash and compare
     * @return true if authenticated; false for invalid or unmatched credentials
     * @throws SQLException if database access fails
     */
    public boolean login(String username, String password) throws SQLException {
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            currentUser = null;
            return false;
        }

        currentUser = userDAO.authenticate(username, hashPassword(password));
        return currentUser != null;
    }

    /**
     * Ends the current authenticated session.
     */
    public void logout() {
        currentUser = null;
    }

    /**
     * Returns the current session identity.
     *
     * @return authenticated user, or null while logged out
     */
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Loads users for assignment controls; does not require a login.
     *
     * @return registered users ordered by username, possibly empty
     * @throws SQLException if database access fails
     */
    public List<User> getRegisteredUsers() throws SQLException {
        return userDAO.findAll();
    }

    private void validateUsernameAndPassword(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
