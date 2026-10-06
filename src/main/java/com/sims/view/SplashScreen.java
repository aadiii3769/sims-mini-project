package com.sims.view;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Splash screen displayed for ~2 seconds on application startup.
 *
 * <h2>Design Pattern – Behavioral: Observer (ActionListener)</h2>
 * <p>A {@link javax.swing.Timer} fires after {@code DISPLAY_MS} milliseconds and
 * notifies the registered {@code ActionListener} (Observer), which dismisses this
 * window and triggers {@code LoginFrame} creation — all on the EDT.</p>
 *
 * <h2>Implementation Rules</h2>
 * <ul>
 *   <li>Uses {@link JWindow} (no title bar, no taskbar entry) as specified.</li>
 *   <li>Uses {@link javax.swing.Timer} — never {@code Thread.sleep()} — to
 *       remain non-blocking on the EDT.</li>
 *   <li>Layout: {@link BorderLayout} (no null / absolute layout).</li>
 * </ul>
 */
public class SplashScreen extends JWindow {

    /** Duration (ms) the splash is shown before LoginFrame opens. */
    private static final int DISPLAY_MS = 2000;

    /**
     * Creates and immediately shows the splash screen, then schedules its
     * dismissal and the display of the {@link LoginFrame} via a Swing Timer.
     *
     * <p>Must be called on the Event Dispatch Thread.</p>
     */
    public SplashScreen() {
        buildUi();
        pack();
        setLocationRelativeTo(null);   // centre on screen
        setVisible(true);

        // ── Swing Timer: fires once after DISPLAY_MS, dismisses splash ────
        Timer timer = new Timer(DISPLAY_MS, e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        timer.setRepeats(false);
        timer.start();
    }

    // ── UI Construction ────────────────────────────────────────────────────

    private void buildUi() {
        setPreferredSize(new Dimension(520, 300));

        GradientPanel content = new GradientPanel();
        content.setLayout(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        // Institution label (top)
        JLabel institutionLabel = new JLabel("University Institute of Technology", SwingConstants.CENTER);
        institutionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        institutionLabel.setForeground(new Color(200, 220, 255));

        // Project name (centre)
        JLabel titleLabel = new JLabel(
            "<html><center>Student Information<br>Management System</center></html>",
            SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);

        // Sub-labels panel (version + loading)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        JLabel versionLabel = new JLabel("UIT3361 · UIT3311 — v1.0", SwingConstants.CENTER);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(160, 190, 230));

        JLabel loadingLabel = new JLabel("Loading…", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        loadingLabel.setForeground(new Color(140, 170, 210));

        bottomPanel.add(versionLabel, BorderLayout.CENTER);
        bottomPanel.add(loadingLabel, BorderLayout.SOUTH);

        content.add(institutionLabel, BorderLayout.NORTH);
        content.add(titleLabel,       BorderLayout.CENTER);
        content.add(bottomPanel,      BorderLayout.SOUTH);

        setContentPane(content);
    }

    // ── Inner panel with gradient background ──────────────────────────────

    /**
     * Panel that renders a vertical gradient from deep navy to medium blue,
     * used as the splash background.
     */
    private static class GradientPanel extends JPanel {

        private static final Color TOP_COLOR    = new Color(15,  25,  60);
        private static final Color BOTTOM_COLOR = new Color(30,  80, 160);

        GradientPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(
                0, 0,          TOP_COLOR,
                0, getHeight(), BOTTOM_COLOR
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
