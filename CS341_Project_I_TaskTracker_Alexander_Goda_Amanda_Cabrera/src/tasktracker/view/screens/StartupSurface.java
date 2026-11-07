package tasktracker.view.screens;

import java.awt.*;
import javax.swing.JPanel;
import tasktracker.view.presentation.ProjectCredit;

/** Lightweight first frame, with no image loading or database access. */
public final class StartupSurface extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final Color BACKGROUND = new Color(25, 34, 48);

    public StartupSurface() {
        setOpaque(true);
        setBackground(BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Keep the same solid background as the window's initial frame.
        g.setColor(new Color(255, 239, 223));
        g.setFont(new Font("Georgia", Font.PLAIN, 32));
        centered(g, "Task Tracker", getHeight() / 2 - 12);
        g.setColor(new Color(205, 196, 194));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        centered(g, "Preparing your workspace...", getHeight() / 2 + 24);
        ProjectCredit.paint(g, getWidth(), getHeight());
        g.dispose();
    }

    private void centered(Graphics2D g, String text, int y) {
        g.drawString(text, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2, y);
    }
}
