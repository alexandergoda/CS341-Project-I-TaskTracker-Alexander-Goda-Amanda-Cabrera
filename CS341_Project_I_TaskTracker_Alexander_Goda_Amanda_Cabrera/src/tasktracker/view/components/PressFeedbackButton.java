package tasktracker.view.components;

import tasktracker.view.animation.ThemeMotion;
import tasktracker.view.theme.AdaptiveScenePalette;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicButtonUI;

/** Shared primary and text actions with gentle press feedback and keyboard focus. */
public final class PressFeedbackButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Timer animation;
    private Timer keyboardClick;
    private Timer releaseWait;
    private double pressAmount;
    private boolean pressed;
    private boolean keyboardFocus;
    private final boolean primary;

    public PressFeedbackButton(String text) {
        this(text, false);
    }

    public PressFeedbackButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setUI(new BasicButtonUI());
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setFont(new Font("SansSerif", primary ? Font.BOLD : Font.PLAIN, primary ? 16 : 13));
        setForeground(primary ? new Color(41, 27, 24) : new Color(247, 199, 167));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getModel().addChangeListener(event -> updatePress());
    }

    private void updatePress() {
        boolean next = isEnabled() && getModel().isPressed() && getModel().isArmed();
        if (pressed == next)
            return;
        pressed = next;
        if (animation != null)
            animation.stop();
        double from = pressAmount;
        double to = pressed ? 1 : 0;
        animation = ThemeMotion.animate(pressed ? 65 : 170, progress -> {
            pressAmount = from + (to - from) * progress;
            repaint();
        });
    }

    /** Keyboard activation without JButton.doClick's blocking press delay. */
    public void clickWithFeedback() {
        if (!isEnabled() || keyboardClick != null || releaseWait != null || getModel().isPressed())
            return;
        getModel().setArmed(true);
        getModel().setPressed(true);
        keyboardClick = new Timer(90, event -> {
            keyboardClick.stop();
            keyboardClick = null;
            // Releasing an armed model dispatches the normal action exactly once.
            getModel().setPressed(false);
            getModel().setArmed(false);
        });
        keyboardClick.setRepeats(false);
        keyboardClick.start();
    }

    /**
     * Let the release settle before an action replaces the button's screen.
     *
     * @param action action to run after the press feedback releases
     */
    public void afterRelease(Runnable action) {
        if (releaseWait != null)
            return;
        // Very quick mouse clicks can finish before the first animation tick.
        pressAmount = Math.max(.65, pressAmount);
        pressed = true;
        updatePress();
        releaseWait = new Timer(16, event -> {
            if (getModel().isPressed() || pressAmount > .001)
                return;
            releaseWait.stop();
            releaseWait = null;
            action.run();
        });
        releaseWait.start();
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_PRESSED)
            keyboardFocus = false;
        super.processMouseEvent(event);
    }

    @Override
    protected void processFocusEvent(FocusEvent event) {
        FocusEvent.Cause cause = event.getCause();
        // macOS can report mouse/programmatic focus as UNKNOWN. Only explicit
        // keyboard traversal should display the keyboard navigation cue.
        keyboardFocus = event.getID() == FocusEvent.FOCUS_GAINED
                && (cause == FocusEvent.Cause.TRAVERSAL_FORWARD || cause == FocusEvent.Cause.TRAVERSAL_BACKWARD
                        || cause == FocusEvent.Cause.TRAVERSAL_UP || cause == FocusEvent.Cause.TRAVERSAL_DOWN);
        super.processFocusEvent(event);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Color textTint = primary ? AdaptiveScenePalette.buttonText() : AdaptiveScenePalette.secondaryText();
        setForeground(
                new Color(textTint.getRed(), textTint.getGreen(), textTint.getBlue(), getForeground().getAlpha()));
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = 1 - 0.035 * pressAmount;
        g.translate(getWidth() / 2.0, getHeight() / 2.0 + pressAmount);
        g.scale(scale, scale);
        g.translate(-getWidth() / 2.0, -getHeight() / 2.0);
        if (primary) {
            Color base = AdaptiveScenePalette.accent();
            g.setColor(base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.setColor(new Color(255, 245, 230,
                    (int) (32 * pressAmount) + (isEnabled() && getModel().isRollover() ? 15 : 0)));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        } else {
            int alpha = (int) ((isEnabled() && getModel().isRollover() ? 18 : 0) + 35 * pressAmount);
            Color tint = getForeground();
            g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha));
            g.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 14, 14);
        }
        if (hasFocus() && keyboardFocus) {
            // Focus can return here when an editor closes. Use a whole-surface
            // tint, never an inset outline or underline that resembles a border.
            Color tint = primary ? Color.WHITE : getForeground();
            g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 28));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        }
        super.paintComponent(g);
        g.dispose();
    }

    @Override
    public void removeNotify() {
        if (releaseWait != null)
            releaseWait.stop();
        releaseWait = null;
        if (keyboardClick != null)
            keyboardClick.stop();
        keyboardClick = null;
        getModel().setArmed(false);
        getModel().setPressed(false);
        if (animation != null)
            animation.stop();
        pressAmount = 0;
        pressed = false;
        super.removeNotify();
    }
}
