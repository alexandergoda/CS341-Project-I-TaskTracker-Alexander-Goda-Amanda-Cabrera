package tasktracker.view.screens;

import tasktracker.view.animation.CardTransitionHost;
import tasktracker.view.animation.ThemeMotion;
import tasktracker.view.components.AppearanceControl;
import tasktracker.view.components.FluidScrollPane;
import tasktracker.view.components.PressFeedbackButton;
import tasktracker.view.presentation.TaskListOrder;
import tasktracker.view.presentation.ProjectCredit;
import tasktracker.view.theme.AdaptiveScenePalette;
import tasktracker.view.theme.CachedSceneTransition;
import tasktracker.view.theme.SceneTheme;
import tasktracker.view.theme.ThemePalette;
import tasktracker.view.theme.UiTheme;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import tasktracker.model.Task;
import tasktracker.model.User;

/** Two real Swing work panels over cached artwork. No task storage lives here. */
public final class DashboardSurface extends JPanel {
    private static final long serialVersionUID = 1L;
    static final Color INK = ThemePalette.INK, MUTED = ThemePalette.MUTED;
    public final JButton newTask = UiTheme.styledButton("+ New task", true);
    public final JButton edit = UiTheme.styledButton("Edit / assign", false);
    public final JButton complete = UiTheme.styledButton("Mark done", true);
    public final JButton delete = UiTheme.styledButton("Delete", false);
    public final JButton refresh = new PressFeedbackButton("Refresh");
    public final JButton logout = UiTheme.styledButton("Log out", false);
    public final tasktracker.view.components.AccountMenuButton account = new tasktracker.view.components.AccountMenuButton();
    public final JToggleButton completedToggle = new JToggleButton("Show completed");
    private String[] groupLabels = new String[0];

    private int groupInset(int row) {
        return row >= 0 && row < groupLabels.length && !groupLabels[row].isEmpty() ? 30 : 0;
    }

    /** Headers share a task row so model indices and controller actions stay identical. */
    public void updateTaskGroups() {
        groupLabels = new String[model.getRowCount()];
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (int row = 0; row < groupLabels.length; row++)
            counts.merge(taskGroup(row, today), 1, Integer::sum);
        String previous = "";
        for (int row = 0; row < groupLabels.length; row++) {
            String group = taskGroup(row, today);
            int count = counts.get(group);
            groupLabels[row] = group.equals(previous) ? "" : group + "  ·  " + count;
            previous = group;
            table.setRowHeight(row, 76 + groupInset(row));
        }
        table.revalidate();
        table.repaint();
    }

    private java.time.LocalDate dueDate(int row) {
        String value = String.valueOf(model.getValueAt(row, 4));
        return value.isEmpty() ? null : java.time.LocalDate.parse(value);
    }

    private String taskGroup(int row, java.time.LocalDate today) {
        return TaskListOrder.group(dueDate(row), "Complete".equals(model.getValueAt(row, 6)), today);
    }

    private boolean completionHit(int row, Point point) {
        Rectangle cell = table.getCellRect(row, 0, true);
        int inset = groupInset(row);
        cell.y += inset;
        cell.height -= inset;
        return isCompletionTarget(cell, point);
    }

    public final JLabel feedback = label("", 12), welcome = label("", 13);
    private final AppearanceControl appearance = new AppearanceControl(mode -> {
        appearanceMode = mode;
        applyAppearance();
    });
    private String appearanceMode = SceneTheme.mode;
    private final Timer appearanceTimer = new Timer(60000, e -> {
        if (appearanceMode.equals("Auto"))
            applyAppearance();
    });
    private final CachedSceneTransition cachedScene = new CachedSceneTransition(this::repaint);

    /**
     * Do not reveal the dashboard until its first background is ready.
     *
     * @return whether the cached scene is ready for this surface's current size
     */
    public boolean isSceneReady() {
        return cachedScene.isReady(getWidth(), getHeight());
    }

    public Rectangle[] entranceCards() {
        return new Rectangle[] { new Rectangle(listCard.getBounds()), new Rectangle(focusCard.getBounds()) };
    }

    private void requestScene() {
        cachedScene.request(getWidth(), getHeight(), new Rectangle[] { listCard.getBounds(), focusCard.getBounds() },
                sceneWeights);
    }

    private final Timer sceneResize = new Timer(100, e -> requestScene());

    private void applyAppearance() {
        String mode = appearanceMode;
        SceneTheme.mode = mode;
        sceneWeights = SceneTheme.current();
        requestScene();
        recolor(focusCard, INK);
        recolor(listCard, INK);
        metadata.setForeground(MUTED);
        for (JButton b : new JButton[] { edit, delete, refresh })
            b.setForeground(new Color(247, 199, 167));
        double hour = java.time.LocalTime.now().toSecondOfDay() / 3600.0;
        appearance.reflect(appearanceMode.equals("Auto") ? (hour < 6 || hour >= 19) : appearanceMode.equals("Dark"));
        table.repaint();
        repaint();
    }

    private void recolor(Container c, Color color) {
        for (Component child : c.getComponents()) {
            if (child instanceof JLabel || (child instanceof JTextArea && !((JTextArea) child).isOpaque()))
                child.setForeground(color);
            if (child instanceof Container)
                recolor((Container) child, color);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        appearanceTimer.start();
        sceneResize.restart();
    }

    @Override
    public void removeNotify() {
        appearanceTimer.stop();
        sceneResize.stop();
        cachedScene.dispose();
        if (completionMotion != null)
            completionMotion.stop();
        completionMotion = null;
        stopAnimation();
        super.removeNotify();
    }

    public final JTable table;
    public final DefaultTableModel model;
    private final JPanel focusCard = new JPanel(new BorderLayout(0, 18));
    private final JPanel focusContent = new JPanel(new BorderLayout(0, 18));
    private final CardTransitionHost focusHost = new CardTransitionHost();

    public void setWorkspace(JComponent component) {
        recolor(component, ThemePalette.text(true));
        focusHost.showContent(component, true, true);
    }

    public void restoreFocus() {
        focusHost.showContent(focusContent, true, false);
    }

    private final JPanel listCard = new JPanel(new BorderLayout(0, 16));
    private final JTextArea title = area(30, "Georgia", Font.BOLD), metadata = area(18, "SansSerif", Font.PLAIN),
            notes = area(19, "SansSerif", Font.PLAIN);
    private float opacity = 1f;
    private final JPanel detail = new FlowDetail() {
        private static final long serialVersionUID = 1L;

        @Override
        public void paint(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));
            super.paint(g);
            g.dispose();
        }
    };
    private Timer fade;
    private double[] sceneWeights = SceneTheme.current();
    private java.util.function.IntConsumer completionRequested = row -> {
    };
    private Timer completionMotion;
    private int completingRow = -1, completionRowHeight;
    private boolean completionTarget;
    private double completionProgress;

    public void onCompletionRequested(java.util.function.IntConsumer action) {
        completionRequested = action;
    }

    /**
     * Shared hit target includes padding around the painted 20px checkbox.
     *
     * @param cell  task-cell bounds in table coordinates
     * @param point pointer location in table coordinates
     * @return whether the point lies within the padded completion target
     */
    public static boolean isCompletionTarget(Rectangle cell, Point point) {
        return new Rectangle(cell.x + 10, cell.y + cell.height / 2 - 20, 40, 40).contains(point);
    }

    public void animateCompletion(int row, boolean completed, Runnable finished) {
        if (completionMotion != null)
            return;
        if (row < 0 || row >= table.getRowCount()) {
            finished.run();
            return;
        }
        completingRow = row;
        completionTarget = completed;
        completionProgress = 0;
        completionRowHeight = table.getRowHeight(row);
        JButton[] controls = { newTask, edit, complete, delete, refresh, logout, account };
        boolean[] enabled = new boolean[controls.length];
        for (int i = 0; i < controls.length; i++) {
            enabled[i] = controls[i].isEnabled();
            controls[i].setEnabled(false);
        }
        table.setEnabled(false);
        completedToggle.setEnabled(false);
        completionMotion = ThemeMotion.animate(580, t -> {
            completionProgress = t;
            // Hold the checkmark briefly, then close the gap as neighbouring rows rise.
            double collapse = Math.max(0, (t - .3) / .7);
            table.setRowHeight(row, Math.max(1, (int) Math.round(completionRowHeight * (1 - collapse))));
            table.repaint();
            if (t >= 1) {
                completionMotion = null;
                table.setRowHeight(row, completionRowHeight);
                completingRow = -1;
                table.setEnabled(true);
                completedToggle.setEnabled(true);
                for (int i = 0; i < controls.length; i++)
                    controls[i].setEnabled(enabled[i]);
                finished.run();
            }
        });
    }

    public DashboardSurface(String username) {
        sceneResize.setRepeats(false);
        setLayout(null);
        setBackground(new Color(41, 32, 43));
        welcome.setText("Signed in as " + username);
        for (JComponent component : new JComponent[] { newTask, logout, account, welcome, feedback, appearance,
                focusCard, listCard })
            add(component);
        SceneTheme.setRenderWeights(sceneWeights);
        focusCard.setOpaque(false);
        listCard.setOpaque(false);
        JLabel focusHeading = label("Your focus", 30);
        focusHeading.setFont(new Font("Georgia", 0, 30));
        focusContent.setOpaque(false);
        focusCard.add(focusHost, BorderLayout.CENTER);
        focusContent.add(focusHeading, BorderLayout.NORTH);
        detail.setOpaque(false);
        detail.setLayout(null);
        metadata.setForeground(MUTED);
        for (JTextArea text : new JTextArea[] { title, metadata, notes }) {
            text.setAlignmentX(0);
            detail.add(text);
        }
        focusContent.add(scroll(detail), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 3, 4, 0));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(100, 44));
        actions.add(complete);
        actions.add(edit);
        actions.add(delete);
        focusContent.add(actions, BorderLayout.SOUTH);
        focusHost.showContent(focusContent, false, true);
        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setOpaque(false);
        listHeader.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        JLabel all = label("All tasks", 30);
        all.setFont(new Font("Georgia", 0, 30));
        JPanel listHeading = new JPanel(new BorderLayout(0, 4));
        listHeading.setOpaque(false);
        listHeading.add(all, BorderLayout.NORTH);
        listHeading.add(feedback, BorderLayout.SOUTH);
        listHeader.add(listHeading, BorderLayout.CENTER);
        listHeader.add(refresh, BorderLayout.EAST);
        listCard.add(listHeader, BorderLayout.NORTH);
        model = new DefaultTableModel(
                new String[] { "ID", "Title", "Creator", "Assignee", "Due Date", "Priority", "Status" }, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (!table.isEnabled() || event.getButton() != java.awt.event.MouseEvent.BUTTON1
                        || event.getClickCount() != 1)
                    return;
                int row = table.rowAtPoint(event.getPoint());
                if (row >= 0 && completionHit(row, event.getPoint()))
                    completionRequested.accept(row);
            }
        });
        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                boolean target = row >= 0 && completionHit(row, event.getPoint());
                table.setCursor(Cursor.getPredefinedCursor(target ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "toggleTaskCompletion");
        table.getActionMap().put("toggleTaskCompletion", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (table.isEnabled() && table.getSelectedRow() >= 0)
                    completionRequested.accept(table.getSelectedRow());
            }
        });
        table.getAccessibleContext().setAccessibleDescription(
                "Select a task and press Space to complete or reopen it. Completed tasks appear after unfinished tasks.");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setOpaque(false);
        table.setForeground(INK);
        table.setBackground(new Color(42, 32, 42));
        table.setBorder(null);
        table.setTableHeader(null);
        table.setFillsViewportHeight(false);
        table.getAccessibleContext().setAccessibleName("Tasks created by or assigned to you");
        // Retain the existing model and action mapping, but present a readable two-line list.
        for (int i = table.getColumnCount() - 1; i >= 0; i--)
            if (i != 1)
                table.removeColumn(table.getColumnModel().getColumn(i));
        table.getColumnModel().getColumn(0).setCellRenderer(new TaskRenderer());
        listCard.add(scroll(table), BorderLayout.CENTER);
        completedToggle.setOpaque(false);
        completedToggle.setContentAreaFilled(false);
        completedToggle.setBorder(BorderFactory.createEmptyBorder(4, 12, 0, 12));
        completedToggle.setForeground(MUTED);
        completedToggle.setFocusPainted(false);
        completedToggle.setVisible(false);
        listCard.add(completedToggle, BorderLayout.SOUTH);
        showTask(null, null, false);
        welcome.setVisible(false);
        table.setRowHeight(66);
        focusCard.setBorder(BorderFactory.createEmptyBorder(25, 34, 32, 24));
        listCard.setBorder(BorderFactory.createEmptyBorder(25, 18, 24, 18));
        applyAppearance();
    }

    private static JLabel label(String value, int size) {
        JLabel label = new JLabel(value);
        label.putClientProperty("html.disable", true);
        label.setForeground(INK);
        label.setFont(new Font("SansSerif", 0, size));
        return label;
    }

    private static JTextArea area(int size, String family, int style) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setForeground(INK);
        area.setFont(new Font(family, style, size));
        area.setBorder(null);
        return area;
    }

    private static JScrollPane scroll(Component view) {
        return new FluidScrollPane(view);
    }

    public void showTask(Task task, User user, boolean animate) {
        stopAnimation();
        if (task == null) {
            boolean hiddenCompleted = model.getRowCount() == 0 && completedToggle.isVisible()
                    && !completedToggle.isSelected();
            title.setText(hiddenCompleted ? "All caught up." : "A little room to focus.");
            metadata.setText(hiddenCompleted ? "Your completed tasks are hidden."
                    : model.getRowCount() > 0 ? "Select a task from your list." : "No tasks yet.");
            notes.setText(hiddenCompleted ? "Choose Show completed below the task list to view or reopen them."
                    : model.getRowCount() > 0 ? "Your task details will appear here."
                            : "Create a task to get started.");
        } else {
            title.setText(task.getTitle());
            notes.setText(task.getDescription().isEmpty() ? "No notes added yet." : task.getDescription());
            String due = "No due date";
            if (task.getDueDate() != null) {
                java.time.LocalDate today = java.time.LocalDate.now();
                due = task.getDueDate().equals(today) ? "Due today"
                        : task.getDueDate().equals(today.plusDays(1)) ? "Due tomorrow"
                                : "Due " + task.getDueDate()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"));
            }
            String priority = task.getPriority().name().toLowerCase(java.util.Locale.ROOT);
            priority = Character.toUpperCase(priority.charAt(0)) + priority.substring(1);
            metadata.setText(due + "\n\nAssigned to "
                    + (task.getAssignee() == null ? "Unassigned" : task.getAssignee().getUsername()) + "\nCreated by "
                    + task.getCreator().getUsername() + " · Priority: " + priority);
        }
        title.setCaretPosition(0);
        metadata.setCaretPosition(0);
        notes.setCaretPosition(0);
        boolean creator = task != null && user != null && task.getCreator().getUserId() == user.getUserId();
        boolean assignee = task != null && user != null && task.getAssignee() != null
                && task.getAssignee().getUserId() == user.getUserId();
        edit.setEnabled(creator);
        delete.setEnabled(creator);
        complete.setEnabled(creator || assignee);
        complete.setText(task != null && task.isCompleted() ? "Reopen" : "Mark done");
        if (animate && task != null) {
            fade = ThemeMotion.animate(180, t -> {
                opacity = (float) (.35 + .65 * t);
                detail.repaint();
            });
        }
        detail.revalidate();
        detail.repaint();
    }

    public void stopAnimation() {
        if (fade != null)
            fade.stop();
        opacity = 1;
        detail.repaint();
    }

    @Override
    public void doLayout() {
        int w = getWidth(), h = getHeight(), margin = Math.max(28, (int) (w * .038)),
                gap = Math.max(20, (int) (w * .022));
        int available = w - 2 * margin - gap;
        int left = (int) (available * .435), right = available - left;
        int top = Math.max(100, (int) (h * .145)), cardHeight = Math.max(300, (int) (h * .64));
        listCard.setBounds(margin, top, left, cardHeight);
        focusCard.setBounds(margin + left + gap, top, right, cardHeight);
        int titleEnd = 40 + getFontMetrics(new Font("Georgia", Font.PLAIN, 28)).stringWidth("Task Tracker");
        newTask.setBounds(titleEnd + 28, 10, 140, 36);
        logout.setBounds(w - 165, 10, 110, 36);
        account.setBounds(w - 48, 12, 28, 32);
        appearance.setBounds(w - 390, 12, 215, 32);
        sceneResize.restart();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        cachedScene.background(g, getWidth(), getHeight());
        g.setColor(INK);
        g.setFont(new Font("Georgia", 0, 28));
        g.drawString("Task Tracker", 40, 40);
        g.dispose();
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        super.paintChildren(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        cachedScene.foreground(g, getWidth(), getHeight());
        ProjectCredit.paint(g, getWidth(), getHeight());
        g.dispose();
    }

    /** Natural text height and viewport-width wrapping, with no stretched rows. */
    private class FlowDetail extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (notes.getY() > 0) {
                graphics.setColor(new Color(255, 232, 216, 65));
                graphics.drawLine(0, notes.getY() - 12, getWidth(), notes.getY() - 12);
            }
        }

        @Override
        public void doLayout() {
            int y = 0, w = Math.max(1, getWidth());
            for (Component c : getComponents()) {
                c.setSize(w, Short.MAX_VALUE);
                int h = c.getPreferredSize().height;
                c.setBounds(0, y, w, h);
                y += h + 28;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getParent() == null ? 350 : Math.max(1, getParent().getWidth()), h = 0;
            for (Component c : getComponents()) {
                c.setSize(w, Short.MAX_VALUE);
                h += c.getPreferredSize().height + 28;
            }
            return new Dimension(w, h);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(350, 300);
        }

        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 18;
        }

        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return r.height - 18;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private final class TaskRenderer extends JPanel implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private int headerInset;
        private String groupTitle = "", dateLabel = "", priorityLabel = "";
        private Color priorityColor;
        private final JLabel heading = label("", 17);
        private boolean selectedRow, focusedRow, completedRow, animatingRow;

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (headerInset > 0) {
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                g.setColor(MUTED);
                g.drawString(groupTitle, 14, 20);
            }
            g.translate(0, headerInset);
            int contentHeight = getHeight() - headerInset;
            if (selectedRow || focusedRow) {
                g.setColor(AdaptiveScenePalette.selection());
                g.fillRoundRect(0, 4, getWidth() - 3, contentHeight - 8, 18, 18);
                g.setColor(AdaptiveScenePalette.accent());
                g.drawRoundRect(1, 4, getWidth() - 5, contentHeight - 9, 18, 18);
            } else {
                g.setColor(new Color(255, 240, 220, 25));
                g.drawLine(14, contentHeight - 1, getWidth() - 17, contentHeight - 1);
            }
            g.setColor(completedRow ? new Color(151, 218, 176) : INK);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(20, contentHeight / 2 - 10, 20, 20, 5, 5);
            if (completedRow) {
                int cy = contentHeight / 2;
                g.drawLine(24, cy, 29, cy + 5);
                g.drawLine(29, cy + 5, 37, cy - 5);
            }
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            int activeBars = priorityLabel.equals("High") ? 3 : priorityLabel.equals("Medium") ? 2 : 1;
            for (int bar = 0; bar < 3; bar++) {
                g.setColor(bar < activeBars ? priorityColor : new Color(255, 240, 220, 35));
                g.fillRoundRect(66 + bar * 15, 50, 11, 4, 4, 4);
            }
            g.setColor(MUTED);
            g.drawString(dateLabel, getWidth() - 20 - g.getFontMetrics().stringWidth(dateLabel), 56);
            g.dispose();
        }

        @Override
        public void paint(Graphics graphics) {
            doLayout();
            Graphics2D g = (Graphics2D) graphics.create();
            if (animatingRow) {
                double exit = Math.max(0, (completionProgress - .3) / .7);
                g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, 1 - exit)));
                g.translate(12 * exit, 0);
            }
            super.paint(g);
            g.dispose();
        }

        TaskRenderer() {
            setOpaque(false);
            setLayout(new BorderLayout(0, 7));
            add(heading, BorderLayout.NORTH);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focused,
                int row, int column) {
            selectedRow = selected;
            focusedRow = focused;
            completedRow = String.valueOf(model.getValueAt(row, 6)).toLowerCase().startsWith("complete");
            animatingRow = row == completingRow;
            if (animatingRow)
                completedRow = completionTarget;
            headerInset = groupInset(row);
            groupTitle = headerInset > 0 ? groupLabels[row] : "";
            priorityLabel = String.valueOf(model.getValueAt(row, 5)).toLowerCase(java.util.Locale.ROOT);
            priorityLabel = Character.toUpperCase(priorityLabel.charAt(0)) + priorityLabel.substring(1);
            priorityColor = priorityLabel.equals("High") ? new Color(235, 145, 141)
                    : priorityLabel.equals("Medium") ? new Color(225, 188, 119) : new Color(148, 190, 162);
            dateLabel = TaskListOrder.date(dueDate(row), java.time.LocalDate.now());
            if (!completedRow && dueDate(row) != null && dueDate(row).isBefore(java.time.LocalDate.now()))
                dateLabel += " · overdue";
            heading.setText(String.valueOf(value));
            heading.setForeground(completedRow ? MUTED : INK);
            setBorder(BorderFactory.createEmptyBorder(headerInset + 14, 66, 34, 18));
            getAccessibleContext().setAccessibleName(
                    value + ", " + priorityLabel + " priority, " + dateLabel + ", " + model.getValueAt(row, 6));
            return this;
        }
    }
}
