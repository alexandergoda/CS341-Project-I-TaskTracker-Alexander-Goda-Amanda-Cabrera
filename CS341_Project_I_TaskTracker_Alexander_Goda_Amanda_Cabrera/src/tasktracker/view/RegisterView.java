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
import tasktracker.model.User;

/**
 * Registration screen for creating a new Task Tracker account.
 *
 * @author Alexander Goda
 */
public class RegisterView extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AuthController authController;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton registerButton;
    private JButton cancelButton;
    private JLabel feedbackLabel;

    /**
     * Creates the window without showing it. Successful registration leaves the window open
     * and does not log the user in.
     * Construct and use on the Swing event thread.
     *
     * @param authController required authentication controller
     * @throws IllegalArgumentException if a required controller is null
     */
    public RegisterView(AuthController authController) {
        if (authController == null) {
            throw new IllegalArgumentException("authController is required");
        }

        this.authController = authController;

        initialize();
        createEvents();
    }

    /**
     * Builds the registration window and its controls.
     */
    private void initialize() {
        setTitle("Task Tracker - Register");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 450, 390);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setLayout(null);

        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        titleLabel.setBounds(95, 25, 260, 35);
        getContentPane().add(titleLabel);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(60, 95, 125, 25);
        getContentPane().add(usernameLabel);

        usernameField = new JTextField();
        usernameField.setBounds(195, 95, 185, 28);
        getContentPane().add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(60, 140, 125, 25);
        getContentPane().add(passwordLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(195, 140, 185, 28);
        getContentPane().add(passwordField);

        JLabel confirmLabel = new JLabel("Confirm password:");
        confirmLabel.setBounds(60, 185, 130, 25);
        getContentPane().add(confirmLabel);

        confirmPasswordField = new JPasswordField();
        confirmPasswordField.setBounds(195, 185, 185, 28);
        getContentPane().add(confirmPasswordField);

        feedbackLabel = new JLabel("");
        feedbackLabel.setHorizontalAlignment(SwingConstants.CENTER);
        feedbackLabel.setBounds(45, 230, 360, 25);
        getContentPane().add(feedbackLabel);

        registerButton = new JButton("Create Account");
        registerButton.setBounds(80, 280, 140, 32);
        getContentPane().add(registerButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setBounds(240, 280, 120, 32);
        getContentPane().add(cancelButton);

        getRootPane().setDefaultButton(registerButton);
    }

    /**
     * Registers the user-interface event handlers.
     */
    private void createEvents() {
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                attemptRegistration();
            }
        });

        confirmPasswordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                attemptRegistration();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
    }

    /**
     * Validates the form and sends the account request to AuthController.
     */
    private void attemptRegistration() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (!password.equals(confirmPassword)) {
            feedbackLabel.setText("Passwords do not match.");
            passwordField.setText("");
            confirmPasswordField.setText("");
            return;
        }

        try {
            User createdUser = authController.register(username, password);
            feedbackLabel.setText("Account created for " + createdUser.getUsername() + ".");
            registerButton.setEnabled(false);
            usernameField.setEnabled(false);
            passwordField.setEnabled(false);
            confirmPasswordField.setEnabled(false);
        } catch (IllegalArgumentException exception) {
            feedbackLabel.setText(exception.getMessage());
        } catch (SQLException exception) {
            feedbackLabel.setText("Unable to create account.");
        }
    }
}
