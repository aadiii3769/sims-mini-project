package com.sims.dao;

import com.sims.model.Department;
import com.sims.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code DEPARTMENT} table.
 *
 * <h2>Design Pattern – Structural: Data Access Object (DAO)</h2>
 * <p><b>Academic Justification</b>: Encapsulates all query logic for academic
 * department entities, isolating schema details from business controllers.</p>
 */
public class DepartmentDAO {

    private final Connection conn;

    public DepartmentDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    private static final String SQL_FIND_ALL =
        "SELECT DEPT_ID, DEPT_NAME, DEPT_CODE FROM DEPARTMENT ORDER BY DEPT_ID";

    private static final String SQL_FIND_BY_ID =
        "SELECT DEPT_ID, DEPT_NAME, DEPT_CODE FROM DEPARTMENT WHERE DEPT_ID = ?";

    private static final String SQL_FIND_BY_CODE =
        "SELECT DEPT_ID, DEPT_NAME, DEPT_CODE FROM DEPARTMENT WHERE DEPT_CODE = ?";

    public List<Department> findAll() throws SQLException {
        List<Department> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Optional<Department> findById(long deptId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setLong(1, deptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Department> findByCode(String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CODE)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        return new Department(
            rs.getLong("DEPT_ID"),
            rs.getString("DEPT_NAME"),
            rs.getString("DEPT_CODE")
        );
    }
}
