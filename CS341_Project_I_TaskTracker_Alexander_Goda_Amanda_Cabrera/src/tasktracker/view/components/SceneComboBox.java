package tasktracker.view.components;

import tasktracker.view.animation.ThemeMotion;
import tasktracker.view.theme.AdaptiveScenePalette;
import tasktracker.view.theme.ThemePalette;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

/** Scene-tinted selection control retaining Swing's selection and keyboard behavior. */
public final class SceneComboBox<E> extends JComboBox<E> {
    private static final long serialVersionUID = 1L;

    public SceneComboBox() {
        setFont(new Font("SansSerif", Font.PLAIN, 16));
        setForeground(ThemePalette.INK);
        setBorder(new javax.swing.border.AbstractBorder() {
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
                Color edge = AdaptiveScenePalette.edge();
                g.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), hasFocus() ? 95 : 45));
                g.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
                g.dispose();
            }
        });
        setMaximumRowCount(7);
        setRenderer(new ChoiceRenderer());
        setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D g = (Graphics2D) graphics.create();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int x = getWidth() / 2, y = getHeight() / 2;
                        g.setColor(isEnabled() ? ThemePalette.INK : ThemePalette.MUTED);
                        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int direction = SceneComboBox.this.isPopupVisible() ? -1 : 1;
                        g.drawLine(x - 4, y - direction * 2, x, y + direction * 2);
                        g.drawLine(x, y + direction * 2, x + 4, y - direction * 2);
                        g.dispose();
                    }
                };
                arrow.setOpaque(false);
                arrow.setContentAreaFilled(false);
                arrow.setBorderPainted(false);
                arrow.setFocusable(false);
                arrow.setPreferredSize(new Dimension(28, 24));
                return arrow;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean focus) {
                g.setColor(AdaptiveScenePalette.field());
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected ComboPopup createPopup() {
                return new BasicComboPopup(comboBox) {
                    private static final long serialVersionUID = 1L;
                    private JPanel overlay;
                    private JLayeredPane layer;
                    private Timer opening;
                    private float reveal = 1;
                    private java.awt.event.ComponentAdapter resizeListener;
                    {
                        // Aqua's popup delegate paints a light bezel independently
                        // of list colors. Use a plain delegate and paint our surface.
                        setUI(new BasicPopupMenuUI());
                        setOpaque(true);
                        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                        list.setFixedCellHeight(40);
                        list.setBackground(AdaptiveScenePalette.field());
                        scroller.setUI(new BasicScrollPaneUI());
                        scroller.setBorder(BorderFactory.createEmptyBorder());
                        scroller.setViewportBorder(null);
                        JScrollBar bar = scroller.getVerticalScrollBar();
                        bar.setPreferredSize(new Dimension(6, 0));
                        bar.setUI(new BasicScrollBarUI() {
                            @Override
                            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                                g.setColor(AdaptiveScenePalette.field());
                                g.fillRect(r.x, r.y, r.width, r.height);
                            }

                            @Override
                            protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
                                Graphics2D g = (Graphics2D) graphics.create();
                                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g.setColor(new Color(220, 203, 190, 100));
                                g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
                                g.dispose();
                            }

                            @Override
                            protected JButton createDecreaseButton(int orientation) {
                                return blank();
                            }

                            @Override
                            protected JButton createIncreaseButton(int orientation) {
                                return blank();
                            }

                            private JButton blank() {
                                JButton b = new JButton();
                                b.setPreferredSize(new Dimension(0, 0));
                                return b;
                            }
                        });
                    }

                    @Override
                    protected int getPopupHeightForRowCount(int count) {
                        return Math.min(count, comboBox.getItemCount()) * 40;
                    }

                    @Override
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D g = (Graphics2D) graphics.create();
                        g.setColor(AdaptiveScenePalette.field());
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        Color edge = AdaptiveScenePalette.edge();
                        g.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 40));
                        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                        g.dispose();
                    }

                    @Override
                    public boolean isVisible() {
                        return overlay != null;
                    }

                    @Override
                    public void show() {
                        if (overlay != null)
                            return;
                        JRootPane root = SwingUtilities.getRootPane(comboBox);
                        if (root == null || comboBox.getItemCount() == 0)
                            return;
                        Color tint = AdaptiveScenePalette.field();
                        setBackground(tint);
                        list.setBackground(tint);
                        list.setForeground(ThemePalette.INK);
                        list.setFixedCellHeight(40);
                        scroller.setBorder(BorderFactory.createEmptyBorder());
                        scroller.setBackground(tint);
                        scroller.getViewport().setBackground(tint);
                        scroller.getVerticalScrollBar().setBackground(tint);
                        // Host the actual Swing list inside our existing window.
                        // No native PopupFactory surface is created or resized.
                        layer = root.getLayeredPane();
                        Point anchor = SwingUtilities.convertPoint(comboBox, 0, 0, layer);
                        int width = Math.min(comboBox.getWidth(), layer.getWidth() - 16);
                        int wanted = getPopupHeightForRowCount(comboBox.getMaximumRowCount()) + 12;
                        int below = layer.getHeight() - anchor.y - comboBox.getHeight() - 14;
                        int above = anchor.y - 14;
                        boolean upwards = below < wanted && above > below;
                        int height = Math.min(wanted, Math.max(52, upwards ? above : below));
                        int x = Math.max(8, Math.min(anchor.x, layer.getWidth() - width - 8));
                        int y = upwards ? anchor.y - height - 6 : anchor.y + comboBox.getHeight() + 6;
                        final BasicComboPopup panel = this;
                        overlay = new JPanel(null) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void paintChildren(Graphics graphics) {
                                Graphics2D g = (Graphics2D) graphics.create();
                                Rectangle r = panel.getBounds();
                                int visible = (int) Math.ceil(r.height * (.72 + .28 * reveal));
                                g.clipRect(r.x, upwards ? r.y + r.height - visible : r.y, r.width, visible);
                                g.setComposite(AlphaComposite.SrcOver.derive(.25f + .75f * reveal));
                                super.paintChildren(g);
                                g.dispose();
                            }
                        };
                        overlay.setOpaque(false);
                        overlay.setBounds(0, 0, layer.getWidth(), layer.getHeight());
                        setBounds(x, y, width, height);
                        scroller.setPreferredSize(new Dimension(width - 12, height - 12));
                        overlay.add(this);
                        overlay.addMouseListener(new java.awt.event.MouseAdapter() {
                            @Override
                            public void mousePressed(java.awt.event.MouseEvent event) {
                                hide();
                            }
                        });
                        resizeListener = new java.awt.event.ComponentAdapter() {
                            @Override
                            public void componentResized(java.awt.event.ComponentEvent event) {
                                hide();
                            }
                        };
                        layer.addComponentListener(resizeListener);
                        reveal = 0;
                        layer.add(overlay, JLayeredPane.POPUP_LAYER);
                        doLayout();
                        scroller.doLayout();
                        scroller.getViewport().doLayout();
                        list.setSelectedIndex(comboBox.getSelectedIndex());
                        if (wanted <= height)
                            scroller.getViewport().setViewPosition(new Point(0, 0));
                        else
                            list.ensureIndexIsVisible(comboBox.getSelectedIndex());
                        comboBox.firePopupMenuWillBecomeVisible();
                        comboBox.requestFocusInWindow();
                        layer.revalidate();
                        layer.repaint();
                        opening = ThemeMotion.animate(150, t -> {
                            reveal = (float) t;
                            if (overlay != null)
                                overlay.repaint();
                        });
                        comboBox.repaint();
                    }

                    @Override
                    public void hide() {
                        if (overlay == null)
                            return;
                        if (opening != null) {
                            opening.stop();
                            opening = null;
                        }
                        reveal = 1;
                        layer.removeComponentListener(resizeListener);
                        JPanel old = overlay;
                        overlay = null;
                        old.remove(this);
                        layer.remove(old);
                        layer.revalidate();
                        layer.repaint();
                        comboBox.firePopupMenuWillBecomeInvisible();
                        comboBox.repaint();
                    }

                    @Override
                    public void uninstallingUI() {
                        hide();
                        super.uninstallingUI();
                    }
                };
            }
        });
    }

    @Override
    public void removeNotify() {
        setPopupVisible(false);
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        setBackground(AdaptiveScenePalette.field());
        super.paintComponent(g);
    }

    private final class ChoiceRenderer extends JLabel implements ListCellRenderer<E> {
        private static final long serialVersionUID = 1L;
        private boolean highlighted, popupRow, chosen;

        ChoiceRenderer() {
            setOpaque(false);
            putClientProperty("html.disable", true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean selected,
                boolean focused) {
            popupRow = index >= 0;
            highlighted = selected && popupRow;
            chosen = popupRow && java.util.Objects.equals(value, getSelectedItem());
            setText(value == null ? "" : value.toString());
            setFont(SceneComboBox.this.getFont());
            setForeground(ThemePalette.INK);
            setBorder(BorderFactory.createEmptyBorder(6, popupRow ? 12 : 2, 6, popupRow ? 32 : 2));
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(AdaptiveScenePalette.field());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (highlighted) {
                g.setColor(AdaptiveScenePalette.selection());
                g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
            }
            if (chosen) {
                int x = getWidth() - 20, y = getHeight() / 2;
                g.setColor(AdaptiveScenePalette.accent());
                g.setStroke(new BasicStroke(1.6f));
                g.drawLine(x - 4, y, x - 1, y + 3);
                g.drawLine(x - 1, y + 3, x + 5, y - 4);
            }
            super.paintComponent(g);
            g.dispose();
        }
    }
}
