package tasktracker.view.screens;

import tasktracker.view.components.DatePickerField;
import tasktracker.view.components.FluidScrollPane;
import tasktracker.view.components.PriorityPicker;
import tasktracker.view.components.SceneComboBox;
import tasktracker.view.components.SceneFields;
import tasktracker.view.presentation.DisplayDates;
import tasktracker.view.theme.ThemePalette;
import tasktracker.view.theme.UiTheme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import tasktracker.model.*;

/** Compact task form that replaces the focus card's contents. */
public final class InlineEditorSurface extends JPanel {
    private static final long serialVersionUID = 1L;
    public final JTextField title = SceneFields.text(), due = SceneFields.text();
    public final JTextArea notes = SceneFields.notes();
    public final JComboBox<Object> assignee = new SceneComboBox<>();
    public final PriorityPicker priority = new PriorityPicker();
    public final JButton save = UiTheme.styledButton("Save task", true), cancel = UiTheme.styledButton("Cancel", false);
    public final JButton saveLeave = UiTheme.styledButton("Save and continue", true),
            discard = UiTheme.styledButton("Discard changes", false),
            keep = UiTheme.styledButton("Keep editing", false);
    public final JLabel feedback = new JLabel(" ");
    private final JPanel warning = new JPanel(new GridBagLayout());
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final JPanel editingPage = new JPanel(new BorderLayout(0, 8));
    private final JPanel form = new JPanel(new GridBagLayout());
    private List<Object> baseline;

    public InlineEditorSurface(Task task, List<User> users) {
        super(new BorderLayout(0, 8));
        setOpaque(false);
        form.setOpaque(false);
        JLabel heading = new JLabel(task == null ? "New task" : "Edit task");
        heading.setFont(new Font("Georgia", Font.PLAIN, 28));
        heading.setForeground(ThemePalette.INK);
        JPanel north = new JPanel(new BorderLayout(0, 5));
        north.setOpaque(false);
        north.add(heading, BorderLayout.NORTH);
        add(north, BorderLayout.NORTH);
        warning.setOpaque(false);
        pages.setOpaque(false);
        editingPage.setOpaque(false);
        JPanel choices = new JPanel(new GridLayout(0, 1, 0, 12));
        choices.setOpaque(false);
        JLabel message = new JLabel("You have unsaved changes.");
        message.setForeground(ThemePalette.INK);
        JLabel explanation = new JLabel("Save them, discard them, or keep editing.");
        explanation.setForeground(ThemePalette.INK);
        choices.add(message);
        choices.add(explanation);
        choices.add(saveLeave);
        choices.add(discard);
        choices.add(keep);
        GridBagConstraints choiceBounds = new GridBagConstraints();
        choiceBounds.weightx = 1;
        choiceBounds.fill = GridBagConstraints.HORIZONTAL;
        choiceBounds.insets = new Insets(16, 16, 16, 16);
        warning.add(choices, choiceBounds);
        pages.add(editingPage, "edit");
        pages.add(warning, "confirm");
        add(pages, BorderLayout.CENTER);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        assignee.addItem("Unassigned");
        for (User u : users)
            assignee.addItem(u);
        priority.setSelectedItem(Priority.MEDIUM);
        JScrollPane notesScroll = new JScrollPane(notes);
        notesScroll.setBorder(SceneFields.edge());
        notesScroll.setOpaque(false);
        notesScroll.getViewport().setOpaque(false);
        addRow("Title", title, 0);
        addRow("Notes", notesScroll, 1);
        addRow("Assign to", assignee, 2);
        addRow("Due date", new DatePickerField(due), 3);
        addRow("Priority", priority, 4);
        editingPage.add(new FluidScrollPane(form), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 5));
        south.setOpaque(false);
        feedback.setForeground(new Color(255, 190, 175));
        south.add(feedback, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(cancel);
        buttons.setPreferredSize(new Dimension(200, 38));
        south.add(buttons, BorderLayout.SOUTH);
        editingPage.add(south, BorderLayout.SOUTH);
        if (task != null) {
            title.setText(task.getTitle());
            notes.setText(task.getDescription());
            due.setText(DisplayDates.format(task.getDueDate()));
            priority.setSelectedItem(task.getPriority());
            if (task.getAssignee() != null)
                assignee.setSelectedItem(task.getAssignee());
        }
        baseline = state();
    }

    private void addRow(String text, JComponent field, int row) {
        JLabel label = new JLabel(text);
        label.setForeground(ThemePalette.INK);
        label.setLabelFor(field);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 10, 10);
        form.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 0);
        field.setMinimumSize(new Dimension(100, 32));
        form.add(field, c);
    }

    private List<Object> state() {
        User selected = assignedUser();
        return java.util.Arrays.asList(title.getText(), notes.getText(), due.getText(),
                selected == null ? null : selected.getUserId(), priority.getSelectedItem());
    }

    public boolean dirty() {
        return !baseline.equals(state());
    }

    public void accept() {
        baseline = state();
    }

    /**
     * Confirmation replaces controls without copying or destroying the draft.
     *
     * @param visible whether to show the unsaved-draft confirmation
     */
    public void askToLeave(boolean visible) {
        pageLayout.show(pages, visible ? "confirm" : "edit");
        revalidate();
        repaint();
        if (visible)
            keep.requestFocusInWindow();
        else
            title.requestFocusInWindow();
    }

    public User assignedUser() {
        Object x = assignee.getSelectedItem();
        return x instanceof User ? (User) x : null;
    }
}
