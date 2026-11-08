package tasktracker.view.theme;

import tasktracker.view.components.DatePickerField;
import tasktracker.view.components.CalendarPanel;
import tasktracker.view.components.SceneFields;
import tasktracker.view.presentation.DisplayDates;
import tasktracker.view.screens.DashboardSurface;
import tasktracker.view.screens.InlineEditorSurface;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import tasktracker.controller.*;
import tasktracker.data.*;
import tasktracker.view.AppWorkspace;

/** Palette, calendar and active workspace date checks using a disposable SQLite file. */
public final class ScenePaletteDateCheck {
    private static DatePickerField picker;
    private static JTextField field;
    private static JRootPane root;

    public static void main(String[] args) throws Exception {

        if (!DisplayDates.format(DisplayDates.parse("2/29/2028")).equals("02/29/2028"))
            throw new AssertionError("Leap date");
        for (String invalid : new String[] { "02/29/2027", "13/01/2026", "04/31/2026", "2026-11-02" }) {
            try {
                DisplayDates.parse(invalid);
                throw new AssertionError("Accepted " + invalid);
            } catch (DateTimeParseException expected) {
            }
        }
        if (DisplayDates.parse(" ") != null)
            throw new AssertionError("Optional date");
        SwingUtilities.invokeAndWait(ScenePaletteDateCheck::checkCalendarBoundaries);
        double primaryMin = 100, fieldMin = 100, glassMin = 100;
        SceneRenderer renderer = new SceneRenderer();
        Rectangle card = new Rectangle(25, 100, 650, 350);
        for (LastLightScene scene : LastLightScene.values()) {
            SceneRenderState.weights = scene.weights();
            primaryMin = Math.min(primaryMin,
                    contrast(AdaptiveScenePalette.buttonText(), AdaptiveScenePalette.accent()));
            fieldMin = Math.min(fieldMin, contrast(ThemePalette.INK, AdaptiveScenePalette.field()));
            BufferedImage image = CachedSceneTransition.render(renderer,
                    new CachedSceneTransition.Request(900, 600, new Rectangle[] { card }, scene.weights())).background;
            for (int y = 120; y < 430; y += 12)
                for (int x = 45; x < 650; x += 12)
                    glassMin = Math.min(glassMin,
                            contrast(AdaptiveScenePalette.secondaryText(), new Color(image.getRGB(x, y))));
        }
        System.out.printf("Minimum sampled contrast: button %.2f, field %.2f, secondary on glass %.2f%n", primaryMin,
                fieldMin, glassMin);
        if (primaryMin < 4.5 || fieldMin < 4.5 || glassMin < 4.5)
            throw new AssertionError("Contrast target");
        DatabaseManager db = new DatabaseManager(
                Files.createTempDirectory("us-date-check-").resolve("test.db").toString());
        db.initializeDatabase();
        AuthController auth = new AuthController(new UserDAO(db));
        auth.register("test-user", "test-password");
        auth.login("test-user", "test-password");
        TaskController tasks = new TaskController(new TaskDAO(db), auth);
        SwingUtilities.invokeAndWait(() -> {
            try {
                AppWorkspace workspace = new AppWorkspace(auth, tasks);
                Method show = AppWorkspace.class.getDeclaredMethod("showDashboard");
                show.setAccessible(true);
                show.invoke(workspace);
                Field dashboardField = AppWorkspace.class.getDeclaredField("dashboard");
                dashboardField.setAccessible(true);
                DashboardSurface dashboard = (DashboardSurface) dashboardField.get(workspace);
                dashboard.newTask.doClick(0);
                Field editorField = AppWorkspace.class.getDeclaredField("editor");
                editorField.setAccessible(true);
                InlineEditorSurface editor = (InlineEditorSurface) editorField.get(workspace);
                editor.title.setText("US date test");
                editor.due.setText("02/30/2028");
                editor.save.doClick(0);
                if (!tasks.getRelevantTasks().isEmpty() || editor.feedback.getText().isEmpty())
                    throw new AssertionError("Invalid date saved");
                editor.due.setText("02/29/2028");
                editor.save.doClick(0);
                if (!tasks.getRelevantTasks().get(0).getDueDate().equals(LocalDate.of(2028, 2, 29)))
                    throw new AssertionError("Save conversion");
                dashboard.edit.doClick(0);
                editor = (InlineEditorSurface) editorField.get(workspace);
                if (!editor.due.getText().equals("02/29/2028"))
                    throw new AssertionError("Edit display");
                dashboard.removeNotify();
                workspace.removeNotify();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            field = SceneFields.text();
            field.setText("11/02/2026");
            picker = new DatePickerField(field);
            root = new JRootPane();
            root.setSize(650, 650);
            root.getContentPane().setLayout(null);
            picker.setBounds(50, 80, 450, 44);
            root.getContentPane().add(picker);
            layout(root);
            ((JButton) picker.getComponent(1)).doClick(0);
        });
        Thread.sleep(220);
        SwingUtilities.invokeAndWait(() -> {
            try {
                Field f = DatePickerField.class.getDeclaredField("overlay");
                f.setAccessible(true);
                JPanel overlay = (JPanel) f.get(picker);
                JButton date = find(overlay, "2026-11-03");
                if (date == null)
                    throw new AssertionError("Calendar missing date");
                date.doClick(0);
                if (!field.getText().equals("11/03/2026") || f.get(picker) != null)
                    throw new AssertionError("Calendar format/close");
                for (LocalDate boundary : new LocalDate[] { LocalDate.MIN, LocalDate.MAX }) {
                    field.setText(DisplayDates.format(boundary));
                    ((JButton) picker.getComponent(1)).doClick(0);
                    JButton boundaryDate = find((JPanel) f.get(picker), boundary.toString());
                    if (boundaryDate == null)
                        throw new AssertionError("Calendar missing boundary date");
                    boundaryDate.doClick(0);
                    if (!field.getText().equals(DisplayDates.format(boundary)) || f.get(picker) != null)
                        throw new AssertionError("Boundary date format/close");
                }
                ((JButton) picker.getComponent(1)).doClick(0);
                picker.removeNotify();
                if (f.get(picker) != null)
                    throw new AssertionError("Calendar removal");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(
                "PASS: strict US dates, representable calendar boundaries and month navigation, actual workspace create/edit/validation, calendar selection and animation cleanup.");
        System.exit(0);
    }

    private static void checkCalendarBoundaries() {
        LocalDate[] chosen = { null };
        CalendarPanel upper = new CalendarPanel(DisplayDates.parse("12/15/+999999999"),
                date -> chosen[0] = date, () -> { });
        checkCalendarGrid(upper, YearMonth.from(LocalDate.MAX));
        checkNavigation(upper, true, false);
        upper.changeMonth(1);
        upper.changeMonth(Integer.MAX_VALUE);
        checkCalendarGrid(upper, YearMonth.from(LocalDate.MAX));
        upper.changeMonth(-1);
        checkCalendarGrid(upper, YearMonth.of(999999999, 11));
        checkNavigation(upper, true, true);
        upper.changeMonth(1);
        find(upper, LocalDate.MAX.toString()).doClick(0);
        if (!LocalDate.MAX.equals(chosen[0]))
            throw new AssertionError("Maximum date selection");

        CalendarPanel lower = new CalendarPanel(LocalDate.MIN, date -> chosen[0] = date, () -> { });
        checkCalendarGrid(lower, YearMonth.from(LocalDate.MIN));
        checkNavigation(lower, false, true);
        lower.changeMonth(-1);
        lower.changeMonth(Integer.MIN_VALUE);
        checkCalendarGrid(lower, YearMonth.from(LocalDate.MIN));
        lower.changeMonth(1);
        checkCalendarGrid(lower, YearMonth.of(-999999999, 2));
        checkNavigation(lower, true, true);
        lower.changeMonth(-1);
        find(lower, LocalDate.MIN.toString()).doClick(0);
        if (!LocalDate.MIN.equals(chosen[0]))
            throw new AssertionError("Minimum date selection");

        CalendarPanel ordinary = new CalendarPanel(LocalDate.of(2028, 2, 29), date -> { }, () -> { });
        checkCalendarGrid(ordinary, YearMonth.of(2028, 2));
        checkNavigation(ordinary, true, true);
        ordinary.changeMonth(-1);
        checkCalendarGrid(ordinary, YearMonth.of(2028, 1));
        ordinary.changeMonth(-1);
        checkCalendarGrid(ordinary, YearMonth.of(2027, 12));
        ordinary.changeMonth(2);
        checkCalendarGrid(ordinary, YearMonth.of(2028, 2));
    }

    private static void checkNavigation(CalendarPanel calendar, boolean previous, boolean next) {
        if (find(calendar, "Previous month").isEnabled() != previous
                || find(calendar, "Next month").isEnabled() != next)
            throw new AssertionError("Calendar navigation availability");
    }

    private static void checkCalendarGrid(CalendarPanel calendar, YearMonth month) {
        JPanel grid = (JPanel) ((BorderLayout) calendar.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (grid.getComponentCount() != 49)
            throw new AssertionError("Calendar grid size");
        Set<LocalDate> visible = new HashSet<>();
        for (int i = 7; i < 49; i++) {
            Component cell = grid.getComponent(i);
            if (cell instanceof JButton) {
                LocalDate date = LocalDate.parse(cell.getAccessibleContext().getAccessibleName());
                if (!visible.add(date) || date.getDayOfWeek().getValue() - 1 != (i - 7) % 7)
                    throw new AssertionError("Calendar date identity or weekday alignment");
            } else if (!(cell instanceof JLabel) || !((JLabel) cell).getText().isEmpty()) {
                throw new AssertionError("Unsupported calendar date must be an empty cell");
            }
        }
        for (int day = 1; day <= month.lengthOfMonth(); day++)
            if (!visible.contains(month.atDay(day)))
                throw new AssertionError("Missing calendar date " + month.atDay(day));
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component x : c.getComponents())
            if (x instanceof Container)
                layout((Container) x);
    }

    private static JButton find(Container c, String name) {
        for (Component x : c.getComponents()) {
            if (x instanceof JButton && name.equals(x.getAccessibleContext().getAccessibleName()))
                return (JButton) x;
            if (x instanceof Container) {
                JButton result = find((Container) x, name);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private static double channel(int c) {
        double v = c / 255.0;
        return v <= .04045 ? v / 12.92 : Math.pow((v + .055) / 1.055, 2.4);
    }

    private static double luminance(Color c) {
        return .2126 * channel(c.getRed()) + .7152 * channel(c.getGreen()) + .0722 * channel(c.getBlue());
    }

    private static double contrast(Color a, Color b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
    }
}
