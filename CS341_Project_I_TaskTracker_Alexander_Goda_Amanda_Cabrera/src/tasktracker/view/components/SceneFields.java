package tasktracker.view.components;

import tasktracker.view.theme.AdaptiveScenePalette;
import tasktracker.view.theme.ThemePalette;
import tasktracker.view.theme.UiTheme;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/** Standard editable Swing fields with live scene tints and consistent focus edges. */
public final class SceneFields {
    private SceneFields() {
    }

    public static JTextField text() {
        JTextField field = new JTextField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                setBackground(AdaptiveScenePalette.field());
                setSelectionColor(AdaptiveScenePalette.accent());
                setSelectedTextColor(AdaptiveScenePalette.buttonText());
                super.paintComponent(g);
            }
        };
        UiTheme.styleField(field);
        field.setBorder(edge());
        return field;
    }

    public static JTextArea notes() {
        JTextArea field = new JTextArea(3, 20) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                setBackground(AdaptiveScenePalette.field());
                super.paintComponent(g);
            }
        };
        field.setForeground(ThemePalette.INK);
        field.setCaretColor(ThemePalette.INK);
        field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return field;
    }

    public static JPasswordField password() {
        JPasswordField field = new JPasswordField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                setBackground(AdaptiveScenePalette.field());
                setSelectionColor(AdaptiveScenePalette.accent());
                setSelectedTextColor(AdaptiveScenePalette.buttonText());
                super.paintComponent(g);
            }
        };
        UiTheme.styleField(field);
        field.setBorder(edge());
        return field;
    }

    public static javax.swing.border.Border edge() {
        return new AbstractBorder() {
            private static final long serialVersionUID = 1L;

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(9, 12, 9, 12);
            }

            @Override
            public Insets getBorderInsets(Component c, Insets in) {
                in.set(9, 12, 9, 12);
                return in;
            }

            @Override
            public void paintBorder(Component c, Graphics graphics, int x, int y, int w, int h) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = c.isFocusOwner()
                        || (c instanceof JScrollPane && ((JScrollPane) c).getViewport().getView().isFocusOwner());
                g.setColor(AdaptiveScenePalette.fieldEdge(focused));
                g.drawRoundRect(x, y, w - 1, h - 1, 8, 8);
                g.dispose();
            }
        };
    }
}
