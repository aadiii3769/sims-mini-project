package com.sims.controller;

import com.sims.dao.StudentDAO;
import com.sims.dao.UserDAO;
import com.sims.model.Student;
import com.sims.model.User;
import com.sims.model.UserRole;
import com.sims.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic and transaction controller for student admission and profile
 * management.
 *
 * <h2>Design Pattern – Behavioral: Observer (indirect)</h2>
 * <p>
 * <b>Academic Justification</b>: This controller is called by View-layer
 * ActionListeners (Observer pattern) when the user triggers UI events such as
 * "Save", "Search", or "Deactivate". It encapsulates all business rules —
 * BCrypt hashing, input sanitisation, and transactional SQL — so Views remain
 * thin and contain zero persistence logic.
 * </p>
 *
 * <h2>Transaction Demarcation</h2>
 * <p>
 * All DML operations that span multiple DAO calls use explicit
 * {@code conn.commit()} on success and {@code conn.rollback()} on failure,
 * per the AGENTS.md mandate (Section 4.3). DAOs never commit on their own.
 * </p>
 */
public class StudentController {

    private final StudentDAO studentDAO;
    private final UserDAO userDAO;
    private final Connection conn;

    public StudentController() {
        this.conn = DBConnection.getInstance().getConnection();
        this.studentDAO = new StudentDAO();
        this.userDAO = new UserDAO();
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Registers a new student: inserts a USERS row then a STUDENT row atomically.
     * The {@code newUser.getPasswordHash()} must be the <em>plain-text</em>
     * password supplied by the form; this method BCrypt-hashes it before
     * inserting.
     *
     * @param newUser    fully populated User (role must be STUDENT; passwordHash is
     *                   plain-text)
     * @param newStudent fully populated Student (userId field is ignored; set from
     *                   insert)
     * @throws RuntimeException wrapping SQLException on any failure; transaction is
     *                          rolled back
     */
    public void registerStudent(User newUser, Student newStudent) {
        // Hash the plain-text password supplied from the form
        String hashed = BCrypt.hashpw(newUser.getPasswordHash(), BCrypt.gensalt(10));
        newUser.setPasswordHash(hashed);
        newUser.setRole(UserRole.STUDENT);

        try {
            long generatedUserId = userDAO.insert(newUser);
            if (generatedUserId <= 0) {
                generatedUserId = newUser.getUserId();
            }
            if (generatedUserId <= 0) {
                Optional<User> inserted = userDAO.findByUsername(newUser.getUsername());
                if (inserted.isPresent()) {
                    generatedUserId = inserted.get().getUserId();
                }
            }
            if (generatedUserId <= 0) {
                conn.rollback();
                throw new RuntimeException("User insert succeeded but user ID could not be resolved — rolled back.");
            }
            newStudent.setUserId(generatedUserId);
            studentDAO.insert(newStudent);

            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException re) {
                /* best effort */ }
            throw new RuntimeException("Student registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the academic profile fields of an existing student (does NOT change
     * credentials or name — those belong to the USERS table which an admin would
     * update separately).
     *
     * @param student Student with a valid studentId and updated fields
     * @throws RuntimeException on JDBC failure; transaction rolled back
     */
    public void updateStudentProfile(Student student) {
        try {
            studentDAO.update(student);
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException re) {
                /* best effort */ }
            throw new RuntimeException("Profile update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all students whose roll number OR full name contains the keyword
     * (case-insensitive). If keyword is blank, returns all students.
     *
     * @param keyword search term (may be blank)
     * @return filtered list; never null
     */
    public List<Student> searchStudents(String keyword) {
        try {
            List<Student> all = studentDAO.findAll();
            if (keyword == null || keyword.isBlank()) {
                return all;
            }
            String lower = keyword.trim().toLowerCase();
            return all.stream()
                    .filter(s -> s.getRollNumber().toLowerCase().contains(lower)
                            || s.getFullName().toLowerCase().contains(lower)
                            || s.getDepartment().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Student search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Soft-deletes a student by setting IS_ACTIVE = 0 on the USERS row.
     * The STUDENT row is retained for historical reporting.
     *
     * @param userId the USER_ID of the student to deactivate
     * @throws RuntimeException on JDBC failure; transaction rolled back
     */
    public void deactivateStudent(long userId) {
        try {
            userDAO.deactivate(userId);
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException re) {
                /* best effort */ }
            throw new RuntimeException("Deactivation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all students (no filter). Convenience method for the initial
     * table load.
     *
     * @return full student list; never null
     */
    public List<Student> getAllStudents() {
        return searchStudents("");
    }
}
