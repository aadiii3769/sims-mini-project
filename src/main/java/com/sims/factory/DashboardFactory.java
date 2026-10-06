package com.sims.factory;

import com.sims.model.User;
import com.sims.model.UserRole;
import com.sims.view.admin.AdminDashboard;
import com.sims.view.faculty.FacultyDashboard;
import com.sims.view.parent.ParentDashboard;
import com.sims.view.student.StudentDashboard;

import javax.swing.JFrame;

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
 * <p>Phase implementations replace placeholder frames with full dashboards
 * while keeping login routing unchanged.</p>
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
        return new AdminDashboard(user);
    }

    private static JFrame createFacultyDashboard(User user) {
        return new FacultyDashboard(user);
    }

    private static JFrame createStudentDashboard(User user) {
        return new StudentDashboard(user);
    }

    private static JFrame createParentDashboard(User user) {
        return new ParentDashboard(user);
    }
}
