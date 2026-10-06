package com.sims.model;

import java.time.LocalDate;

/**
 * POJO mapping to the {@code ATTENDANCE} database table.
 */
public class Attendance {

    private long      logId;
    private long      studentId;
    private long      facultyId;
    private String    subject;
    private LocalDate logDate;
    private String    status;     // PRESENT | ABSENT | OD | MEDICAL
    private String    remarks;

    // Joined field
    private String    studentName;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Attendance() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getLogId()                        { return logId; }
    public void setLogId(long logId)              { this.logId = logId; }

    public long getStudentId()                    { return studentId; }
    public void setStudentId(long studentId)      { this.studentId = studentId; }

    public long getFacultyId()                    { return facultyId; }
    public void setFacultyId(long facultyId)      { this.facultyId = facultyId; }

    public String getSubject()                    { return subject; }
    public void setSubject(String subject)        { this.subject = subject; }

    public LocalDate getLogDate()                 { return logDate; }
    public void setLogDate(LocalDate logDate)     { this.logDate = logDate; }

    public String getStatus()                     { return status; }
    public void setStatus(String status)          { this.status = status; }

    public String getRemarks()                    { return remarks; }
    public void setRemarks(String remarks)        { this.remarks = remarks; }

    public String getStudentName()                { return studentName; }
    public void setStudentName(String name)       { this.studentName = name; }
}
