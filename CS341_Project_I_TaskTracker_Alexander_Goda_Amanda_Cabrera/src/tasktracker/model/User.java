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
     * @param username unique nonblank username
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

    @Override
    public String toString() {
        return username;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
