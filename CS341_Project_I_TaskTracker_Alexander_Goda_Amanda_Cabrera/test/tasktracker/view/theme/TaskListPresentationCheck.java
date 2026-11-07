package tasktracker.view.theme;

import tasktracker.view.presentation.TaskListOrder;
import tasktracker.view.screens.DashboardSurface;
import tasktracker.view.screens.InlineEditorSurface;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.lang.reflect.Field;
import tasktracker.model.*;

/** Deterministic date rules and rendered controls. Never connects to a database. */
public final class TaskListPresentationCheck {
    public static void main(String[] args) throws Exception {

        LocalDate today = LocalDate.of(2026, 12, 31);
        User user = new User(1, "Test");
        Task high = new Task(1, "High", "", user, null, today, Priority.HIGH, false);
        Task low = new Task(2, "Low", "", user, null, today, Priority.LOW, false);
        Task earlier = new Task(3, "Earlier", "", user, null, today.minusDays(1), Priority.LOW, false);
        Task undated = new Task(4, "Undated", "", user, null, null, Priority.HIGH, false);
        Task done = new Task(5, "Done", "", user, null, today.minusDays(3), Priority.HIGH, true);
        java.util.List<Task> sorted = new ArrayList<>(Arrays.asList(done, undated, low, high, earlier));
        sorted.sort(TaskListOrder.BY_ATTENTION);
        if (!sorted.equals(Arrays.asList(earlier, high, low, undated, done)))
            throw new AssertionError("Date/priority/completion order");
        if (!TaskListOrder.group(today.plusDays(1), false, today).equals("Tomorrow"))
            throw new AssertionError("Year boundary");
        if (!TaskListOrder.group(null, false, today).equals("No date"))
            throw new AssertionError("Undated");
        if (!TaskListOrder.group(today.minusDays(1), true, today).equals("Completed"))
            throw new AssertionError("Completed precedence");
        SwingUtilities.invokeAndWait(() -> {
            SceneTheme.mode = "Dark";
            DashboardSurface d = new DashboardSurface("Test");
            LocalDate now = LocalDate.now();
            d.model.addRow(new Object[] { 1, "Finish presentation", "alex", "amanda", now.minusDays(1).toString(),
                    "HIGH", "Open" });
            d.model.addRow(new Object[] { 2, "Review UML diagram", "alex", "alex", now.toString(), "MEDIUM", "Open" });
            d.model.addRow(new Object[] { 3, "Check saved tasks", "alex", "alex", now.toString(), "LOW", "Open" });
            d.model.addRow(
                    new Object[] { 4, "Prepare demo", "alex", "alex", now.plusDays(1).toString(), "HIGH", "Open" });
            d.model.addRow(new Object[] { 5, "Explore styles", "alex", "alex", "", "LOW", "Open" });
            d.updateTaskGroups();
            if (d.table.getRowHeight(0) != 106 || d.table.getRowHeight(2) != 76)
                throw new AssertionError("Group heights");
            final int[] hit = { -1 };
            d.onCompletionRequested(row -> hit[0] = row);
            Rectangle cell = d.table.getCellRect(1, 0, true);
            MouseEvent event = new MouseEvent(d.table, MouseEvent.MOUSE_CLICKED, 0, 0, 30, cell.y + 30 + 38, 1, false,
                    MouseEvent.BUTTON1);
            for (MouseListener l : d.table.getMouseListeners())
                l.mouseClicked(event);
            if (hit[0] != 1)
                throw new AssertionError("Grouped checkbox mapping");
            checkFocusMetadata(d, user);
            checkPriorityLocales(d);
            InlineEditorSurface editor = new InlineEditorSurface(null, Collections.emptyList());
            editor.setSize(560, 490);
            AdaptiveAppearanceCheck.layout(editor);
            Color last = null;
            for (LastLightScene scene : LastLightScene.values()) {
                SceneRenderState.weights = scene.weights();
                BufferedImage image = new BufferedImage(560, 490, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                editor.printAll(g);
                g.dispose();
                Color tint = editor.title.getBackground();
                if (last != null && last.equals(tint))
                    throw new AssertionError("Static field tint");
                last = tint;
            }
            SceneRenderState.weights = LastLightScene.NIGHT.weights();
            BufferedImage image = new BufferedImage(1080, 560, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(AdaptiveScenePalette.field());
            g.fillRect(0, 0, 1080, 560);
            d.table.setSize(500, 550);
            d.table.doLayout();
            d.table.printAll(g);
            g.translate(520, 20);
            editor.printAll(g);
            g.dispose();
            if (args.length > 0)
                try {
                    ImageIO.write(image, "png", new File(args[0]));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            d.removeNotify();
        });
        System.out.println("PASS: date groups, year boundary, row heights, checkbox mapping, visible creator/priority "
                + "for creators and assignees, deselection guidance, locale-independent priority bars, and all six editor tints.");
        System.exit(0);
    }

    private static void checkFocusMetadata(DashboardSurface dashboard, User creator) {
        User assignee = new User(2, "Amanda");
        Priority[] priorities = { Priority.LOW, Priority.MEDIUM, Priority.HIGH };
        String[] labels = { "Low", "Medium", "High" };
        for (int i = 0; i < priorities.length; i++) {
            Task task = new Task(20 + i, "Focus task", "Focus notes", creator, assignee, LocalDate.now(), priorities[i],
                    false);
            dashboard.showTask(task, creator, false);
            String text = text(dashboard, "metadata");
            if (!text.contains("Created by Test") || !text.contains("Priority: " + labels[i]))
                throw new AssertionError("Missing visible creator or " + labels[i] + " priority");
            if (!text.contains("Due today") || !text.contains("Assigned to Amanda"))
                throw new AssertionError("Existing due date or assignee metadata lost");
            if (!text(dashboard, "notes").equals("Focus notes") || !dashboard.edit.isEnabled()
                    || !dashboard.delete.isEnabled() || !dashboard.complete.isEnabled())
                throw new AssertionError("Creator detail content or permissions changed");

            dashboard.showTask(task, assignee, false);
            if (!text(dashboard, "metadata").contains("Created by Test")
                    || !text(dashboard, "metadata").contains("Priority: " + labels[i]))
                throw new AssertionError("Assignee cannot read creator or priority");
            if (dashboard.edit.isEnabled() || dashboard.delete.isEnabled() || !dashboard.complete.isEnabled())
                throw new AssertionError("Showing metadata changed assignee permissions");
        }
        dashboard.showTask(new Task(25, "Undated focus", "", creator, null, null, Priority.HIGH, false), creator,
                false);
        if (!text(dashboard, "metadata").contains("No due date")
                || !text(dashboard, "metadata").contains("Assigned to Unassigned")
                || !text(dashboard, "metadata").contains("Created by Test")
                || !text(dashboard, "metadata").contains("Priority: High"))
            throw new AssertionError("Unassigned or undated task metadata");

        dashboard.showTask(null, creator, false);
        if (!text(dashboard, "metadata").equals("Select a task from your list.")
                || !text(dashboard, "notes").equals("Your task details will appear here."))
            throw new AssertionError("Nonempty list without selection shows empty-account guidance");
        if (dashboard.edit.isEnabled() || dashboard.delete.isEnabled() || dashboard.complete.isEnabled())
            throw new AssertionError("Deselection retained task actions");
    }

    private static String text(DashboardSurface dashboard, String name) {
        try {
            Field field = DashboardSurface.class.getDeclaredField(name);
            field.setAccessible(true);
            return ((JTextArea) field.get(dashboard)).getText();
        } catch (ReflectiveOperationException error) {
            throw new RuntimeException(error);
        }
    }

    /** Enum priorities retain their meaning even when the default locale treats I differently. */
    private static void checkPriorityLocales(DashboardSurface dashboard) {
        Locale original = Locale.getDefault();
        Locale originalDisplay = Locale.getDefault(Locale.Category.DISPLAY);
        Locale originalFormat = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.ENGLISH);
            int[][] english = renderPriorityBars(dashboard);
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            int[][] turkish = renderPriorityBars(dashboard);
            for (int row = 0; row < english.length; row++)
                if (!Arrays.equals(english[row], turkish[row]))
                    throw new AssertionError("Priority bars changed with the default locale at row " + row);
        } finally {
            Locale.setDefault(original);
            Locale.setDefault(Locale.Category.DISPLAY, originalDisplay);
            Locale.setDefault(Locale.Category.FORMAT, originalFormat);
        }
    }

    private static int[][] renderPriorityBars(DashboardSurface dashboard) {
        String[] labels = { "High", "Medium", "Low" };
        Color[] colors = { new Color(235, 145, 141), new Color(225, 188, 119), new Color(148, 190, 162) };
        int[][] pixels = new int[labels.length][3];
        for (int row = 0; row < labels.length; row++) {
            JComponent renderer = (JComponent) dashboard.table.prepareRenderer(dashboard.table.getCellRenderer(row, 0),
                    row, 0);
            String accessible = renderer.getAccessibleContext().getAccessibleName();
            if (accessible == null || !accessible.contains(labels[row] + " priority"))
                throw new AssertionError(labels[row] + " priority label changed under " + Locale.getDefault());
            int height = dashboard.table.getRowHeight(row);
            renderer.setSize(480, height);
            BufferedImage image = new BufferedImage(480, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            renderer.paint(graphics);
            graphics.dispose();
            int active = 0;
            for (int bar = 0; bar < 3; bar++) {
                pixels[row][bar] = image.getRGB(71 + bar * 15, height - 76 + 52);
                if (pixels[row][bar] == colors[row].getRGB())
                    active++;
                if ((bar < 3 - row) != (pixels[row][bar] == colors[row].getRGB()))
                    throw new AssertionError(labels[row] + " priority bar " + bar + " has incorrect color/count under "
                            + Locale.getDefault());
            }
            if (active != 3 - row)
                throw new AssertionError(
                        labels[row] + " priority needs " + (3 - row) + " active bars under " + Locale.getDefault());
        }
        return pixels;
    }
}
