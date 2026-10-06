package com.sims.dao;

import com.sims.model.Student;
import com.sims.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code STUDENT} table.
 *
 * <p>All SQL is encapsulated here; controllers receive and submit
 * {@link Student} POJOs only. See {@link UserDAO} class Javadoc for
 * the full DAO pattern academic justification.
 */
public class StudentDAO {

    private final Connection conn;

    public StudentDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String SQL_FIND_BY_USER_ID =
        "SELECT s.STUDENT_ID, s.USER_ID, s.ROLL_NUMBER, s.DEPARTMENT, s.YEAR, " +
        "       s.SECTION, s.DATE_OF_BIRTH, s.ADDRESS, s.PARENT_USER_ID, " +
        "       u.FULL_NAME, u.EMAIL " +
        "FROM   STUDENT s JOIN USERS u ON s.USER_ID = u.USER_ID " +
        "WHERE  s.USER_ID = ?";

    private static final String SQL_FIND_ALL =
        "SELECT s.STUDENT_ID, s.USER_ID, s.ROLL_NUMBER, s.DEPARTMENT, s.YEAR, " +
        "       s.SECTION, s.DATE_OF_BIRTH, s.ADDRESS, s.PARENT_USER_ID, " +
        "       u.FULL_NAME, u.EMAIL " +
        "FROM   STUDENT s JOIN USERS u ON s.USER_ID = u.USER_ID " +
        "ORDER BY s.ROLL_NUMBER";

    private static final String SQL_FIND_BY_ROLL =
        "SELECT s.STUDENT_ID, s.USER_ID, s.ROLL_NUMBER, s.DEPARTMENT, s.YEAR, " +
        "       s.SECTION, s.DATE_OF_BIRTH, s.ADDRESS, s.PARENT_USER_ID, " +
        "       u.FULL_NAME, u.EMAIL " +
        "FROM   STUDENT s JOIN USERS u ON s.USER_ID = u.USER_ID " +
        "WHERE  s.ROLL_NUMBER = ?";

    private static final String SQL_INSERT =
        "INSERT INTO STUDENT (USER_ID, ROLL_NUMBER, DEPARTMENT, YEAR, SECTION, " +
        "                     DATE_OF_BIRTH, ADDRESS, PARENT_USER_ID) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE STUDENT SET DEPARTMENT = ?, YEAR = ?, SECTION = ?, " +
        "                   DATE_OF_BIRTH = ?, ADDRESS = ?, PARENT_USER_ID = ? " +
        "WHERE STUDENT_ID = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    public Optional<Student> findByUserId(long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USER_ID)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Student> findByRollNumber(String rollNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ROLL)) {
            ps.setString(1, rollNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void insert(Student student) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setLong(1, student.getUserId());
            ps.setString(2, student.getRollNumber());
            ps.setString(3, student.getDepartment());
            ps.setInt(4, student.getYear());
            ps.setString(5, student.getSection());
            if (student.getDateOfBirth() != null) {
                ps.setDate(6, Date.valueOf(student.getDateOfBirth()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            ps.setString(7, student.getAddress());
            if (student.getParentUserId() != null) {
                ps.setLong(8, student.getParentUserId());
            } else {
                ps.setNull(8, Types.NUMERIC);
            }
            ps.executeUpdate();
        }
    }

    public int update(Student student) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, student.getDepartment());
            ps.setInt(2, student.getYear());
            ps.setString(3, student.getSection());
            if (student.getDateOfBirth() != null) {
                ps.setDate(4, Date.valueOf(student.getDateOfBirth()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            ps.setString(5, student.getAddress());
            if (student.getParentUserId() != null) {
                ps.setLong(6, student.getParentUserId());
            } else {
                ps.setNull(6, Types.NUMERIC);
            }
            ps.setLong(7, student.getStudentId());
            return ps.executeUpdate();
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getLong("STUDENT_ID"));
        s.setUserId(rs.getLong("USER_ID"));
        s.setRollNumber(rs.getString("ROLL_NUMBER"));
        s.setDepartment(rs.getString("DEPARTMENT"));
        s.setYear(rs.getInt("YEAR"));
        s.setSection(rs.getString("SECTION"));
        Date dob = rs.getDate("DATE_OF_BIRTH");
        if (dob != null) s.setDateOfBirth(dob.toLocalDate());
        s.setAddress(rs.getString("ADDRESS"));
        long pid = rs.getLong("PARENT_USER_ID");
        if (!rs.wasNull()) s.setParentUserId(pid);
        s.setFullName(rs.getString("FULL_NAME"));
        s.setEmail(rs.getString("EMAIL"));
        return s;
    }
}
