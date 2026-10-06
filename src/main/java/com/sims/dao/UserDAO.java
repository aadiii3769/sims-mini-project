package com.sims.dao;

import com.sims.model.User;
import com.sims.model.UserRole;
import com.sims.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Data Access Object for the {@code USERS} table.
 *
 * <h2>Design Pattern – Structural: DAO Pattern</h2>
 * <p><b>Academic Justification</b>: All SQL is confined here, behind a clean
 * Java API. The LoginController and other controllers never see a
 * {@link PreparedStatement} — they call {@code findByUsername()} and get back
 * a domain object. This decoupling means the underlying database or SQL dialect
 * can change without touching any UI or business-logic class.</p>
 *
 * <h2>Security</h2>
 * <p>Every query uses {@link PreparedStatement} with {@code ?} placeholders.
 * Concatenating user input into SQL strings is strictly forbidden (SQL
 * injection prevention).</p>
 *
 * <h2>Resource Management</h2>
 * <p>All {@link PreparedStatement} and {@link ResultSet} objects are opened
 * inside {@code try-with-resources} blocks so they are closed automatically
 * even when exceptions occur. The shared {@link Connection} from
 * {@link DBConnection} is intentionally NOT closed here.</p>
 */
public class UserDAO {

    private final Connection conn;

    public UserDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String SQL_FIND_BY_USERNAME =
        "SELECT USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, " +
        "       ROLE, IS_ACTIVE, CREATED_AT " +
        "FROM   USERS " +
        "WHERE  USERNAME = ? AND IS_ACTIVE = 1";

    private static final String SQL_INSERT_USER =
        "INSERT INTO USERS (USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_ACTIVE =
        "UPDATE USERS SET IS_ACTIVE = ? WHERE USER_ID = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Looks up a user by their username (case-sensitive, as stored).
     *
     * @param username the login username entered by the user
     * @return {@code Optional<User>} — empty if not found or inactive
     * @throws SQLException on any JDBC error
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Inserts a new user row.  Transaction commit must be called by the
     * controller after validating all related inserts (e.g., STUDENT row).
     *
     * @param user the {@link User} object to persist (USER_ID will be assigned by
     *             the Oracle sequence — do not pre-populate it)
     * @throws SQLException on any JDBC error or constraint violation
     */
    public void insert(User user) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_USER)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole().name());
            ps.executeUpdate();
            // Caller must conn.commit() — autoCommit is OFF
        }
    }

    /**
     * Soft-deletes (deactivates) a user by setting {@code IS_ACTIVE = 0}.
     *
     * @param userId the target user ID
     * @throws SQLException on any JDBC error
     */
    public void deactivate(long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_ACTIVE)) {
            ps.setInt(1, 0);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("USER_ID"));
        u.setUsername(rs.getString("USERNAME"));
        u.setPasswordHash(rs.getString("PASSWORD_HASH"));
        u.setFullName(rs.getString("FULL_NAME"));
        u.setEmail(rs.getString("EMAIL"));
        u.setPhone(rs.getString("PHONE"));
        u.setRole(UserRole.fromString(rs.getString("ROLE")));
        u.setActive(rs.getInt("IS_ACTIVE") == 1);
        Timestamp ts = rs.getTimestamp("CREATED_AT");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }
}
