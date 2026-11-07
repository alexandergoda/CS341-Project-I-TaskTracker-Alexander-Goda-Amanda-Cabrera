package tasktracker.view.presentation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import tasktracker.model.Task;

/** Presentation ordering only. Controllers check permissions; DAOs handle persistence. */
public final class TaskListOrder {
    private TaskListOrder() {
    }

    public static final Comparator<Task> BY_ATTENTION = Comparator.comparing(Task::isCompleted)
            .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::getPriority, Comparator.reverseOrder()).thenComparingInt(Task::getTaskId);

    public static String group(LocalDate due, boolean completed, LocalDate today) {
        if (completed)
            return "Completed";
        if (due == null)
            return "No date";
        if (due.isBefore(today))
            return "Overdue";
        if (due.equals(today))
            return "Today";
        if (due.equals(today.plusDays(1)))
            return "Tomorrow";
        return "Upcoming";
    }

    public static String date(LocalDate due, LocalDate today) {
        if (due == null)
            return "No date";
        if (due.equals(today))
            return "Today";
        if (due.equals(today.plusDays(1)))
            return "Tomorrow";
        return due.format(DateTimeFormatter.ofPattern(due.getYear() == today.getYear() ? "MMM d" : "MMM d, yyyy"));
    }
}
