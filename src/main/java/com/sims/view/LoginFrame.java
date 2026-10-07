package com.sims.view;

import com.sims.controller.LoginController;
import com.sims.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

/**
 * Login form for SIMS — the first view displayed after the splash screen.
 *
 * <h2>Design Pattern – Behavioral: Observer (ActionListener)</h2>
 * <p>The "Login" {@link JButton} (Subject) notifies the registered
 * {@link ActionListener} (Observer) when clicked. The listener calls
 * {@link LoginController#loginAndOpenDashboard(String, String)}, keeping all
 * SQL execution off the UI component itself. This satisfies the Observer
 * pattern requirement documented in {@code ARCHITECTURE.md §4.4}.</p>
 *
 * <h2>Error Handling Protocol</h2>
 * <ul>
 *   <li>{@link IllegalArgumentException} (empty fields): red border on the
 *       offending field via
 *       {@link BorderFactory#createLineBorder(Color)}.</li>
 *   <li>{@link SecurityException} (wrong credentials): modal error dialog
 *       with message "Invalid credentials".</li>
 *   <li>{@link RuntimeException} (DB unreachable): specific dialog
 *       "Database connection failed — is Docker running?".</li>
 * </ul>
 *
 * <h2>Layout</h2>
 * <p>Uses {@link GridBagLayout} for the form panel as required — no null/
 * absolute layout anywhere in this class.</p>
 */
public class LoginFrame extends JFrame {

    // ── Controllers ──────────────────────────────────────────────────────────
    private final LoginController loginController = new LoginController();

    // ── UI Components ────────────────────────────────────────────────────────
    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JButton        loginButton;

    // ── Border constants ─────────────────────────────────────────────────────
    private static final Color NORMAL_BORDER = new Color(180, 195, 220);
    private static final Color ERROR_BORDER  = new Color(220,  50,  50);
    private static final Color FOCUS_BORDER  = new Color(60,  120, 210);

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final Color BG_TOP        = new Color(18,  30,  72);
    private static final Color BG_BOTTOM     = new Color(28,  75, 155);
    private static final Color TITLE_COLOR   = new Color(15,  30,  80);
    private static final Color SUBTITLE_COLOR= new Color(90, 110, 150);
    private static final Color BTN_BG        = new Color(30,  90, 210);
    private static final Color BTN_HOVER     = new Color(20,  70, 180);
    private static final Color LABEL_COLOR   = new Color(50,  70, 120);

    public LoginFrame() {
        super("SIMS — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        buildUi();
        pack();
        setLocationRelativeTo(null);
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private void buildUi() {
        // Outer gradient background panel
        GradientBackgroundPanel outerPanel = new GradientBackgroundPanel(BG_TOP, BG_BOTTOM);
        outerPanel.setLayout(new GridBagLayout());
        outerPanel.setPreferredSize(new Dimension(480, 560));

        // Card panel (white-ish, rounded feel via padding and border)
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(new Color(248, 250, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 215, 240), 1),
            BorderFactory.createEmptyBorder(40, 45, 40, 45)
        ));
        card.setPreferredSize(new Dimension(380, 420));

        // Header
        card.add(buildHeader(), BorderLayout.NORTH);

        // Form
        card.add(buildForm(), BorderLayout.CENTER);

        // Login button
        card.add(buildLoginButton(), BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        outerPanel.add(card, gbc);

        setContentPane(outerPanel);
    }

    /** Builds the title/subtitle header section of the card. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Welcome Back", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TITLE_COLOR);

        JLabel subtitleLabel = new JLabel(
            "Student Information Management System",
            SwingConstants.CENTER
        );
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(SUBTITLE_COLOR);

        header.add(titleLabel,    BorderLayout.CENTER);
        header.add(subtitleLabel, BorderLayout.SOUTH);
        return header;
    }

    /** Builds the GridBagLayout form with username and password fields. */
    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = new Insets(6, 0, 6, 0);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 1.0;

        // ── Username ─────────────────────────────────────────────────────────
        JLabel userLabel = makeLabel("Username");
        gbc.gridy = 0;
        form.add(userLabel, gbc);

        usernameField = new JTextField(20);
        styleField(usernameField);
        usernameField.setName("usernameField");   // unique ID for browser testing
        gbc.gridy = 1;
        form.add(usernameField, gbc);

        // ── Password ─────────────────────────────────────────────────────────
        JLabel passLabel = makeLabel("Password");
        gbc.gridy = 2;
        gbc.insets = new Insets(16, 0, 6, 0);
        form.add(passLabel, gbc);

        passwordField = new JPasswordField(20);
        styleField(passwordField);
        passwordField.setName("passwordField");   // unique ID for browser testing
        // Allow Enter key in password field to trigger login
        passwordField.addActionListener(e -> performLogin());
        gbc.gridy  = 3;
        gbc.insets = new Insets(6, 0, 6, 0);
        form.add(passwordField, gbc);

        return form;
    }

    /** Builds the styled login button. */
    private JPanel buildLoginButton() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        loginButton = new JButton("Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? BTN_HOVER : BTN_BG;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        loginButton.setName("loginButton");   // unique ID for browser testing
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setForeground(Color.WHITE);
        loginButton.setPreferredSize(new Dimension(280, 44));
        loginButton.setContentAreaFilled(false);
        loginButton.setBorderPainted(false);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setOpaque(false);

        // ── Observer Pattern: ActionListener on the Login button ─────────────
        loginButton.addActionListener(e -> performLogin());

        // Also trigger on ENTER key at form level
        loginButton.setMnemonic(KeyEvent.VK_ENTER);

        panel.add(loginButton, BorderLayout.CENTER);
        return panel;
    }

    // ── Login Action (Observer callback) ─────────────────────────────────────

    /**
     * Executes the login workflow:
     * <ol>
     *   <li>Reads username and password fields.</li>
     *   <li>Validates non-empty (red border on empty field).</li>
     *   <li>Delegates to {@link LoginController#loginAndOpenDashboard}.</li>
     *   <li>Disposes this frame on success.</li>
     *   <li>Shows appropriate error dialog on failure.</li>
     * </ol>
     */
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Reset borders before each attempt
        resetFieldBorder(usernameField);
        resetFieldBorder(passwordField);

        // Client-side empty-field validation (before hitting the DB)
        if (username.isEmpty()) {
            markFieldError(usernameField);
            usernameField.requestFocusInWindow();
            return;
        }
        if (password.isEmpty()) {
            markFieldError(passwordField);
            passwordField.requestFocusInWindow();
            return;
        }

        try {
            loginController.loginAndOpenDashboard(username, password);
            dispose();   // close login frame after successful login

        } catch (IllegalArgumentException ex) {
            // Empty field detected server-side (defensive — should be caught above)
            String msg = ex.getMessage().toLowerCase();
            if (msg.contains("username")) markFieldError(usernameField);
            else                          markFieldError(passwordField);

        } catch (SecurityException ex) {
            String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Invalid credentials. Please check your username and password.";
            JOptionPane.showMessageDialog(
                this,
                msg,
                "Login Failed",
                JOptionPane.ERROR_MESSAGE
            );

        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null && ex.getMessage().contains("Docker")
                ? "Database connection failed — is Docker running?"
                : "An unexpected error occurred: " + ex.getMessage();
            JOptionPane.showMessageDialog(
                this,
                message,
                "Connection Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ── Field Styling Helpers ─────────────────────────────────────────────────

    /** Applies consistent padding, font and default border to a text field. */
    private void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(280, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NORMAL_BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        field.setBackground(Color.WHITE);
        field.setForeground(TITLE_COLOR);

        // Focus highlight
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (!isErrorBorder(field)) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(FOCUS_BORDER, 1),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)
                    ));
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (!isErrorBorder(field)) resetFieldBorder(field);
            }
        });
    }

    /** Highlights the field border in red to indicate a validation error. */
    private void markFieldError(JTextField field) {
        field.putClientProperty("hasError", Boolean.TRUE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ERROR_BORDER, 2),
            BorderFactory.createEmptyBorder(5, 9, 5, 9)
        ));
    }

    /** Restores the default border on a field. */
    private void resetFieldBorder(JTextField field) {
        field.putClientProperty("hasError", Boolean.FALSE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NORMAL_BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    /**
     * Returns {@code true} if the field currently has a red (error) border,
     * to prevent the focus listener from overwriting it.
     */
    private boolean isErrorBorder(JTextField field) {
        // Heuristic: check if the outer border colour is the error colour
        // by re-examining the border foreground property we set.
        // Since we recreate borders each time, we track state via a client property.
        Object flag = field.getClientProperty("hasError");
        return Boolean.TRUE.equals(flag);
    }

    /** Creates a consistently styled field label. */
    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(LABEL_COLOR);
        return lbl;
    }

    // ── Inner gradient background panel ──────────────────────────────────────

    /**
     * Full-window gradient background panel — avoids null layout and supports
     * dynamic resizing via {@link #paintComponent(Graphics)}.
     */
    private static class GradientBackgroundPanel extends JPanel {

        private final Color topColor;
        private final Color bottomColor;

        GradientBackgroundPanel(Color top, Color bottom) {
            this.topColor    = top;
            this.bottomColor = bottom;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                                RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(
                0, 0,          topColor,
                0, getHeight(), bottomColor
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
