package tasktracker.view.components;

import tasktracker.view.animation.ThemeMotion;
import tasktracker.view.presentation.DisplayDates;
import tasktracker.view.theme.AdaptiveScenePalette;
import tasktracker.view.theme.UiTheme;

import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import javax.swing.*;

/** Calendar popover hosted inside the existing window, never a dialog. */
public final class DatePickerField extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField field;
    private final JButton open = UiTheme.styledButton("", false);
    private JPanel overlay;
    private JLayeredPane layer;
    private ComponentAdapter resizeListener;
    private Timer opening;
    private float reveal = 1;
    private boolean opensAbove;

    public DatePickerField(JTextField field) {
        super(new BorderLayout(6, 0));
        this.field = field;
        setOpaque(false);
        field.getAccessibleContext().setAccessibleName("Due date, MM/DD/YYYY, optional");
        open.setPreferredSize(new Dimension(40, 38));
        open.getAccessibleContext().setAccessibleName("Open calendar");
        open.setIcon(new Icon() {
            public int getIconWidth() {
                return 22;
            }

            public int getIconHeight() {
                return 22;
            }

            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(1.6f));
                g.setColor(AdaptiveScenePalette.accent());
                g.drawRoundRect(2, 4, 17, 16, 4, 4);
                g.drawLine(2, 9, 19, 9);
                g.drawLine(6, 1, 6, 6);
                g.drawLine(15, 1, 15, 6);
                g.fillRoundRect(6, 12, 3, 3, 1, 1);
                g.dispose();
            }
        });
        add(field);
        add(open, BorderLayout.EAST);
        open.addActionListener(e -> showCalendar());
        field.getInputMap().put(KeyStroke.getKeyStroke("alt DOWN"), "calendar");
        field.getActionMap().put("calendar", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) {
                showCalendar();
            }
        });
    }

    private void showCalendar() {
        if (overlay != null) {
            dismiss();
            return;
        }
        JRootPane root = SwingUtilities.getRootPane(this);
        if (root == null)
            return;
        LocalDate selected = null;
        try {
            selected = DisplayDates.parse(field.getText());
        } catch (DateTimeParseException ignored) {
        }
        layer = root.getLayeredPane();
        overlay = new JPanel(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintChildren(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                if (getComponentCount() > 0) {
                    Rectangle r = getComponent(0).getBounds();
                    int visible = (int) Math.ceil(r.height * (.72 + .28 * reveal));
                    g.clipRect(r.x, opensAbove ? r.y + r.height - visible : r.y, r.width, visible);
                }
                g.setComposite(AlphaComposite.SrcOver.derive(.25f + .75f * reveal));
                super.paintChildren(g);
                g.dispose();
            }
        };
        overlay.setOpaque(false);
        overlay.setBounds(0, 0, layer.getWidth(), layer.getHeight());
        CalendarPanel calendar = new CalendarPanel(selected, date -> {
            field.setText(DisplayDates.format(date));
            dismiss();
        }, this::dismiss);
        Point anchor = SwingUtilities.convertPoint(this, 0, getHeight() + 6, layer);
        int w = 330, h = 350;
        int x = Math.max(8, Math.min(anchor.x, layer.getWidth() - w - 8));
        int y = anchor.y + h <= layer.getHeight() - 8 ? anchor.y : Math.max(8, anchor.y - getHeight() - h - 12);
        opensAbove = y < anchor.y;
        calendar.setBounds(x, y, w, h);
        overlay.add(calendar);
        overlay.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dismiss();
            }
        });
        overlay.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        overlay.getActionMap().put("close", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) {
                dismiss();
            }
        });
        overlay.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "choose");
        overlay.getActionMap().put("choose", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) {
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focus instanceof AbstractButton && SwingUtilities.isDescendingFrom(focus, overlay))
                    ((AbstractButton) focus).doClick();
            }
        });
        reveal = 0;
        layer.add(overlay, JLayeredPane.POPUP_LAYER);
        resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                dismiss();
            }
        };
        layer.addComponentListener(resizeListener);
        layer.revalidate();
        layer.repaint();
        opening = ThemeMotion.animate(150, t -> {
            reveal = (float) t;
            if (overlay != null)
                overlay.repaint();
        });
        calendar.setFocusable(true);
        SwingUtilities.invokeLater(() -> {
            if (overlay != null)
                calendar.requestFocusInWindow();
        });
    }

    private void dismiss() {
        if (overlay == null)
            return;
        if (opening != null) {
            opening.stop();
            opening = null;
        }
        reveal = 1;
        layer.removeComponentListener(resizeListener);
        layer.remove(overlay);
        overlay = null;
        layer.repaint();
        open.requestFocusInWindow();
    }

    @Override
    public void removeNotify() {
        dismiss();
        super.removeNotify();
    }
}
