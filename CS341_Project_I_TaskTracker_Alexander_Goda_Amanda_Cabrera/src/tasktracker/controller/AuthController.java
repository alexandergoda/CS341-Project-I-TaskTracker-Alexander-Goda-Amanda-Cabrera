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

    public AuthController(UserDAO userDAO) {
        if (userDAO == null) {
            throw new IllegalArgumentException("userDAO is required");
        }
        this.userDAO = userDAO;
    }

    /**
     * Registers a new local account.
     *
     * @param username requested username
     * @param password raw password entered by the user
     * @return newly created user
     * @throws SQLException if the account cannot be stored
     */
    public User register(String username, String password) throws SQLException {
        validateUsernameAndPassword(username, password);
        if (userDAO.usernameExists(username)) {
            throw new IllegalArgumentException("username is already registered");
        }
        return userDAO.createUser(username, hashPassword(password));
    }

    /**
     * Attempts to log in and stores the authenticated user when successful.
     *
     * @return true when the credentials match a registered account
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

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Returns registered users for task-assignment controls.
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
