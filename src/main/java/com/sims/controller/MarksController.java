package com.sims.controller;

import com.sims.dao.CourseDAO;
import com.sims.dao.FacultyDAO;
import com.sims.dao.MarksDAO;
import com.sims.dao.StudentDAO;
import com.sims.model.Course;
import com.sims.model.Faculty;
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
 * Business logic controller for continuous assessments (CATs & Assignments),
 * semester grade derivation, GPA/CGPA calculations, and multi-department course queries.
 *
 * <h2>Design Pattern – Behavioral: Observer (indirect)</h2>
 * <p><b>Academic Justification</b>: Swing {@code ActionListener} observers in
 * {@code MarksEntryPanel} and {@code MarksViewPanel} invoke this controller when
 * faculty save marks or students switch semesters. This class coordinates
 * domain validation, grade calculations, DAOs, and transaction demarcation.</p>
 *
 * <h2>Transaction Demarcation</h2>
 * <p>{@code MarksDAO} methods perform DML operations without committing.
 * This controller issues {@code conn.commit()} on complete success or
 * {@code conn.rollback()} on any exception, complying with AGENTS.md §4.3.</p>
 */
public class MarksController {

    private final MarksDAO   marksDAO;
    private final CourseDAO  courseDAO;
    private final FacultyDAO facultyDAO;
    private final StudentDAO studentDAO;
    private final Connection conn;

    public MarksController() {
        this.conn        = DBConnection.getInstance().getConnection();
        this.marksDAO    = new MarksDAO();
        this.courseDAO   = new CourseDAO();
        this.facultyDAO  = new FacultyDAO();
        this.studentDAO  = new StudentDAO();
    }

    // ── Grade scale & derivation ─────────────────────────────────────────────

    /**
     * Derives the 10-point grade point from raw total internal marks.
     *
     * @param totalMarks sum of CAT1 + CAT2 + Assignment
     * @return grade point (10.0, 9.0, 8.0, 7.0, 6.0, or 0.0 for Fail)
     */
    public double deriveGradePoint(double totalMarks) {
        if (totalMarks >= 90) return 10.0;
        if (totalMarks >= 80) return  9.0;
        if (totalMarks >= 70) return  8.0;
        if (totalMarks >= 60) return  7.0;
        if (totalMarks >= 50) return  6.0;
        return 0.0;   // Fail
    }

    /**
     * Maps a grade point to its Anna University letter grade.
     *
     * @param gradePoint numeric grade (0.0 – 10.0)
     * @return letter grade ("O", "A+", "A", "B+", "B", or "F")
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
     * Persists an assessment record. Computes {@code totalInternal},
     * {@code gradePoint}, and {@code semesterGrade} before delegating to DAO.
     * Commits transaction atomically or rolls back on failure.
     */
    public void saveMarks(Mark mark) {
        if (mark == null) {
            throw new IllegalArgumentException("Mark record is required.");
        }
        validateMarkFields(mark);

        double total      = mark.getCat1Marks() + mark.getCat2Marks() + mark.getAssignmentMarks();
        double gradePoint = deriveGradePoint(total);
        String grade      = getLetterGrade(gradePoint);

        mark.setTotalInternal(total);
        mark.setGradePoint(gradePoint);
        mark.setSemesterGrade(grade);

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

    // ── Query helpers for Student & Parent ───────────────────────────────────

    public List<Mark> getMarksBySemester(long studentId, int semesterNo) {
        return getMarksByStudentAndSemester(studentId, semesterNo);
    }

    public List<Mark> getMarksByStudentAndSemester(long studentId, int semesterNo) {
        try {
            return marksDAO.getMarksByStudentAndSemester(studentId, semesterNo);
        } catch (SQLException e) {
            throw new RuntimeException("Marks lookup failed: " + e.getMessage(), e);
        }
    }

    public List<Mark> getAllMarks(long studentId) {
        try {
            return marksDAO.findAllByStudent(studentId);
        } catch (SQLException e) {
            throw new RuntimeException("All marks lookup failed: " + e.getMessage(), e);
        }
    }

    public double getSemesterGPA(long studentId, int semesterNo) {
        try {
            return Math.round(marksDAO.computeGPA(studentId, semesterNo) * 100.0) / 100.0;
        } catch (SQLException e) {
            throw new RuntimeException("GPA computation failed: " + e.getMessage(), e);
        }
    }

    public double getCGPA(long studentId) {
        try {
            return Math.round(marksDAO.computeCGPA(studentId) * 100.0) / 100.0;
        } catch (SQLException e) {
            throw new RuntimeException("CGPA computation failed: " + e.getMessage(), e);
        }
    }

    // ── Query helpers for Faculty ────────────────────────────────────────────

    public Optional<Faculty> findFacultyProfileByUserId(long userId) {
        try {
            return facultyDAO.findByUserId(userId);
        } catch (SQLException e) {
            throw new RuntimeException("Faculty profile lookup failed: " + e.getMessage(), e);
        }
    }

    public List<Course> getCoursesByDeptAndSemester(Long deptId, int semesterNo) {
        try {
            return courseDAO.getCoursesByDeptAndSemester(deptId, semesterNo);
        } catch (SQLException e) {
            throw new RuntimeException("Courses lookup failed: " + e.getMessage(), e);
        }
    }

    public List<Course> getFacultyAllocatedCourses(long facultyId) {
        try {
            return courseDAO.getFacultyAllocatedCourses(facultyId);
        } catch (SQLException e) {
            throw new RuntimeException("Allocated courses lookup failed: " + e.getMessage(), e);
        }
    }

    public List<Course> getFacultyAllocatedCoursesBySemester(long facultyId, int semesterNo) {
        try {
            return courseDAO.getFacultyAllocatedCoursesBySemester(facultyId, semesterNo);
        } catch (SQLException e) {
            throw new RuntimeException("Allocated courses lookup failed: " + e.getMessage(), e);
        }
    }

    public List<Student> getStudentsForCourseEntry(Long deptId, int semesterNo) {
        try {
            return studentDAO.findByDeptAndSemester(deptId, semesterNo);
        } catch (SQLException e) {
            throw new RuntimeException("Eligible students lookup failed: " + e.getMessage(), e);
        }
    }

    public List<Mark> getMarksByCourseAndSemester(long courseId, int semesterNo) {
        try {
            return marksDAO.getMarksByCourseAndSemester(courseId, semesterNo);
        } catch (SQLException e) {
            throw new RuntimeException("Course marks lookup failed: " + e.getMessage(), e);
        }
    }

    public Optional<Mark> findExistingMark(long studentId, long courseId, int semesterNo) {
        try {
            return marksDAO.findByStudentCourseAndSem(studentId, courseId, semesterNo);
        } catch (SQLException e) {
            throw new RuntimeException("Mark lookup failed: " + e.getMessage(), e);
        }
    }

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

    // ── Validation & transaction cleanup ─────────────────────────────────────

    private void validateMarkFields(Mark mark) {
        if (mark.getStudentId() <= 0) {
            throw new IllegalArgumentException("Student selection is required.");
        }
        if (mark.getCourseId() <= 0) {
            throw new IllegalArgumentException("Course selection is required.");
        }
        if (mark.getSemesterNo() < 1 || mark.getSemesterNo() > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
        if (mark.getCat1Marks() < 0 || mark.getCat1Marks() > 50) {
            throw new IllegalArgumentException("CAT 1 marks must be between 0 and 50.");
        }
        if (mark.getCat2Marks() < 0 || mark.getCat2Marks() > 50) {
            throw new IllegalArgumentException("CAT 2 marks must be between 0 and 50.");
        }
        if (mark.getAssignmentMarks() < 0 || mark.getAssignmentMarks() > 50) {
            throw new IllegalArgumentException("Assignment marks must be between 0 and 50.");
        }
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            System.err.println("[WARN] Marks rollback failed: " + ex.getMessage());
        }
    }
}
