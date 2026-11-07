package tasktracker.view.components;

import tasktracker.view.theme.AdaptiveScenePalette;
import tasktracker.view.theme.ThemePalette;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.EnumMap;
import javax.swing.*;
import tasktracker.model.Priority;

/** Labeled, keyboard-accessible priority choices with redundant color and check cues. */
public final class PriorityPicker extends JPanel {
    private static final long serialVersionUID = 1L;
    private final EnumMap<Priority, JToggleButton> buttons = new EnumMap<>(Priority.class);
    private Priority selected = Priority.MEDIUM;

    public PriorityPicker() {
        super(new GridLayout(1, 3, 8, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(300, 42));
        setMinimumSize(new Dimension(270, 42));
        ButtonGroup group = new ButtonGroup();
        Priority[] values = Priority.values();
        for (int i = 0; i < values.length; i++) {
            final Priority value = values[i];
            final int index = i;
            final Color color = value == Priority.LOW ? new Color(112, 203, 149)
                    : value == Priority.MEDIUM ? new Color(243, 188, 84) : new Color(237, 126, 124);
            final String label = value == Priority.LOW ? "Low" : value == Priority.MEDIUM ? "Medium" : "High";
            JToggleButton button = new JToggleButton(label) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setColor(AdaptiveScenePalette.field());
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    if (isSelected() || getModel().isRollover()) {
                        g.setColor(
                                new Color(color.getRed(), color.getGreen(), color.getBlue(), isSelected() ? 65 : 24));
                        g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    }
                    g.setColor(isSelected() ? color : AdaptiveScenePalette.edge());
                    g.setStroke(new BasicStroke(isSelected() ? 1.5f : 1));
                    g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                    if (hasFocus()) {
                        g.setColor(ThemePalette.INK);
                        g.setStroke(new BasicStroke(1));
                        g.drawRoundRect(4, 4, getWidth() - 9, getHeight() - 9, 10, 10);
                    }
                    g.setFont(getFont());
                    FontMetrics metrics = g.getFontMetrics();
                    int contentWidth = 12 + 8 + metrics.stringWidth(label);
                    int left = (getWidth() - contentWidth) / 2;
                    int cy = getHeight() / 2 + (getModel().isPressed() && getModel().isArmed() ? 1 : 0);
                    g.setColor(color);
                    g.fillOval(left, cy - 6, 12, 12);
                    if (isSelected()) {
                        g.setColor(new Color(30, 33, 30));
                        g.setStroke(new BasicStroke(1.5f));
                        g.drawLine(left + 3, cy, left + 5, cy + 2);
                        g.drawLine(left + 5, cy + 2, left + 9, cy - 3);
                    }
                    g.setColor(ThemePalette.INK);
                    g.drawString(label, left + 20, cy + (metrics.getAscent() - metrics.getDescent()) / 2);
                    g.dispose();
                }
            };
            button.setFont(new Font("SansSerif", Font.BOLD, 13));
            button.setOpaque(false);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.getAccessibleContext().setAccessibleName(label + " priority");
            button.addActionListener(e -> {
                selected = value;
                repaint();
            });
            for (String key : new String[] { "LEFT", "RIGHT", "UP", "DOWN" }) {
                final int delta = key.equals("LEFT") || key.equals("UP") ? -1 : 1;
                button.getInputMap().put(KeyStroke.getKeyStroke(key), key);
                button.getActionMap().put(key, new AbstractAction() {
                    private static final long serialVersionUID = 1L;
                    public void actionPerformed(ActionEvent e) {
                        Priority next = values[(index + delta + values.length) % values.length];
                        setSelectedItem(next);
                        buttons.get(next).requestFocusInWindow();
                    }
                });
            }
            group.add(button);
            buttons.put(value, button);
            add(button);
        }
        setSelectedItem(Priority.MEDIUM);
    }

    public Priority getSelectedItem() {
        return selected;
    }

    public void setSelectedItem(Priority value) {
        selected = value == null ? Priority.MEDIUM : value;
        buttons.get(selected).setSelected(true);
        repaint();
    }
}
