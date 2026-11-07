package tasktracker.view.theme;

import tasktracker.view.screens.DashboardSurface;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

/** Tests completion hit targets and animation without reading or writing SQLite. */
public final class TaskCompletionInteractionCheck {
    static DashboardSurface surface;
    static AtomicInteger requests = new AtomicInteger(), finished = new AtomicInteger();

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            surface = new DashboardSurface("Test");
            surface.model.addRow(new Object[] { 1, "First task", "alex", "alex", "", "HIGH", "Open" });
            surface.model.addRow(new Object[] { 2, "Second task", "alex", "alex", "", "LOW", "Open" });
            surface.setSize(1280, 820);
            layout(surface);
            surface.onCompletionRequested(row -> {
                if (row != 0)
                    throw new AssertionError("Wrong row");
                requests.incrementAndGet();
            });
            Rectangle cell = surface.table.getCellRect(0, 0, true);
            if (!DashboardSurface.isCompletionTarget(cell, new Point(30, cell.height / 2)))
                throw new AssertionError("Square missed");
            if (DashboardSurface.isCompletionTarget(cell, new Point(90, cell.height / 2)))
                throw new AssertionError("Title toggles task");
            MouseEvent click = new MouseEvent(surface.table, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    30, cell.height / 2, 1, false, MouseEvent.BUTTON1);
            for (MouseListener listener : surface.table.getMouseListeners())
                listener.mouseClicked(click);
            if (requests.get() != 1)
                throw new AssertionError("Click not delivered");
            surface.table.setRowSelectionInterval(0, 0);
            surface.table.getActionMap().get("toggleTaskCompletion")
                    .actionPerformed(new ActionEvent(surface.table, 0, ""));
            if (requests.get() != 2)
                throw new AssertionError("Space not delivered");
            surface.animateCompletion(0, true, finished::incrementAndGet);
            if (surface.table.isEnabled())
                throw new AssertionError("Repeated interaction not blocked");
        });
        Thread.sleep(350);
        SwingUtilities.invokeAndWait(() -> {
            if (surface.table.getRowHeight(0) >= surface.table.getRowHeight())
                throw new AssertionError("Row did not shrink");
        });
        Thread.sleep(450);
        SwingUtilities.invokeAndWait(() -> {
            if (finished.get() != 1 || !surface.table.isEnabled())
                throw new AssertionError("Animation completion failed");
            if (surface.table.getRowHeight(0) != surface.table.getRowHeight())
                throw new AssertionError("Height not restored");
            surface.removeNotify();
        });
        System.out.println(
                "PASS: checkbox hit area, click, Space, repeat protection, row collapse, restoration and one completion callback.");
        System.exit(0);
    }

    static void layout(Container c) {
        c.doLayout();
        for (Component x : c.getComponents())
            if (x instanceof Container)
                layout((Container) x);
    }
}
