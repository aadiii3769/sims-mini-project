package com.sims.controller;

import com.sims.dao.MarksDAO;
import com.sims.dao.StudentDAO;
import com.sims.model.Mark;
import com.sims.model.Student;
import com.sims.model.User;
import com.sims.model.UserRole;
import com.sims.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic controller for CAT marks entry, grade derivation, and
 * GPA / CGPA computation.
 *
 * <h2>Design Pattern – Behavioral: Observer (indirect)</h2>
 * <p><b>Academic Justification</b>: Swing {@code ActionListener} observers in
 * {@code MarksEntryPanel} and {@code MarksViewPanel} call this controller when
 * faculty save marks or students refresh their grade tables. This class
 * validates input, computes derived grades, coordinates DAO calls, and
 * demarcates transactions — keeping SQL and arithmetic out of the UI.</p>
 *
 * <h2>Transaction Demarcation</h2>
 * <p>{@code MarksDAO} methods execute DML but do NOT commit.  This controller
 * issues {@code conn.commit()} after all DAO operations succeed, or
 * {@code conn.rollback()} on any failure, per the AGENTS.md rule §4.3.</p>
 */
public class MarksController {

    private final MarksDAO   marksDAO;
    private final StudentDAO studentDAO;
    private final Connection conn;

    public MarksController() {
        this.conn        = DBConnection.getInstance().getConnection();
        this.marksDAO    = new MarksDAO();
        this.studentDAO  = new StudentDAO();
    }

    // ── Grade scale constant ─────────────────────────────────────────────────

    /**
     * Derives the 10-point grade point from a raw total mark using the
     * Anna University UG grading scale.
     *
     * @param totalMarks sum of CAT1 + CAT2 + CAT3 (out of 150)
     * @return grade point (10.0, 9.0, 8.0, 7.0, 6.0, or 0.0 for Fail)
     */
    public double deriveGradePoint(double totalMarks) {
        if (totalMarks >= 91) return 10.0;
        if (totalMarks >= 81) return  9.0;
        if (totalMarks >= 71) return  8.0;
        if (totalMarks >= 61) return  7.0;
        if (totalMarks >= 51) return  6.0;
        return 0.0;   // Fail
    }

    /**
     * Maps a grade point value to its letter-grade string.
     *
     * @param gradePoint numeric grade (0.0 – 10.0)
     * @return letter grade such as "O", "A+", "A", "B+", "B", or "F"
     */
    public String getLetterGrade(double gradePoint) {
        if (gradePoint >= 10.0) return "O";
        if (gradePoint >=  9.0) return "A+";
        if (gradePoint >=  8.0) return "A";
        if (gradePoint >=  7.0) return "B+";
        if (gradePoint >=  6.0) return "B";
        return "F";
    }

    // ── Save / update marks ──────────────────────────────────────────────────

    /**
     * Persists a marks record.  If the {@code Mark} already has a positive
     * {@code recordId}, the existing row is updated; otherwise a new row is
     * inserted.  The controller computes {@code totalMarks} and
     * {@code gradePoint} before delegating to the DAO.
     *
     * @param mark the marks object populated by the view (recordId may be 0
     *             for a new record)
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public void saveMarks(Mark mark) {
        if (mark == null) {
            throw new IllegalArgumentException("Mark record is required.");
        }
        validateMarkFields(mark);

        double total      = mark.getCat1Marks() + mark.getCat2Marks() + mark.getCat3Marks();
        double gradePoint = deriveGradePoint(total);
        mark.setTotalMarks(total);
        mark.setGradePoint(gradePoint);
        mark.setLetterGrade(getLetterGrade(gradePoint));

        try {
            if (mark.getRecordId() > 0) {
                marksDAO.update(mark);
            } else {
                marksDAO.insert(mark);
            }
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new RuntimeException("Marks save failed: " + e.getMessage(), e);
        }
    }

    // ── Query helpers ────────────────────────────────────────────────────────

    /**
     * Returns all marks records for a student in a given semester, with
     * derived totals and letter grades populated.
     *
     * @param studentId the target student's primary key
     * @param semester  semester number (1–8)
     * @return list of {@link Mark} objects, may be empty
     */
    public List<Mark> getMarksBySemester(long studentId, int semester) {
        try {
            List<Mark> marks = marksDAO.findByStudentAndSemester(studentId, semester);
            marks.forEach(this::populateDerivedFields);
            return marks;
        } catch (SQLException e) {
            throw new RuntimeException("Marks lookup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all marks records for a student across all semesters, with
     * derived totals and letter grades populated.
     *
     * @param studentId the target student's primary key
     * @return list of {@link Mark} objects ordered by semester then subject
     */
    public List<Mark> getAllMarks(long studentId) {
        try {
            List<Mark> marks = marksDAO.findAllByStudent(studentId);
            marks.forEach(this::populateDerivedFields);
            return marks;
        } catch (SQLException e) {
            throw new RuntimeException("Marks lookup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Computes the GPA for a specific semester using {@code AVG(GRADE_POINT)}.
     *
     * @param studentId the target student's primary key
     * @param semester  semester number
     * @return GPA rounded to two decimal places, or 0.0 if no records exist
     */
    public double getSemesterGPA(long studentId, int semester) {
        try {
            return Math.round(marksDAO.computeGPA(studentId, semester) * 100.0) / 100.0;
        } catch (SQLException e) {
            throw new RuntimeException("GPA computation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Computes the CGPA across all semesters using {@code AVG(GRADE_POINT)}.
     *
     * @param studentId the target student's primary key
     * @return CGPA rounded to two decimal places, or 0.0 if no records exist
     */
    public double getCGPA(long studentId) {
        try {
            return Math.round(marksDAO.computeCGPA(studentId) * 100.0) / 100.0;
        } catch (SQLException e) {
            throw new RuntimeException("CGPA computation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves the student profile linked to the given viewer account.
     * Returns the student linked by USER_ID for STUDENT role, or the child
     * student linked by PARENT_USER_ID for PARENT role.
     *
     * @param viewer the currently logged-in user
     * @return {@code Optional<Student>} — empty if not found or wrong role
     */
    public Optional<Student> findStudentForViewer(User viewer) {
        if (viewer == null || viewer.getRole() == null) {
            return Optional.empty();
        }
        try {
            if (viewer.getRole() == UserRole.STUDENT) {
                return studentDAO.findByUserId(viewer.getUserId());
            }
            if (viewer.getRole() == UserRole.PARENT) {
                return studentDAO.findByParentUserId(viewer.getUserId());
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Student lookup failed: " + e.getMessage(), e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void validateMarkFields(Mark mark) {
        if (mark.getStudentId() <= 0) {
            throw new IllegalArgumentException("Student is required.");
        }
        if (mark.getFacultyId() <= 0) {
            throw new IllegalArgumentException("Faculty is required.");
        }
        if (mark.getSubject() == null || mark.getSubject().isBlank()) {
            throw new IllegalArgumentException("Subject is required.");
        }
        if (mark.getSemester() < 1 || mark.getSemester() > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
        if (mark.getCat1Marks() < 0 || mark.getCat1Marks() > 50) {
            throw new IllegalArgumentException("CAT1 marks must be between 0 and 50.");
        }
        if (mark.getCat2Marks() < 0 || mark.getCat2Marks() > 50) {
            throw new IllegalArgumentException("CAT2 marks must be between 0 and 50.");
        }
        if (mark.getCat3Marks() < 0 || mark.getCat3Marks() > 50) {
            throw new IllegalArgumentException("CAT3 marks must be between 0 and 50.");
        }
        if (mark.getAcademicYear() == null || mark.getAcademicYear().isBlank()) {
            throw new IllegalArgumentException("Academic year is required.");
        }
    }

    private void populateDerivedFields(Mark mark) {
        double total = mark.getCat1Marks() + mark.getCat2Marks() + mark.getCat3Marks();
        mark.setTotalMarks(total);
        mark.setLetterGrade(getLetterGrade(mark.getGradePoint()));
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            System.err.println("[WARN] Marks rollback failed: " + ex.getMessage());
        }
    }
}
