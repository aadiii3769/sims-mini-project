package com.sims.dao;

import com.sims.model.Faculty;
import com.sims.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code FACULTY} table.
 *
 * <h2>Design Pattern – Structural: Data Access Object (DAO)</h2>
 * <p><b>Academic Justification</b>: Encapsulates database operations for
 * faculty profiles and their department mappings.</p>
 */
public class FacultyDAO {

    private final Connection conn;

    public FacultyDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    private static final String SQL_FIND_BY_USER_ID =
        "SELECT f.FACULTY_ID, f.USER_ID, f.DEPT_ID, f.DESIGNATION, " +
        "       u.FULL_NAME, u.EMAIL, u.PHONE, d.DEPT_NAME, d.DEPT_CODE " +
        "FROM   FACULTY f " +
        "JOIN   USERS u      ON f.USER_ID = u.USER_ID " +
        "JOIN   DEPARTMENT d ON f.DEPT_ID = d.DEPT_ID " +
        "WHERE  f.USER_ID = ?";

    private static final String SQL_FIND_BY_FACULTY_ID =
        "SELECT f.FACULTY_ID, f.USER_ID, f.DEPT_ID, f.DESIGNATION, " +
        "       u.FULL_NAME, u.EMAIL, u.PHONE, d.DEPT_NAME, d.DEPT_CODE " +
        "FROM   FACULTY f " +
        "JOIN   USERS u      ON f.USER_ID = u.USER_ID " +
        "JOIN   DEPARTMENT d ON f.DEPT_ID = d.DEPT_ID " +
        "WHERE  f.FACULTY_ID = ?";

    private static final String SQL_FIND_ALL =
        "SELECT f.FACULTY_ID, f.USER_ID, f.DEPT_ID, f.DESIGNATION, " +
        "       u.FULL_NAME, u.EMAIL, u.PHONE, d.DEPT_NAME, d.DEPT_CODE " +
        "FROM   FACULTY f " +
        "JOIN   USERS u      ON f.USER_ID = u.USER_ID " +
        "JOIN   DEPARTMENT d ON f.DEPT_ID = d.DEPT_ID " +
        "ORDER BY d.DEPT_CODE, u.FULL_NAME";

    public Optional<Faculty> findByUserId(long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USER_ID)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapFaculty(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Faculty> findById(long facultyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_FACULTY_ID)) {
            ps.setLong(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapFaculty(rs));
            }
        }
        return Optional.empty();
    }

    public List<Faculty> findAll() throws SQLException {
        List<Faculty> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapFaculty(rs));
        }
        return list;
    }

    private Faculty mapFaculty(ResultSet rs) throws SQLException {
        Faculty f = new Faculty();
        f.setFacultyId(rs.getLong("FACULTY_ID"));
        f.setUserId(rs.getLong("USER_ID"));
        f.setDeptId(rs.getLong("DEPT_ID"));
        f.setDesignation(rs.getString("DESIGNATION"));
        f.setFullName(rs.getString("FULL_NAME"));
        f.setEmail(rs.getString("EMAIL"));
        f.setPhone(rs.getString("PHONE"));
        f.setDeptName(rs.getString("DEPT_NAME"));
        f.setDeptCode(rs.getString("DEPT_CODE"));
        return f;
    }
}
