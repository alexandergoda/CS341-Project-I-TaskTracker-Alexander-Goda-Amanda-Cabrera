package tasktracker.view.components;

import tasktracker.view.theme.AdaptiveScenePalette;
import tasktracker.view.theme.SceneTheme;
import tasktracker.view.theme.ThemePalette;
import tasktracker.view.theme.UiTheme;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;

/** Dependency-free month calendar. Selecting a date never saves the task. */
public final class CalendarPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private YearMonth month;
    private final LocalDate selected;
    private final Consumer<LocalDate> choose;
    private final JPanel days = new JPanel(new GridLayout(0, 7, 3, 3));
    private final JLabel heading = new JLabel("", SwingConstants.CENTER);
    private final Color ink = ThemePalette.text(SceneTheme.usesDarkGlass());
    private final JButton previousMonth;
    private final JButton nextMonth;

    public CalendarPanel(LocalDate selected, Consumer<LocalDate> choose, Runnable close) {
        super(new BorderLayout(0, 12));
        this.selected = selected;
        this.choose = choose;
        month = YearMonth.from(selected == null ? LocalDate.now() : selected);
        setBackground(AdaptiveScenePalette.field());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AdaptiveScenePalette.fieldEdge(false)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        setPreferredSize(new Dimension(330, 350));
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        previousMonth = button("‹");
        nextMonth = button("›");
        previousMonth.getAccessibleContext().setAccessibleName("Previous month");
        nextMonth.getAccessibleContext().setAccessibleName("Next month");
        previousMonth.addActionListener(e -> changeMonth(-1));
        nextMonth.addActionListener(e -> changeMonth(1));
        heading.setForeground(ink);
        heading.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.add(previousMonth, BorderLayout.WEST);
        header.add(heading);
        header.add(nextMonth, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        days.setOpaque(false);
        add(days, BorderLayout.CENTER);
        JPanel footer = new JPanel(new GridLayout(1, 3, 6, 0));
        footer.setOpaque(false);
        JButton today = button("Today"), clear = button("Clear"), done = button("Close");
        today.addActionListener(e -> choose.accept(LocalDate.now()));
        clear.addActionListener(e -> choose.accept(null));
        done.addActionListener(e -> close.run());
        footer.add(today);
        footer.add(clear);
        footer.add(done);
        add(footer, BorderLayout.SOUTH);
        render();
    }

    private JButton button(String text) {
        JButton b = UiTheme.styledButton(text, false);
        b.setForeground(ink);
        return b;
    }

    public void changeMonth(int delta) {
        try {
            month = month.plusMonths(delta);
        } catch (DateTimeException limit) {
            return;
        }
        render();
    }

    private void render() {
        heading.setText(month.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)));
        previousMonth.setEnabled(!month.equals(YearMonth.from(LocalDate.MIN)));
        nextMonth.setEnabled(!month.equals(YearMonth.from(LocalDate.MAX)));
        days.removeAll();
        for (String day : new String[] { "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su" }) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setForeground(ink);
            label.setFont(new Font("SansSerif", Font.PLAIN, 11));
            days.add(label);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        long start = month.atDay(1).toEpochDay() - offset;
        for (int i = 0; i < 42; i++) {
            long epochDay = start + i;
            // A boundary month's grid can extend beyond LocalDate's supported range.
            if (epochDay < LocalDate.MIN.toEpochDay() || epochDay > LocalDate.MAX.toEpochDay()) {
                days.add(new JLabel(""));
                continue;
            }
            final LocalDate date = LocalDate.ofEpochDay(epochDay);
            final boolean picked = date.equals(selected), today = date.equals(LocalDate.now());
            JButton day = new JButton(String.valueOf(date.getDayOfMonth())) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (picked || getModel().isRollover()) {
                        g.setColor(picked ? AdaptiveScenePalette.accent() : AdaptiveScenePalette.selection());
                        g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
                    }
                    if (today || hasFocus()) {
                        g.setColor(AdaptiveScenePalette.accent());
                        g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                    }
                    // Paint the short date label ourselves: Aqua button insets can otherwise
                    // replace even two digits with an ellipsis in these compact cells.
                    g.setFont(getFont());
                    g.setColor(getForeground());
                    FontMetrics metrics = g.getFontMetrics();
                    String number = String.valueOf(date.getDayOfMonth());
                    g.drawString(number, (getWidth() - metrics.stringWidth(number)) / 2,
                            (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                    g.dispose();
                }
            };
            day.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            day.setBorderPainted(false);
            day.setContentAreaFilled(false);
            day.setFocusPainted(false);
            day.setMargin(new Insets(0, 0, 0, 0));
            day.setBorder(BorderFactory.createEmptyBorder());
            day.setFont(new Font("SansSerif", Font.PLAIN, 13));
            day.setForeground(picked ? new Color(45, 30, 25)
                    : date.getMonth() == month.getMonth() ? ink : new Color(151, 132, 125));
            day.getAccessibleContext().setAccessibleName(date.toString());
            day.addActionListener(e -> choose.accept(date));
            days.add(day);
        }
        revalidate();
        repaint();
    }
}
