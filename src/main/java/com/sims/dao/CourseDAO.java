package com.sims.dao;

import com.sims.model.Course;
import com.sims.model.FacultyCourseAllocation;
import com.sims.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code COURSE} and {@code FACULTY_COURSE_ALLOCATION} tables.
 *
 * <h2>Design Pattern – Structural: Data Access Object (DAO)</h2>
 * <p><b>Academic Justification</b>: Encapsulates all SQL execution, mapping,
 * and database interactions for academic courses and faculty course allocations.
 * Controllers interact with strongly-typed {@link Course} models without coupling
 * to vendor-specific JDBC code.</p>
 */
public class CourseDAO {

    private final Connection conn;

    public CourseDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── SQL queries ──────────────────────────────────────────────────────────

    private static final String SQL_FIND_BY_ID =
        "SELECT c.COURSE_ID, c.COURSE_NAME, c.COURSE_CODE, c.CREDITS, c.SEMESTER, c.DEPT_ID, " +
        "       d.DEPT_NAME, d.DEPT_CODE " +
        "FROM   COURSE c LEFT JOIN DEPARTMENT d ON c.DEPT_ID = d.DEPT_ID " +
        "WHERE  c.COURSE_ID = ?";

    private static final String SQL_FIND_ALL =
        "SELECT c.COURSE_ID, c.COURSE_NAME, c.COURSE_CODE, c.CREDITS, c.SEMESTER, c.DEPT_ID, " +
        "       d.DEPT_NAME, d.DEPT_CODE " +
        "FROM   COURSE c LEFT JOIN DEPARTMENT d ON c.DEPT_ID = d.DEPT_ID " +
        "ORDER BY c.SEMESTER, c.COURSE_CODE";

    /**
     * For Semester 1 (common baseline), courses have DEPT_ID IS NULL, so this
     * query returns common courses for any dept. For Semesters 2 to 8, it filters
     * by department.
     */
    private static final String SQL_FIND_BY_DEPT_AND_SEM =
        "SELECT c.COURSE_ID, c.COURSE_NAME, c.COURSE_CODE, c.CREDITS, c.SEMESTER, c.DEPT_ID, " +
        "       d.DEPT_NAME, d.DEPT_CODE " +
        "FROM   COURSE c LEFT JOIN DEPARTMENT d ON c.DEPT_ID = d.DEPT_ID " +
        "WHERE  c.SEMESTER = ? " +
        "  AND  (c.DEPT_ID = ? OR c.DEPT_ID IS NULL) " +
        "ORDER BY c.COURSE_CODE";

    private static final String SQL_FIND_ALLOCATED_BY_FACULTY =
        "SELECT c.COURSE_ID, c.COURSE_NAME, c.COURSE_CODE, c.CREDITS, c.SEMESTER, c.DEPT_ID, " +
        "       d.DEPT_NAME, d.DEPT_CODE, fca.ALLOCATION_ID, fca.ACADEMIC_YEAR " +
        "FROM   FACULTY_COURSE_ALLOCATION fca " +
        "JOIN   COURSE c ON fca.COURSE_ID = c.COURSE_ID " +
        "LEFT JOIN DEPARTMENT d ON c.DEPT_ID = d.DEPT_ID " +
        "WHERE  fca.FACULTY_ID = ? " +
        "ORDER BY c.SEMESTER, c.COURSE_CODE";

    private static final String SQL_FIND_ALLOCATED_BY_FACULTY_AND_SEM =
        "SELECT c.COURSE_ID, c.COURSE_NAME, c.COURSE_CODE, c.CREDITS, c.SEMESTER, c.DEPT_ID, " +
        "       d.DEPT_NAME, d.DEPT_CODE, fca.ALLOCATION_ID, fca.ACADEMIC_YEAR " +
        "FROM   FACULTY_COURSE_ALLOCATION fca " +
        "JOIN   COURSE c ON fca.COURSE_ID = c.COURSE_ID " +
        "LEFT JOIN DEPARTMENT d ON c.DEPT_ID = d.DEPT_ID " +
        "WHERE  fca.FACULTY_ID = ? AND c.SEMESTER = ? " +
        "ORDER BY c.COURSE_CODE";

    // ── Public API ───────────────────────────────────────────────────────────

    public Optional<Course> findById(long courseId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapCourse(rs));
            }
        }
        return Optional.empty();
    }

    public List<Course> findAll() throws SQLException {
        List<Course> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapCourse(rs));
        }
        return list;
    }

    public List<Course> getCoursesByDeptAndSemester(Long deptId, int semesterNo) throws SQLException {
        List<Course> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DEPT_AND_SEM)) {
            ps.setInt(1, semesterNo);
            if (deptId != null && deptId > 0) {
                ps.setLong(2, deptId);
            } else {
                ps.setNull(2, java.sql.Types.NUMERIC);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCourse(rs));
            }
        }
        return list;
    }

    public List<Course> getFacultyAllocatedCourses(long facultyId) throws SQLException {
        List<Course> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALLOCATED_BY_FACULTY)) {
            ps.setLong(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCourse(rs));
            }
        }
        return list;
    }

    public List<Course> getFacultyAllocatedCoursesBySemester(long facultyId, int semesterNo) throws SQLException {
        List<Course> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALLOCATED_BY_FACULTY_AND_SEM)) {
            ps.setLong(1, facultyId);
            ps.setInt(2, semesterNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCourse(rs));
            }
        }
        return list;
    }

    // ── Mapping Helper ───────────────────────────────────────────────────────

    private Course mapCourse(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setCourseId(rs.getLong("COURSE_ID"));
        c.setCourseName(rs.getString("COURSE_NAME"));
        c.setCourseCode(rs.getString("COURSE_CODE"));
        c.setCredits(rs.getInt("CREDITS"));
        c.setSemester(rs.getInt("SEMESTER"));
        long deptId = rs.getLong("DEPT_ID");
        if (!rs.wasNull()) {
            c.setDeptId(deptId);
        }
        c.setDeptName(rs.getString("DEPT_NAME"));
        c.setDeptCode(rs.getString("DEPT_CODE"));
        return c;
    }
}
