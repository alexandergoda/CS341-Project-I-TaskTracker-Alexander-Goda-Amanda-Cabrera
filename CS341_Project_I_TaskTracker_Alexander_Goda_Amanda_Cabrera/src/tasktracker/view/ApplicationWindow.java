package tasktracker.view;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import tasktracker.controller.AuthController;
import tasktracker.controller.TaskController;
import tasktracker.view.animation.TransitionHost;
import tasktracker.view.screens.StartupSurface;
import javax.swing.SwingWorker;
import java.awt.Container;
import java.awt.Component;

/** The application's single top-level window. */
public final class ApplicationWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    private SwingWorker<?, ?> startupWorker;

    public ApplicationWindow(AuthController auth, TaskController tasks) {
        super("Task Tracker");
        setSize(1280, 820);
        setMinimumSize(new Dimension(1100, 760));
        setLocationRelativeTo(null);
        setBackground(StartupSurface.BACKGROUND);
        getRootPane().setBackground(StartupSurface.BACKGROUND);
        AppWorkspace workspace = new AppWorkspace(auth, tasks);
        workspace.setBackground(StartupSurface.BACKGROUND);
        TransitionHost startup = new TransitionHost();
        startup.setOpaque(true);
        startup.setBackground(StartupSurface.BACKGROUND);
        startup.showContent(new StartupSurface(), false);
        setContentPane(startup);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                prepareLogin(startup, workspace);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                workspace.requestClose(() -> {
                    if (startupWorker != null)
                        startupWorker.cancel(true);
                    dispose();
                });
            }
        });
    }

    private void prepareLogin(TransitionHost startup, AppWorkspace workspace) {
        Dimension size = startup.getSize();
        workspace.setSize(size);
        layoutTree(workspace);
        startupWorker = workspace.prepareStartup(() -> {
            if (!isDisplayable())
                return;
            if (!size.equals(startup.getSize())) {
                prepareLogin(startup, workspace);
                return;
            }
            startup.showContent(workspace, true);
        });
    }

    private static void layoutTree(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents())
            if (child instanceof Container)
                layoutTree((Container) child);
    }
}
