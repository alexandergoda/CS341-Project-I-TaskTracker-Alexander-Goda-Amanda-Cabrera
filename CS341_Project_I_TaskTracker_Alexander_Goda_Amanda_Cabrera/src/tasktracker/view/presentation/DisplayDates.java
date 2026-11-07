package tasktracker.view.presentation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** US date entry at the UI boundary. Storage continues using ISO LocalDate values. */
public final class DisplayDates {
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter OUTPUT = DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US);

    private DisplayDates() {
    }

    public static LocalDate parse(String value) {
        return value == null || value.trim().isEmpty() ? null : LocalDate.parse(value.trim(), INPUT);
    }

    public static String format(LocalDate value) {
        return value == null ? "" : OUTPUT.format(value);
    }
}
