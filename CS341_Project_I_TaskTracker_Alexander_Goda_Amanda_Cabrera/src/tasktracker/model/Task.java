package tasktracker.model;

import java.time.LocalDate;

/**
 * Represents one Task Tracker task.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class Task {
    private int taskId;
    private String title;
    private String description;
    private final User creator;
    private User assignee;
    private LocalDate dueDate;
    private Priority priority;
    private boolean completed;

    /**
     * Constructs a new unsaved task.
     */
    public Task(String title, String description, User creator, User assignee,
            LocalDate dueDate, Priority priority) {
        this(0, title, description, creator, assignee, dueDate, priority, false);
    }

    /**
     * Constructs a task loaded from persistence.
     */
    public Task(int taskId, String title, String description, User creator, User assignee,
            LocalDate dueDate, Priority priority, boolean completed) {
        if (taskId < 0) {
            throw new IllegalArgumentException("taskId cannot be negative");
        }
        if (creator == null) {
            throw new IllegalArgumentException("creator is required");
        }
        this.taskId = taskId;
        setTitle(title);
        setDescription(description);
        this.creator = creator;
        this.assignee = assignee;
        this.dueDate = dueDate;
        this.priority = priority == null ? Priority.MEDIUM : priority;
        this.completed = completed;
    }

    public int getTaskId() {
        return taskId;
    }

    /**
     * Assigns the generated database identifier after a successful insert.
     *
     * @param taskId positive database identifier
     */
    public void setTaskId(int taskId) {
        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public User getCreator() {
        return creator;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority == null ? Priority.MEDIUM : priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        String assignedTo = assignee == null ? "Unassigned" : assignee.getUsername();
        String due = dueDate == null ? "None" : dueDate.toString();
        return "Task " + taskId + " | " + title + " | creator=" + creator.getUsername()
                + " | assignedTo=" + assignedTo + " | due=" + due + " | priority=" + priority
                + " | completed=" + completed;
    }
}
