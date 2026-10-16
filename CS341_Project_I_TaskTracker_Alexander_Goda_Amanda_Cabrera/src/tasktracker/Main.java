package tasktracker;

import java.sql.SQLException;

import tasktracker.data.DatabaseManager;

/**
 * Application entry point. During the bottom-up implementation stage this
 * verifies that the production database can initialize. The Swing views and
 * controllers will be connected here during the next implementation stage.
 *
 * @author Alexander Goda
 * @author Amanda Cabrera
 */
public class Main {
    public static void main(String[] args) {
        DatabaseManager databaseManager = new DatabaseManager();
        try {
            databaseManager.initializeDatabase();
            System.out.println("Task Tracker database initialized successfully.");
            System.out.println("Database file: " + databaseManager.getDatabaseFile());
            System.out.println("Model and persistence foundation is ready for controller/UI integration.");
        } catch (SQLException e) {
            System.err.println("Task Tracker could not initialize its database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
