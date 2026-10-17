package tasktracker.model;

import java.util.Objects;

/**
 * Represents a registered Task Tracker user.
 * Password data is intentionally kept out of the domain object and remains
 * inside the persistence/authentication boundary.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class User {
    private final int userId;
    private final String username;

    /**
     * Constructs a persisted user.
     *
     * @param userId positive database identifier
     * @param username nonblank username, trimmed here; uniqueness is enforced by storage
     * @throws IllegalArgumentException if userId is not positive or username is blank
     */
    public User(int userId, String username) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        this.userId = userId;
        this.username = username.trim();
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Provides the account name for display controls.
     *
     * @return username without password data
     */
    @Override
    public String toString() {
        return username;
    }

    /**
     * Compares persisted identity using only the user ID.
     *
     * @param obj object to compare
     * @return true for a User with the same ID, regardless of username
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof User)) {
            return false;
        }
        User other = (User) obj;
        return userId == other.userId;
    }

    /**
     * Hashes persisted identity consistently with equals.
     *
     * @return hash derived from the user ID
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
