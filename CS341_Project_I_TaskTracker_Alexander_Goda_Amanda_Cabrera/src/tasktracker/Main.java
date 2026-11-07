package tasktracker;

import java.awt.EventQueue;
import java.sql.SQLException;

import tasktracker.controller.AuthController;
import tasktracker.controller.TaskController;
import tasktracker.data.DatabaseManager;
import tasktracker.data.TaskDAO;
import tasktracker.data.UserDAO;
import tasktracker.view.TaskTrackerUI;

/**
 * Application entry point for Task Tracker. Initializes the database and starts the Swing user interface.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class Main {

    /**
     * Initializes the local database and schedules the login UI on the Swing event thread. Database initialization
     * failures are printed to standard error and prevent UI startup.
     *
     * @param args command-line arguments; ignored
     */
    public static void main(String[] args) {
        DatabaseManager databaseManager = new DatabaseManager();

        try {
            databaseManager.initializeDatabase();

            System.out.println("Task Tracker database initialized successfully.");
            System.out.println("Database file: " + databaseManager.getDatabaseFile());

            UserDAO userDAO = new UserDAO(databaseManager);
            TaskDAO taskDAO = new TaskDAO(databaseManager);

            AuthController authController = new AuthController(userDAO);
            TaskController taskController = new TaskController(taskDAO, authController);

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    showLogin(authController, taskController);
                }
            });

        } catch (SQLException exception) {
            System.err.println("Task Tracker could not initialize its database: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /** Opens the adaptive UI using the existing application controllers. */
    private static void showLogin(AuthController authController, TaskController taskController) {
        TaskTrackerUI.launch(authController, taskController);
    }
}
