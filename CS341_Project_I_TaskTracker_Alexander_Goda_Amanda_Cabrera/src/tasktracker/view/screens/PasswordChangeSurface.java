package tasktracker.view.screens;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import javax.swing.*;
import tasktracker.view.components.SceneFields;
import tasktracker.view.theme.UiTheme;
import tasktracker.view.theme.ThemePalette;

/** Signed-in password form. AuthController validates changes and delegates persistence to UserDAO. */
public final class PasswordChangeSurface extends JPanel {
    private static final long serialVersionUID = 1L;
    public final JPasswordField current = SceneFields.password();
    public final JPasswordField replacement = SceneFields.password();
    public final JPasswordField confirmation = SceneFields.password();
    public final JButton save = UiTheme.styledButton("Change password", true);
    public final JButton cancel = UiTheme.styledButton("Cancel", false);
    public final JLabel feedback = new JLabel("Your current password is required.");

    public PasswordChangeSurface() {
        super(new BorderLayout(0, 18));
        setOpaque(false);
        JLabel heading = new JLabel("Change password");
        heading.setFont(new Font("Georgia", Font.PLAIN, 28));
        heading.setForeground(ThemePalette.INK);
        add(heading, BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 8));
        fields.setOpaque(false);
        addField(fields, "Current password", current);
        addField(fields, "New password", replacement);
        addField(fields, "Confirm new password", confirmation);
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(fields, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        feedback.setForeground(ThemePalette.INK);
        footer.add(feedback, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(cancel);
        footer.add(buttons, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
    }

    private void addField(JPanel panel, String text, JPasswordField field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        label.setForeground(ThemePalette.INK);
        panel.add(label);
        panel.add(field);
    }

    public void clearPasswords() {
        for (JPasswordField field : new JPasswordField[] { current, replacement, confirmation }) {
            char[] value = field.getPassword();
            Arrays.fill(value, '\0');
            field.setText("");
        }
    }

    @Override
    public void removeNotify() {
        clearPasswords();
        super.removeNotify();
    }
}
