package com.sims.controller;

import com.sims.dao.UserDAO;
import com.sims.factory.DashboardFactory;
import com.sims.model.User;

import java.sql.SQLException;
import java.util.Optional;

/**
 * LoginController — business logic for user authentication.
 *
 * <h2>Design Pattern – Behavioral: Observer (ActionListener)</h2>
 * <p>The corresponding {@code LoginFrame} registers an {@link java.awt.event.ActionListener}
 * on the "Login" button (the Subject). When clicked, it notifies this controller
 * (the Observer) by calling {@link #authenticate(String, String)}. This
 * decouples the view's event-generation from the authentication logic.</p>
 *
 * <h2>Transaction Policy</h2>
 * <p>Authentication is a read-only operation — no commit/rollback is needed.
 * Write operations (registration) require the caller to commit after all
 * related inserts succeed.</p>
 *
 * <h2>Phase 0 Status</h2>
 * <p>This is a foundation stub. Phase 1 wires it to {@code LoginFrame}.</p>
 */
public class LoginController {

    private final UserDAO userDAO;

    public LoginController() {
        this.userDAO = new UserDAO();
    }

    /**
     * Authenticates a user by username and plain-text password.
     *
     * <p>Phase 0: performs a plain-text string comparison against the stored
     * {@code PASSWORD_HASH} (which is plain-text in seed data). Phase 1 will
     * replace this comparison with a BCrypt {@code checkpw()} call.</p>
     *
     * @param username  the entered username
     * @param password  the entered password (plain-text from Swing field)
     * @return the authenticated {@link User} if credentials are valid
     * @throws IllegalArgumentException if username or password is blank
     * @throws SecurityException        if credentials are invalid
     * @throws RuntimeException         wrapping any {@link SQLException}
     */
    public User authenticate(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        try {
            Optional<User> userOpt = userDAO.findByUsername(username.trim());
            if (userOpt.isEmpty()) {
                throw new SecurityException("Invalid username or password.");
            }

            User user = userOpt.get();

            // Phase 0: plain-text comparison.
            // Phase 1: BCrypt.checkpw(password, user.getPasswordHash())
            boolean passwordMatch = password.equals(user.getPasswordHash());
            if (!passwordMatch) {
                throw new SecurityException("Invalid username or password.");
            }

            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during authentication: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience: authenticate and immediately open the role dashboard.
     * Called from the LoginFrame's ActionListener.
     *
     * @param username the entered username
     * @param password the entered password
     */
    public void loginAndOpenDashboard(String username, String password) {
        User user = authenticate(username, password);    // throws on failure
        javax.swing.SwingUtilities.invokeLater(() -> {
            DashboardFactory.createDashboard(user).setVisible(true);
        });
    }
}
