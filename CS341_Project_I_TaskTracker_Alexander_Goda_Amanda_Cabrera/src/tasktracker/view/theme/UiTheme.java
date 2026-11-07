package tasktracker.view.theme;

import tasktracker.view.components.PressFeedbackButton;
import java.awt.Font;
import javax.swing.*;

/** Shared native Swing controls. No application or database dependencies. */
public final class UiTheme {
    private UiTheme() {
    }

    public static void styleField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        field.setBackground(new java.awt.Color(43, 35, 45));
        field.setForeground(ThemePalette.INK);
        field.setCaretColor(ThemePalette.INK);
        field.setSelectionColor(new java.awt.Color(120, 80, 77));
        field.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(155, 132, 134)),
                javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    public static JButton styledButton(String text, final boolean primary) {
        return new PressFeedbackButton(text, primary);
    }
}
