package com.sims.dao;

import com.sims.model.Attendance;
import com.sims.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code ATTENDANCE} table.
 *
 * <h2>Design Pattern - Structural: DAO Pattern</h2>
 * <p><b>Academic Justification</b>: Attendance SQL is isolated from Swing
 * screens and controller logic. The controller submits and receives
 * {@link Attendance} POJOs, while this DAO owns all JDBC statements,
 * preserving the MVC boundary required for the SIMS lab evaluation.</p>
 *
 * <h2>Resource Management</h2>
 * <p>Every {@link PreparedStatement} and {@link ResultSet} is opened with
 * try-with-resources. DML methods intentionally do not commit; transaction
 * demarcation belongs to {@code AttendanceController}.</p>
 */
public class AttendanceDAO {

    private final Connection conn;

    private static final String SQL_MERGE_ATTENDANCE =
        "MERGE INTO ATTENDANCE a " +
        "USING (SELECT ? AS STUDENT_ID, ? AS SUBJECT, ? AS LOG_DATE FROM dual) src " +
        "ON (a.STUDENT_ID = src.STUDENT_ID AND a.SUBJECT = src.SUBJECT AND a.LOG_DATE = src.LOG_DATE) " +
        "WHEN MATCHED THEN UPDATE SET a.FACULTY_ID = ?, a.STATUS = ?, a.REMARKS = ? " +
        "WHEN NOT MATCHED THEN INSERT (STUDENT_ID, FACULTY_ID, SUBJECT, LOG_DATE, STATUS, REMARKS) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_STUDENT_SUBJECT =
        "SELECT a.LOG_ID, a.STUDENT_ID, a.FACULTY_ID, a.SUBJECT, a.LOG_DATE, " +
        "       a.STATUS, a.REMARKS, u.FULL_NAME " +
        "FROM   ATTENDANCE a " +
        "       JOIN STUDENT s ON a.STUDENT_ID = s.STUDENT_ID " +
        "       JOIN USERS u ON s.USER_ID = u.USER_ID " +
        "WHERE  a.STUDENT_ID = ? AND a.SUBJECT = ? " +
        "ORDER BY a.LOG_DATE DESC, a.LOG_ID DESC";

    private static final String SQL_FIND_BY_STUDENT =
        "SELECT a.LOG_ID, a.STUDENT_ID, a.FACULTY_ID, a.SUBJECT, a.LOG_DATE, " +
        "       a.STATUS, a.REMARKS, u.FULL_NAME " +
        "FROM   ATTENDANCE a " +
        "       JOIN STUDENT s ON a.STUDENT_ID = s.STUDENT_ID " +
        "       JOIN USERS u ON s.USER_ID = u.USER_ID " +
        "WHERE  a.STUDENT_ID = ? " +
        "ORDER BY a.LOG_DATE DESC, a.LOG_ID DESC";

    private static final String SQL_MONTHLY_SUMMARY =
        "SELECT SUM(CASE WHEN STATUS IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) AS PRESENT_COUNT, " +
        "       SUM(CASE WHEN STATUS = 'ABSENT' THEN 1 ELSE 0 END) AS ABSENT_COUNT, " +
        "       COUNT(*) AS TOTAL_COUNT " +
        "FROM   ATTENDANCE " +
        "WHERE  STUDENT_ID = ? AND SUBJECT = ? " +
        "AND    EXTRACT(MONTH FROM LOG_DATE) = ? " +
        "AND    EXTRACT(YEAR FROM LOG_DATE) = ?";

    public AttendanceDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public void insertOrUpdate(Attendance attendance) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_MERGE_ATTENDANCE)) {
            Date logDate = Date.valueOf(attendance.getLogDate());
            ps.setLong(1, attendance.getStudentId());
            ps.setString(2, attendance.getSubject());
            ps.setDate(3, logDate);
            ps.setLong(4, attendance.getFacultyId());
            ps.setString(5, attendance.getStatus());
            ps.setString(6, attendance.getRemarks());
            ps.setLong(7, attendance.getStudentId());
            ps.setLong(8, attendance.getFacultyId());
            ps.setString(9, attendance.getSubject());
            ps.setDate(10, logDate);
            ps.setString(11, attendance.getStatus());
            ps.setString(12, attendance.getRemarks());
            ps.executeUpdate();
        }
    }

    public List<Attendance> findByStudent(long studentId, String subject, Integer limit) throws SQLException {
        boolean filterSubject = subject != null
                && !subject.isBlank()
                && !"ALL".equalsIgnoreCase(subject.trim())
                && !"All Subjects".equalsIgnoreCase(subject.trim());
        String sql = filterSubject ? SQL_FIND_BY_STUDENT_SUBJECT : SQL_FIND_BY_STUDENT;

        List<Attendance> attendanceList = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (limit != null && limit > 0) {
                ps.setMaxRows(limit);
            }
            ps.setLong(1, studentId);
            if (filterSubject) {
                ps.setString(2, subject.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attendanceList.add(mapRow(rs));
                    if (limit != null && limit > 0 && attendanceList.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return attendanceList;
    }

    public List<Attendance> findByStudentAndSubject(long studentId, String subject) throws SQLException {
        return findByStudent(studentId, subject, null);
    }

    public int[] getMonthlySummary(long studentId, String subject, int month, int year) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_MONTHLY_SUMMARY)) {
            ps.setLong(1, studentId);
            ps.setString(2, subject);
            ps.setInt(3, month);
            ps.setInt(4, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[] {
                        rs.getInt("PRESENT_COUNT"),
                        rs.getInt("ABSENT_COUNT"),
                        rs.getInt("TOTAL_COUNT")
                    };
                }
            }
        }
        return new int[] {0, 0, 0};
    }

    private Attendance mapRow(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setLogId(rs.getLong("LOG_ID"));
        attendance.setStudentId(rs.getLong("STUDENT_ID"));
        attendance.setFacultyId(rs.getLong("FACULTY_ID"));
        attendance.setSubject(rs.getString("SUBJECT"));
        Date logDate = rs.getDate("LOG_DATE");
        if (logDate != null) {
            attendance.setLogDate(logDate.toLocalDate());
        }
        attendance.setStatus(rs.getString("STATUS"));
        attendance.setRemarks(rs.getString("REMARKS"));
        attendance.setStudentName(rs.getString("FULL_NAME"));
        return attendance;
    }
}
