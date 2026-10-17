package tasktracker.view;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import tasktracker.controller.AuthController;

/**
 * Login screen for the Task Tracker application.
 *
 * @author Alexander Goda
 */
public class LoginView extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AuthController authController;
    private final Runnable loginSuccessAction;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel feedbackLabel;

    public LoginView(AuthController authController) {
        this(authController, null);
    }

    public LoginView(AuthController authController, Runnable loginSuccessAction) {
        if (authController == null) {
            throw new IllegalArgumentException("authController is required");
        }

        this.authController = authController;
        this.loginSuccessAction = loginSuccessAction;

        initialize();
        createEvents();
    }

    /**
     * Builds the login window and its controls.
     */
    private void initialize() {
        setTitle("Task Tracker - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 430, 330);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setLayout(null);

        JLabel titleLabel = new JLabel("Task Tracker");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        titleLabel.setBounds(90, 25, 250, 35);
        getContentPane().add(titleLabel);

        JLabel subtitleLabel = new JLabel("Sign in to manage your tasks");
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        subtitleLabel.setBounds(90, 62, 250, 20);
        getContentPane().add(subtitleLabel);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(65, 105, 95, 25);
        getContentPane().add(usernameLabel);

        usernameField = new JTextField();
        usernameField.setBounds(165, 105, 195, 28);
        getContentPane().add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(65, 145, 95, 25);
        getContentPane().add(passwordLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(165, 145, 195, 28);
        getContentPane().add(passwordField);

        feedbackLabel = new JLabel("");
        feedbackLabel.setHorizontalAlignment(SwingConstants.CENTER);
        feedbackLabel.setBounds(55, 182, 320, 25);
        getContentPane().add(feedbackLabel);

        loginButton = new JButton("Login");
        loginButton.setBounds(90, 220, 115, 32);
        getContentPane().add(loginButton);

        registerButton = new JButton("Register");
        registerButton.setBounds(225, 220, 115, 32);
        getContentPane().add(registerButton);

        getRootPane().setDefaultButton(loginButton);
    }

    /**
     * Registers the user-interface event handlers.
     */
    private void createEvents() {
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                attemptLogin();
            }
        });

        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                attemptLogin();
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                openRegistration();
            }
        });
    }

    /**
     * Sends the entered credentials to AuthController.
     */
    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            if (authController.login(username, password)) {
                feedbackLabel.setText("Login successful.");
                passwordField.setText("");

                if (loginSuccessAction != null) {
                    dispose();
                    loginSuccessAction.run();
                }
            } else {
                feedbackLabel.setText("Invalid username or password.");
                passwordField.setText("");
            }
        } catch (SQLException exception) {
            feedbackLabel.setText("Unable to complete login.");
        }
    }

    /**
     * Opens the account-registration window.
     */
    private void openRegistration() {
        RegisterView registerView = new RegisterView(authController);
        registerView.setLocationRelativeTo(this);
        registerView.setVisible(true);
    }

    /**
     * Pre-fills the username field after a successful registration.
     *
     * @param username username to display in the login field
     */
    public void setUsername(String username) {
        usernameField.setText(username == null ? "" : username);
        passwordField.requestFocusInWindow();
    }
}
