package com.sims.controller;

import com.sims.dao.AttendanceDAO;
import com.sims.dao.StudentDAO;
import com.sims.model.Attendance;
import com.sims.model.Student;
import com.sims.model.User;
import com.sims.model.UserRole;
import com.sims.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic and transaction controller for attendance logging and summary
 * reporting.
 *
 * <h2>Design Pattern - Behavioral: Observer (indirect)</h2>
 * <p><b>Academic Justification</b>: Swing {@code ActionListener} observers in
 * the View layer call this controller when users save attendance or refresh
 * reports. This class validates input, coordinates DAO calls, and performs
 * transaction demarcation while keeping SQL out of the UI.</p>
 */
public class AttendanceController {

    public static final String[] SUBJECTS = {
        "Object-Oriented Programming (CS3391)",
        "Database Technology (IT3301)",
        "Computer Networks (CS3392)"
    };

    private final AttendanceDAO attendanceDAO;
    private final StudentDAO studentDAO;
    private final Connection conn;

    public AttendanceController() {
        this.conn = DBConnection.getInstance().getConnection();
        this.attendanceDAO = new AttendanceDAO();
        this.studentDAO = new StudentDAO();
    }

    public void markAttendance(List<Attendance> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("No attendance records selected.");
        }

        try {
            for (Attendance record : records) {
                validateAttendance(record);
                attendanceDAO.insertOrUpdate(record);
            }
            conn.commit();
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly();
            throw new RuntimeException("Attendance save failed: " + e.getMessage(), e);
        }
    }

    public List<Attendance> getAttendanceHistory(long studentId, String subject) {
        return getAttendanceHistory(studentId, subject, null);
    }

    public List<Attendance> getAttendanceHistory(long studentId, String subject, Integer limit) {
        try {
            return attendanceDAO.findByStudent(studentId, subject, limit);
        } catch (SQLException e) {
            throw new RuntimeException("Attendance history lookup failed: " + e.getMessage(), e);
        }
    }

    public MonthlySummary getMonthlySummary(long studentId, String subject, int month, int year) {
        try {
            int[] counts = attendanceDAO.getMonthlySummary(studentId, subject, month, year);
            return new MonthlySummary(
                subject,
                counts[0],
                counts[1],
                counts[2],
                computeAttendancePercentage(counts[0], counts[2])
            );
        } catch (SQLException e) {
            throw new RuntimeException("Attendance summary lookup failed: " + e.getMessage(), e);
        }
    }

    public double computeAttendancePercentage(int present, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return (present * 100.0) / total;
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

    private void validateAttendance(Attendance record) {
        if (record == null) {
            throw new IllegalArgumentException("Attendance record is required.");
        }
        if (record.getStudentId() <= 0) {
            throw new IllegalArgumentException("Student is required.");
        }
        if (record.getFacultyId() <= 0) {
            throw new IllegalArgumentException("Faculty is required.");
        }
        if (record.getSubject() == null || record.getSubject().isBlank()) {
            throw new IllegalArgumentException("Subject is required.");
        }
        if (record.getLogDate() == null || record.getLogDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Attendance date must not be in the future.");
        }
        if (!List.of("PRESENT", "ABSENT", "OD", "MEDICAL").contains(record.getStatus())) {
            throw new IllegalArgumentException("Invalid attendance status.");
        }
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            System.err.println("[WARN] Attendance rollback failed: " + rollbackError.getMessage());
        }
    }

    public static final class MonthlySummary {
        private final String subject;
        private final int present;
        private final int absent;
        private final int total;
        private final double percentage;

        public MonthlySummary(String subject, int present, int absent, int total, double percentage) {
            this.subject = subject;
            this.present = present;
            this.absent = absent;
            this.total = total;
            this.percentage = percentage;
        }

        public String getSubject() {
            return subject;
        }

        public int getPresent() {
            return present;
        }

        public int getAbsent() {
            return absent;
        }

        public int getTotal() {
            return total;
        }

        public double getPercentage() {
            return percentage;
        }
    }
}
