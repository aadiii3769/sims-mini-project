package com.sims.dao;

import com.sims.model.Mark;
import com.sims.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code MARKS} table.
 *
 * <h2>Design Pattern – Structural: Data Access Object (DAO)</h2>
 * <p><b>Academic Justification</b>: Encapsulates all SQL execution, mapping,
 * and database interactions for continuous assessments (CAT1, CAT2, Assignment),
 * internal marks, semester grades, and GPA calculations.</p>
 */
public class MarksDAO {

    private final Connection conn;

    public MarksDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String SQL_BASE_SELECT =
        "SELECT m.RECORD_ID, m.STUDENT_ID, m.COURSE_ID, m.CAT1_MARKS, m.CAT2_MARKS, " +
        "       m.ASSIGNMENT_MARKS, m.TOTAL_INTERNAL, m.SEMESTER_GRADE, m.GRADE_POINT, " +
        "       m.SEMESTER_NO, m.IS_COMPLETED, m.FACULTY_ID, m.ACADEMIC_YEAR, " +
        "       c.COURSE_NAME, c.COURSE_CODE, c.CREDITS, " +
        "       u.FULL_NAME AS STUDENT_NAME, s.ROLL_NUMBER " +
        "FROM   MARKS m " +
        "JOIN   COURSE c   ON m.COURSE_ID = c.COURSE_ID " +
        "JOIN   STUDENT s  ON m.STUDENT_ID = s.STUDENT_ID " +
        "JOIN   USERS u    ON s.USER_ID = u.USER_ID ";

    private static final String SQL_FIND_BY_STUDENT_SEM =
        SQL_BASE_SELECT +
        "WHERE  m.STUDENT_ID = ? AND m.SEMESTER_NO = ? " +
        "ORDER BY c.COURSE_CODE";

    private static final String SQL_FIND_BY_STUDENT =
        SQL_BASE_SELECT +
        "WHERE  m.STUDENT_ID = ? " +
        "ORDER BY m.SEMESTER_NO, c.COURSE_CODE";

    private static final String SQL_FIND_BY_COURSE_SEM =
        SQL_BASE_SELECT +
        "WHERE  m.COURSE_ID = ? AND m.SEMESTER_NO = ? " +
        "ORDER BY s.ROLL_NUMBER";

    private static final String SQL_FIND_BY_STUDENT_COURSE_SEM =
        SQL_BASE_SELECT +
        "WHERE  m.STUDENT_ID = ? AND m.COURSE_ID = ? AND m.SEMESTER_NO = ?";

    private static final String SQL_INSERT =
        "INSERT INTO MARKS (STUDENT_ID, COURSE_ID, CAT1_MARKS, CAT2_MARKS, " +
        "                   ASSIGNMENT_MARKS, TOTAL_INTERNAL, SEMESTER_GRADE, " +
        "                   GRADE_POINT, SEMESTER_NO, IS_COMPLETED, FACULTY_ID, ACADEMIC_YEAR) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE MARKS " +
        "SET CAT1_MARKS = ?, CAT2_MARKS = ?, ASSIGNMENT_MARKS = ?, TOTAL_INTERNAL = ?, " +
        "    SEMESTER_GRADE = ?, GRADE_POINT = ?, IS_COMPLETED = ?, ACADEMIC_YEAR = ? " +
        "WHERE RECORD_ID = ?";

    /** Computes semester GPA for a student: AVG(GRADE_POINT) for a given semester. */
    private static final String SQL_GPA =
        "SELECT AVG(GRADE_POINT) AS GPA " +
        "FROM   MARKS " +
        "WHERE  STUDENT_ID = ? AND SEMESTER_NO = ?";

    /** Computes CGPA across all semesters. */
    private static final String SQL_CGPA =
        "SELECT AVG(GRADE_POINT) AS CGPA " +
        "FROM   MARKS " +
        "WHERE  STUDENT_ID = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    public List<Mark> getMarksByStudentAndSemester(long studentId, int semesterNo) throws SQLException {
        List<Mark> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT_SEM)) {
            ps.setLong(1, studentId);
            ps.setInt(2, semesterNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Alias for backwards-compatibility */
    public List<Mark> findByStudentAndSemester(long studentId, int semesterNo) throws SQLException {
        return getMarksByStudentAndSemester(studentId, semesterNo);
    }

    public List<Mark> findAllByStudent(long studentId) throws SQLException {
        List<Mark> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Mark> getMarksByCourseAndSemester(long courseId, int semesterNo) throws SQLException {
        List<Mark> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_COURSE_SEM)) {
            ps.setLong(1, courseId);
            ps.setInt(2, semesterNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Optional<Mark> findByStudentCourseAndSem(long studentId, long courseId, int semesterNo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT_COURSE_SEM)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            ps.setInt(3, semesterNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public void insert(Mark mark) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setLong(1, mark.getStudentId());
            ps.setLong(2, mark.getCourseId());
            ps.setDouble(3, mark.getCat1Marks());
            ps.setDouble(4, mark.getCat2Marks());
            ps.setDouble(5, mark.getAssignmentMarks());
            ps.setDouble(6, mark.getTotalInternal());
            ps.setString(7, mark.getSemesterGrade());
            ps.setDouble(8, mark.getGradePoint());
            ps.setInt(9, mark.getSemesterNo());
            ps.setInt(10, mark.isCompleted() ? 1 : 0);
            if (mark.getFacultyId() != null && mark.getFacultyId() > 0) {
                ps.setLong(11, mark.getFacultyId());
            } else {
                ps.setNull(11, Types.NUMERIC);
            }
            ps.setString(12, mark.getAcademicYear() != null ? mark.getAcademicYear() : "2025-26");
            ps.executeUpdate();
        }
    }

    public int update(Mark mark) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setDouble(1, mark.getCat1Marks());
            ps.setDouble(2, mark.getCat2Marks());
            ps.setDouble(3, mark.getAssignmentMarks());
            ps.setDouble(4, mark.getTotalInternal());
            ps.setString(5, mark.getSemesterGrade());
            ps.setDouble(6, mark.getGradePoint());
            ps.setInt(7, mark.isCompleted() ? 1 : 0);
            ps.setString(8, mark.getAcademicYear() != null ? mark.getAcademicYear() : "2025-26");
            ps.setLong(9, mark.getRecordId());
            return ps.executeUpdate();
        }
    }

    public double computeGPA(long studentId, int semesterNo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_GPA)) {
            ps.setLong(1, studentId);
            ps.setInt(2, semesterNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("GPA");
            }
        }
        return 0.0;
    }

    public double computeCGPA(long studentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_CGPA)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("CGPA");
            }
        }
        return 0.0;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Mark mapRow(ResultSet rs) throws SQLException {
        Mark m = new Mark();
        m.setRecordId(rs.getLong("RECORD_ID"));
        m.setStudentId(rs.getLong("STUDENT_ID"));
        m.setCourseId(rs.getLong("COURSE_ID"));
        m.setCat1Marks(rs.getDouble("CAT1_MARKS"));
        m.setCat2Marks(rs.getDouble("CAT2_MARKS"));
        m.setAssignmentMarks(rs.getDouble("ASSIGNMENT_MARKS"));
        m.setTotalInternal(rs.getDouble("TOTAL_INTERNAL"));
        m.setSemesterGrade(rs.getString("SEMESTER_GRADE"));
        m.setGradePoint(rs.getDouble("GRADE_POINT"));
        m.setSemesterNo(rs.getInt("SEMESTER_NO"));
        m.setCompleted(rs.getInt("IS_COMPLETED") == 1);
        long fid = rs.getLong("FACULTY_ID");
        if (!rs.wasNull()) m.setFacultyId(fid);
        m.setAcademicYear(rs.getString("ACADEMIC_YEAR"));
        m.setCourseName(rs.getString("COURSE_NAME"));
        m.setCourseCode(rs.getString("COURSE_CODE"));
        m.setCredits(rs.getInt("CREDITS"));
        m.setStudentName(rs.getString("STUDENT_NAME"));
        m.setRollNumber(rs.getString("ROLL_NUMBER"));

        // Derived total and letter grade
        if (m.getTotalInternal() <= 0 && (m.getCat1Marks() > 0 || m.getCat2Marks() > 0 || m.getAssignmentMarks() > 0)) {
            m.setTotalInternal(m.getCat1Marks() + m.getCat2Marks() + m.getAssignmentMarks());
        }
        return m;
    }
}
