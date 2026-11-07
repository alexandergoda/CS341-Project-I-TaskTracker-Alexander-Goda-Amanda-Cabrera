package tasktracker.view.components;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import tasktracker.view.theme.AdaptiveScenePalette;
import tasktracker.view.theme.ThemePalette;

/** Compact account menu using the scene picker's reversible popover motion. */
public final class AccountMenuButton extends JButton {
    private static final long serialVersionUID = 1L;
    private final JPanel menu = new JPanel(new GridLayout(0, 1));
    private final JButton changePassword = new JButton("Change password") {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover() || hasFocus() || getModel().isPressed()) {
                g.setColor(AdaptiveScenePalette.selection());
                g.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 12, 12);
            }
            g.setFont(getFont());
            g.setColor(ThemePalette.INK);
            g.drawString(getText(), 12,
                    (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
            g.dispose();
        }
    };
    private final SceneMenuPopover popover = new SceneMenuPopover(this, menu);

    public AccountMenuButton() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName("Account options");
        menu.setBackground(new Color(32, 34, 44));
        menu.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(110, 105, 113)),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        changePassword.setPreferredSize(new Dimension(220, 44));
        changePassword.setFont(new Font("SansSerif", Font.PLAIN, 14));
        changePassword.setForeground(ThemePalette.INK);
        changePassword.setBackground(new Color(32, 34, 44));
        changePassword.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        changePassword.setFocusPainted(false);
        changePassword.setOpaque(false);
        changePassword.setContentAreaFilled(false);
        changePassword.setBorderPainted(false);
        changePassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menu.add(changePassword);
        addActionListener(e -> popover.toggle());
    }

    /**
     * Selection, rather than opening the menu, requests the account screen.
     *
     * @param listener listener to notify when Change password is chosen
     */
    public void onChangePassword(ActionListener listener) {
        changePassword.addActionListener(e -> {
            popover.close();
            listener.actionPerformed(e);
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (getModel().isRollover() || hasFocus() || popover.isOpen()) {
            g.setColor(AdaptiveScenePalette.selection());
            g.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 4, 12, 12);
        }
        g.setColor(ThemePalette.INK);
        g.setStroke(new BasicStroke(1.5f));
        int x = getWidth() / 2, y = getHeight() / 2;
        g.rotate(Math.PI * popover.progress(), x, y);
        g.drawLine(x - 4, y - 2, x, y + 2);
        g.drawLine(x, y + 2, x + 4, y - 2);
        g.dispose();
    }

    @Override
    public void removeNotify() {
        popover.dispose();
        super.removeNotify();
    }
}
