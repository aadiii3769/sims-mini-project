package com.sims.model;

import java.time.LocalDate;

/**
 * POJO mapping to the {@code STUDENT} database table.
 *
 * <p>Contains only data fields — no SQL, no Swing. The linked {@link User}
 * object carries authentication/contact details; this object carries the
 * academic enrolment record.
 */
public class Student {

    private long      studentId;
    private long      userId;          // FK → USERS.USER_ID
    private String    rollNumber;
    private String    department;
    private Long      deptId;          // FK → DEPARTMENT.DEPT_ID
    private int       year;
    private String    section;
    private LocalDate dateOfBirth;
    private String    address;
    private Long      parentUserId;   // nullable FK → USERS.USER_ID (role=PARENT)

    // Joined fields — populated by DAO queries with JOIN
    private String    fullName;       // from USERS.FULL_NAME
    private String    email;          // from USERS.EMAIL

    // ── Constructors ─────────────────────────────────────────────────────────

    public Student() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getStudentId()                      { return studentId; }
    public void setStudentId(long studentId)        { this.studentId = studentId; }

    public long getUserId()                         { return userId; }
    public void setUserId(long userId)              { this.userId = userId; }

    public String getRollNumber()                   { return rollNumber; }
    public void setRollNumber(String rollNumber)    { this.rollNumber = rollNumber; }

    public String getDepartment()                   { return department; }
    public void setDepartment(String department)    { this.department = department; }

    public Long getDeptId()                         { return deptId; }
    public void setDeptId(Long deptId)              { this.deptId = deptId; }

    public int getYear()                            { return year; }
    public void setYear(int year)                   { this.year = year; }

    /** Returns current active odd semester (Year 1 -> Sem 1, Year 2 -> Sem 3, etc.) */
    public int getCurrentSemester()                 { return Math.max(1, (year * 2) - 1); }

    public String getSection()                      { return section; }
    public void setSection(String section)          { this.section = section; }

    public LocalDate getDateOfBirth()               { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dob)       { this.dateOfBirth = dob; }

    public String getAddress()                      { return address; }
    public void setAddress(String address)          { this.address = address; }

    public Long getParentUserId()                   { return parentUserId; }
    public void setParentUserId(Long parentUserId)  { this.parentUserId = parentUserId; }

    public String getFullName()                     { return fullName; }
    public void setFullName(String fullName)        { this.fullName = fullName; }

    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }

    @Override
    public String toString() {
        return "Student{id=" + studentId + ", roll='" + rollNumber +
               "', dept='" + department + "', year=" + year + '}';
    }
}
