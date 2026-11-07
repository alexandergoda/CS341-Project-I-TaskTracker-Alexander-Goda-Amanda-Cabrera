package tasktracker.view.screens;

import tasktracker.view.animation.ThemeMotion;
import tasktracker.view.components.SceneFields;
import tasktracker.view.theme.SunsetLoginPanel;
import tasktracker.view.theme.UiTheme;
import java.awt.Font;
import javax.swing.*;

/** Authentication layout and card morph; account verification stays in the controller. */
public final class AuthSurface {
    private static final java.awt.Color ERROR_COLOR = new java.awt.Color(255, 194, 188);
    public JTextField usernameField;
    public JPasswordField passwordField, confirmPasswordField;
    public JButton loginButton, registerButton;
    public JLabel feedbackLabel;
    private JLabel titleLabel, subtitleLabel;
    public SunsetLoginPanel scenePanel;
    private JPanel confirmPanel;
    private float confirmOpacity;
    private int confirmReveal;
    private Timer transitionTimer;

    public AuthSurface() {
        javax.swing.JPanel form = new javax.swing.JPanel(null);
        form.setOpaque(false);
        scenePanel = new SunsetLoginPanel(form);

        titleLabel = new JLabel("Welcome back.");
        titleLabel.setForeground(new java.awt.Color(255, 245, 233));
        titleLabel.setFont(new Font("Georgia", Font.PLAIN, 38));
        titleLabel.setBounds(0, 20, 365, 52);
        form.add(titleLabel);

        subtitleLabel = new JLabel("A calmer place for everything on your list.");
        subtitleLabel.setForeground(new java.awt.Color(232, 215, 204));
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitleLabel.setBounds(0, 76, 365, 24);
        form.add(subtitleLabel);

        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setForeground(new java.awt.Color(255, 245, 233));
        usernameLabel.setBounds(0, 125, 150, 22);
        form.add(usernameLabel);

        usernameField = SceneFields.text();
        usernameField.setBounds(0, 153, 365, 44);
        usernameLabel.setLabelFor(usernameField);
        form.add(usernameField);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setForeground(new java.awt.Color(255, 245, 233));
        passwordLabel.setBounds(0, 211, 150, 22);
        form.add(passwordLabel);

        passwordField = SceneFields.password();
        passwordField.setBounds(0, 239, 365, 44);
        passwordLabel.setLabelFor(passwordField);
        form.add(passwordField);

        confirmPanel = new javax.swing.JPanel(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(java.awt.Graphics graphics) {
                java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
                g.clipRect(0, 0, getWidth(), confirmReveal);
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive(confirmOpacity));
                super.paint(g);
                g.dispose();
            }
        };
        confirmPanel.setOpaque(false);
        confirmPanel.setBounds(0, 297, 365, 72);
        JLabel confirmLabel = new JLabel("Confirm password");
        confirmLabel.setForeground(new java.awt.Color(255, 245, 233));
        confirmLabel.setBounds(0, 0, 200, 22);
        confirmPasswordField = SceneFields.password();
        confirmPasswordField.setBounds(0, 28, 365, 44);
        confirmLabel.setLabelFor(confirmPasswordField);
        confirmPanel.add(confirmLabel);
        confirmPanel.add(confirmPasswordField);
        confirmPanel.setVisible(false);
        confirmPasswordField.setEnabled(false);
        form.add(confirmPanel);

        feedbackLabel = new JLabel("");
        feedbackLabel.setForeground(ERROR_COLOR);
        feedbackLabel.setBounds(0, 289, 365, 24);
        form.add(feedbackLabel);

        loginButton = UiTheme.styledButton("Sign in", true);
        loginButton.setBounds(0, 321, 365, 46);
        form.add(loginButton);

        registerButton = UiTheme.styledButton("New here?  Create an account", false);
        registerButton.setBounds(0, 376, 365, 30);
        form.add(registerButton);

    }

    /**
     * Display authentication feedback using the error color after any prior success.
     *
     * @param message feedback text to display
     */
    public void showError(String message) {
        feedbackLabel.setForeground(ERROR_COLOR);
        feedbackLabel.setText(message);
    }

    public void transition(boolean from, boolean target, boolean accountCreated, Runnable done) {
        stop();
        passwordField.setText("");
        confirmPasswordField.setText("");
        showError("");
        usernameField.setEditable(false);
        passwordField.setEditable(false);
        confirmPasswordField.setEnabled(true);
        confirmPasswordField.setEditable(false);
        confirmPanel.setVisible(true);
        // One continuous layout motion. Do not fade text out, switch it at the
        // midpoint, then fade it back in: that reads as two separate transitions.
        titleLabel.setText(target ? "Create account." : "Welcome back.");
        subtitleLabel.setText(
                target ? "Sign up to start organizing your tasks." : "A calmer place for everything on your list.");
        loginButton.setText(target ? "Create account" : "Sign in");
        registerButton.setText(target ? "Already have an account?  Sign in" : "New here?  Create an account");
        transitionTimer = ThemeMotion.animate(360, t -> {
            double eased = t;
            double amount = from ? 1 - eased : eased;
            int shift = (int) Math.round(86 * amount);
            scenePanel.setCardHeight(470 + shift);
            feedbackLabel.setLocation(0, 289 + shift);
            loginButton.setLocation(0, 321 + shift);
            registerButton.setLocation(0, 376 + shift);
            confirmOpacity = (float) amount;
            // Reveal only the space already opened above the moving actions.
            // Otherwise the confirmation field briefly paints across the button.
            confirmReveal = Math.max(0, Math.min(72, shift - 8));
            scenePanel.repaint();
            if (t >= 1) {
                transitionTimer = null;
                done.run();
                confirmPanel.setVisible(target);
                confirmPasswordField.setEnabled(target);
                usernameField.setEditable(true);
                passwordField.setEditable(true);
                confirmPasswordField.setEditable(target);
                if (accountCreated) {
                    feedbackLabel.setForeground(new java.awt.Color(190, 235, 205));
                    feedbackLabel.setText("Account created. Sign in to continue.");
                    passwordField.requestFocusInWindow();
                } else
                    usernameField.requestFocusInWindow();
            }
        });
    }

    public void stop() {
        if (transitionTimer != null)
            transitionTimer.stop();
    }
}
