package com.sims.factory;

import com.sims.model.User;
import com.sims.model.UserRole;
import com.sims.view.LoginFrame;
import com.sims.view.admin.AdminDashboard;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Factory Method for role-specific dashboard creation.
 *
 * <h2>Design Pattern – Creational: Factory Method</h2>
 * <p><b>Academic Justification</b>: The SRS mandates explicit Role-Based
 * Access Control (RBAC) for Admin, Faculty, Student, and Parent users. Without
 * a factory, the post-login code would contain deeply nested {@code if-else}
 * or {@code switch} blocks tightly coupling authentication to UI instantiation.
 * The Factory Method pattern encapsulates the conditional creation logic in
 * {@link #createDashboard(User)}, allowing new roles to be added by extending
 * this factory rather than modifying existing login logic (Open/Closed
 * Principle).</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Inside LoginController, after authentication succeeds:
 * JFrame dashboard = DashboardFactory.createDashboard(authenticatedUser);
 * dashboard.setVisible(true);
 * }</pre>
 *
 * <p>Phase 1 will replace the stub {@code JFrame} returns with fully
 * implemented {@code AdminDashboard}, {@code FacultyDashboard},
 * {@code StudentDashboard}, and {@code ParentDashboard} views.</p>
 */
public final class DashboardFactory {

    // Utility class — no instantiation
    private DashboardFactory() {}

    /**
     * Creates and returns the role-appropriate dashboard frame for the
     * authenticated user.
     *
     * @param user the authenticated {@link User} — role must not be null
     * @return a configured, un-shown {@link JFrame} dashboard
     * @throws IllegalArgumentException if the role is unrecognised
     */
    public static JFrame createDashboard(User user) {
        UserRole role = user.getRole();
        return switch (role) {
            case ADMIN   -> createAdminDashboard(user);
            case FACULTY -> createFacultyDashboard(user);
            case STUDENT -> createStudentDashboard(user);
            case PARENT  -> createParentDashboard(user);
        };
    }

    // ── Private factory methods ──────────────────────────────────────────────

    private static JFrame createAdminDashboard(User user) {
        return new AdminDashboard(user);   // Phase 2: full implementation
    }

    private static JFrame createFacultyDashboard(User user) {
        // TODO (Phase 3): return new FacultyDashboard(user);
        return buildStubFrame("Faculty Dashboard", user);
    }

    private static JFrame createStudentDashboard(User user) {
        // TODO (Phase 4): return new StudentDashboard(user);
        return buildStubFrame("Student Dashboard", user);
    }

    private static JFrame createParentDashboard(User user) {
        // TODO (Phase 5): return new ParentDashboard(user);
        return buildStubFrame("Parent Dashboard", user);
    }

    /** Temporary scaffold frame used until real dashboards are implemented. */
    private static JFrame buildStubFrame(String title, User user) {
        JFrame frame = new JFrame(title + " — " + user.getFullName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 650);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(
            "<html><center><h2>" + title + "</h2>" +
            "<p>Welcome, " + user.getFullName() + "</p>" +
            "<p><i>Implementation pending.</i></p></center></html>",
            SwingConstants.CENTER
        );
        panel.add(label, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });
        bottomBar.add(logoutBtn);
        panel.add(bottomBar, BorderLayout.SOUTH);

        frame.setContentPane(panel);
        return frame;
    }
}
