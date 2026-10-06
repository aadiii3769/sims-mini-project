package com.sims.dao;

import com.sims.model.Mark;
import com.sims.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code MARKS} table.
 *
 * <p>Provides CRUD for CAT marks and a GPA aggregation query.
 * TOTAL_MARKS is intentionally excluded from persistence (see 3NF note
 * in docs/DATABASE.md); it is computed by the controller layer.
 */
public class MarksDAO {

    private final Connection conn;

    public MarksDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String SQL_FIND_BY_STUDENT_SEM =
        "SELECT m.RECORD_ID, m.STUDENT_ID, m.FACULTY_ID, m.SUBJECT, m.SEMESTER, " +
        "       m.CAT1_MARKS, m.CAT2_MARKS, m.CAT3_MARKS, m.GRADE_POINT, " +
        "       m.ACADEMIC_YEAR, u.FULL_NAME " +
        "FROM   MARKS m JOIN USERS u ON m.STUDENT_ID = " +
        "       (SELECT USER_ID FROM STUDENT WHERE STUDENT_ID = m.STUDENT_ID) " +
        "WHERE  m.STUDENT_ID = ? AND m.SEMESTER = ? " +
        "ORDER BY m.SUBJECT";

    private static final String SQL_FIND_BY_STUDENT =
        "SELECT m.RECORD_ID, m.STUDENT_ID, m.FACULTY_ID, m.SUBJECT, m.SEMESTER, " +
        "       m.CAT1_MARKS, m.CAT2_MARKS, m.CAT3_MARKS, m.GRADE_POINT, " +
        "       m.ACADEMIC_YEAR, u.FULL_NAME " +
        "FROM   MARKS m " +
        "JOIN   STUDENT st ON m.STUDENT_ID = st.STUDENT_ID " +
        "JOIN   USERS u    ON st.USER_ID   = u.USER_ID " +
        "WHERE  m.STUDENT_ID = ? " +
        "ORDER BY m.SEMESTER, m.SUBJECT";

    private static final String SQL_INSERT =
        "INSERT INTO MARKS (STUDENT_ID, FACULTY_ID, SUBJECT, SEMESTER, " +
        "                   CAT1_MARKS, CAT2_MARKS, CAT3_MARKS, GRADE_POINT, ACADEMIC_YEAR) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE MARKS " +
        "SET CAT1_MARKS = ?, CAT2_MARKS = ?, CAT3_MARKS = ?, GRADE_POINT = ? " +
        "WHERE RECORD_ID = ?";

    /** Computes semester GPA for a student: AVG(GRADE_POINT) for a given semester. */
    private static final String SQL_GPA =
        "SELECT AVG(GRADE_POINT) AS GPA " +
        "FROM   MARKS " +
        "WHERE  STUDENT_ID = ? AND SEMESTER = ?";

    /** Computes CGPA across all semesters. */
    private static final String SQL_CGPA =
        "SELECT AVG(GRADE_POINT) AS CGPA " +
        "FROM   MARKS " +
        "WHERE  STUDENT_ID = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    public List<Mark> findByStudentAndSemester(long studentId, int semester) throws SQLException {
        List<Mark> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT_SEM)) {
            ps.setLong(1, studentId);
            ps.setInt(2, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
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

    public void insert(Mark mark) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setLong(1, mark.getStudentId());
            ps.setLong(2, mark.getFacultyId());
            ps.setString(3, mark.getSubject());
            ps.setInt(4, mark.getSemester());
            ps.setDouble(5, mark.getCat1Marks());
            ps.setDouble(6, mark.getCat2Marks());
            ps.setDouble(7, mark.getCat3Marks());
            ps.setDouble(8, mark.getGradePoint());
            ps.setString(9, mark.getAcademicYear());
            ps.executeUpdate();
        }
    }

    public int update(Mark mark) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setDouble(1, mark.getCat1Marks());
            ps.setDouble(2, mark.getCat2Marks());
            ps.setDouble(3, mark.getCat3Marks());
            ps.setDouble(4, mark.getGradePoint());
            ps.setLong(5, mark.getRecordId());
            return ps.executeUpdate();
        }
    }

    public double computeGPA(long studentId, int semester) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_GPA)) {
            ps.setLong(1, studentId);
            ps.setInt(2, semester);
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
        m.setFacultyId(rs.getLong("FACULTY_ID"));
        m.setSubject(rs.getString("SUBJECT"));
        m.setSemester(rs.getInt("SEMESTER"));
        m.setCat1Marks(rs.getDouble("CAT1_MARKS"));
        m.setCat2Marks(rs.getDouble("CAT2_MARKS"));
        m.setCat3Marks(rs.getDouble("CAT3_MARKS"));
        m.setGradePoint(rs.getDouble("GRADE_POINT"));
        m.setAcademicYear(rs.getString("ACADEMIC_YEAR"));
        // Compute transient total in-memory
        m.setTotalMarks(m.getCat1Marks() + m.getCat2Marks() + m.getCat3Marks());
        return m;
    }
}
