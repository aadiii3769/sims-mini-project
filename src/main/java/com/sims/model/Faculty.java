package com.sims.model;

/**
 * POJO mapping to the {@code FACULTY} database table.
 */
public class Faculty {

    private long   facultyId;
    private long   userId;
    private long   deptId;
    private String designation;

    // Joined fields from USERS and DEPARTMENT
    private String fullName;
    private String email;
    private String phone;
    private String deptName;
    private String deptCode;

    public Faculty() {}

    public Faculty(long facultyId, long userId, long deptId, String designation) {
        this.facultyId   = facultyId;
        this.userId      = userId;
        this.deptId      = deptId;
        this.designation = designation;
    }

    public long getFacultyId()                  { return facultyId; }
    public void setFacultyId(long facultyId)    { this.facultyId = facultyId; }

    public long getUserId()                     { return userId; }
    public void setUserId(long userId)          { this.userId = userId; }

    public long getDeptId()                     { return deptId; }
    public void setDeptId(long deptId)          { this.deptId = deptId; }

    public String getDesignation()              { return designation; }
    public void setDesignation(String desig)    { this.designation = desig; }

    public String getFullName()                 { return fullName; }
    public void setFullName(String fullName)    { this.fullName = fullName; }

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }

    public String getPhone()                    { return phone; }
    public void setPhone(String phone)          { this.phone = phone; }

    public String getDeptName()                 { return deptName; }
    public void setDeptName(String deptName)    { this.deptName = deptName; }

    public String getDeptCode()                 { return deptCode; }
    public void setDeptCode(String deptCode)    { this.deptCode = deptCode; }

    @Override
    public String toString() {
        return (fullName != null ? fullName : "Faculty #" + facultyId) + " (" + designation + ")";
    }
}
