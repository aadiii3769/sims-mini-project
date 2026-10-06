package com.sims.controller;

import com.sims.dao.UserDAO;
import com.sims.factory.DashboardFactory;
import com.sims.model.User;
import org.mindrot.jbcrypt.BCrypt;

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
 * <h2>Security – BCrypt (Phase 1)</h2>
 * <p>Passwords are never stored or compared as plain text.
 * {@link BCrypt#checkpw(String, String)} performs a constant-time comparison
 * between the entered password and the stored BCrypt hash, preventing
 * timing-based side-channel attacks.</p>
 */
public class LoginController {

    private final UserDAO userDAO;

    public LoginController() {
        this.userDAO = new UserDAO();
    }

    /**
     * Authenticates a user by username and plain-text password.
     *
     * <p>Phase 1: uses {@link BCrypt#checkpw(String, String)} to securely
     * validate the entered password against the stored BCrypt hash.</p>
     *
     * @param username  the entered username (trimmed before lookup)
     * @param password  the entered password (plain-text from Swing field)
     * @return the authenticated {@link User} if credentials are valid
     * @throws IllegalArgumentException if username or password is blank
     * @throws SecurityException        if credentials are invalid
     * @throws RuntimeException         wrapping any {@link SQLException} or DB unreachable
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

            // Phase 1: BCrypt constant-time comparison.
            boolean passwordMatch = BCrypt.checkpw(password, user.getPasswordHash());
            if (!passwordMatch) {
                throw new SecurityException("Invalid username or password.");
            }

            return user;

        } catch (SQLException e) {
            throw new RuntimeException(
                "Database connection failed — is Docker running? " + e.getMessage(), e);
        }
    }

    /**
     * Convenience: authenticate and immediately open the role-specific dashboard.
     * Called from {@code LoginFrame}'s ActionListener on the EDT.
     *
     * <p>The dashboard is opened via {@link javax.swing.SwingUtilities#invokeLater}
     * to keep all Swing mutations on the Event Dispatch Thread.</p>
     *
     * @param username the entered username
     * @param password the entered password
     * @return the authenticated user (allows the caller to dispose the login frame)
     * @throws IllegalArgumentException if a field is blank
     * @throws SecurityException        if credentials are wrong
     * @throws RuntimeException         if the DB is unreachable
     */
    public User loginAndOpenDashboard(String username, String password) {
        User user = authenticate(username, password);    // throws on failure
        javax.swing.SwingUtilities.invokeLater(() ->
            DashboardFactory.createDashboard(user).setVisible(true)
        );
        return user;
    }
}
