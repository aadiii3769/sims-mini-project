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
 * <p>
 * <b>Academic Justification</b>: All SQL is confined here, behind a clean
 * Java API. Controllers never see a {@link PreparedStatement} or SQL syntax;
 * they interact strictly with {@link User} domain objects. This decoupling
 * ensures
 * database dialect portability and maintains a clean separation of concerns.
 * </p>
 *
 * <h2>Security</h2>
 * <p>
 * Every query uses {@link PreparedStatement} with {@code ?} parameter
 * placeholders.
 * Concatenating user input into SQL strings is strictly forbidden (SQL
 * injection prevention).
 * </p>
 *
 * <h2>Resource Management</h2>
 * <p>
 * All {@link PreparedStatement} and {@link ResultSet} objects are opened
 * inside {@code try-with-resources} blocks so they are closed automatically
 * even when exceptions occur. The shared {@link Connection} from
 * {@link DBConnection} is intentionally NOT closed here.
 * </p>
 */
public class UserDAO {

    public UserDAO() {
    }

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String SQL_FIND_BY_USERNAME = "SELECT USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, "
            +
            "       ROLE, IS_ACTIVE, CREATED_AT " +
            "FROM   USERS " +
            "WHERE  UPPER(USERNAME) = UPPER(?)";

    private static final String SQL_FIND_BY_ID = "SELECT USER_ID, USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, " +
            "       ROLE, IS_ACTIVE, CREATED_AT " +
            "FROM   USERS " +
            "WHERE  USER_ID = ?";

    private static final String SQL_INSERT_USER = "INSERT INTO USERS (USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE) "
            +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_USER = "UPDATE USERS SET FULL_NAME = ?, EMAIL = ?, PHONE = ? " +
            "WHERE USER_ID = ?";

    private static final String SQL_UPDATE_PASSWORD = "UPDATE USERS SET PASSWORD_HASH = ? WHERE USER_ID = ?";

    private static final String SQL_UPDATE_ACTIVE = "UPDATE USERS SET IS_ACTIVE = ? WHERE USER_ID = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Looks up a user by their username (case-insensitive).
     *
     * @param username the login username entered by the user
     * @return {@code Optional<User>} — empty if not found
     * @throws SQLException on any JDBC error
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        try (PreparedStatement ps = getConnection().prepareStatement(SQL_FIND_BY_USERNAME)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Looks up a user by primary key ID.
     *
     * @param userId the user ID
     * @return {@code Optional<User>} — empty if not found
     * @throws SQLException on any JDBC error
     */
    public Optional<User> findById(long userId) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(SQL_FIND_BY_ID)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a username already exists (case-insensitive).
     *
     * @param username the username to check
     * @return {@code true} if username exists, {@code false} otherwise
     * @throws SQLException on JDBC error
     */
    public boolean existsByUsername(String username) throws SQLException {
        return findByUsername(username).isPresent();
    }

    /**
     * Inserts a new user row and automatically sets the generated {@code USER_ID}
     * on the supplied {@link User} object. Transaction commit must be called by the
     * controller after validating all related inserts.
     *
     * @param user the {@link User} object to persist
     * @return the generated {@code USER_ID}
     * @throws SQLException on any JDBC error or constraint violation
     */
    public long insert(User user) throws SQLException {
        long generatedId = -1;
        Connection conn = getConnection();

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_USER, new String[] { "USER_ID" })) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            // Fallback for Oracle environments where return-keys array may not be supported
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_USER)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getPhone());
                ps.setString(6, user.getRole().name());
                ps.executeUpdate();
            }
        }

        if (generatedId <= 0) {
            Optional<User> created = findByUsername(user.getUsername());
            if (created.isPresent()) {
                generatedId = created.get().getUserId();
            }
        }

        user.setUserId(generatedId);
        return generatedId;
    }

    /**
     * Updates non-credential profile attributes (full name, email, phone).
     *
     * @param user the {@link User} with updated fields and valid {@code userId}
     * @throws SQLException on any JDBC error
     */
    public void update(User user) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(SQL_UPDATE_USER)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setLong(4, user.getUserId());
            ps.executeUpdate();
        }
    }

    /**
     * Updates the password hash for a user.
     *
     * @param userId       the target user ID
     * @param passwordHash the new BCrypt password hash
     * @throws SQLException on any JDBC error
     */
    public void updatePassword(long userId, String passwordHash) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Activates a user by setting {@code IS_ACTIVE = 1}.
     *
     * @param userId the target user ID
     * @throws SQLException on any JDBC error
     */
    public void activate(long userId) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(SQL_UPDATE_ACTIVE)) {
            ps.setInt(1, 1);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Soft-deletes (deactivates) a user by setting {@code IS_ACTIVE = 0}.
     *
     * @param userId the target user ID
     * @throws SQLException on any JDBC error
     */
    public void deactivate(long userId) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(SQL_UPDATE_ACTIVE)) {
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
        String roleStr = rs.getString("ROLE");
        if (roleStr != null) {
            u.setRole(UserRole.fromString(roleStr));
        }
        u.setActive(rs.getInt("IS_ACTIVE") == 1);
        Timestamp ts = rs.getTimestamp("CREATED_AT");
        if (ts != null) {
            u.setCreatedAt(ts.toLocalDateTime());
        }
        return u;
    }
}
