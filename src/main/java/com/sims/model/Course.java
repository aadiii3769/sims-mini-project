package com.sims.model;

/**
 * POJO mapping to the {@code COURSE} database table.
 *
 * <p>Semester 1 foundation courses have a {@code null} {@code deptId},
 * indicating commonality across all departments. Semesters 2 to 8 are
 * department-specific.</p>
 */
public class Course {

    private long   courseId;
    private String courseName;
    private String courseCode;
    private int    credits;
    private int    semester;
    private Long   deptId;        // nullable (null = common foundation course)

    // Joined fields
    private String deptName;
    private String deptCode;

    public Course() {}

    public Course(long courseId, String courseName, String courseCode, int credits, int semester, Long deptId) {
        this.courseId   = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.credits    = credits;
        this.semester   = semester;
        this.deptId     = deptId;
    }

    public long getCourseId()                   { return courseId; }
    public void setCourseId(long courseId)      { this.courseId = courseId; }

    public String getCourseName()               { return courseName; }
    public void setCourseName(String name)      { this.courseName = name; }

    public String getCourseCode()               { return courseCode; }
    public void setCourseCode(String code)      { this.courseCode = code; }

    public int getCredits()                     { return credits; }
    public void setCredits(int credits)         { this.credits = credits; }

    public int getSemester()                    { return semester; }
    public void setSemester(int semester)       { this.semester = semester; }

    public Long getDeptId()                     { return deptId; }
    public void setDeptId(Long deptId)          { this.deptId = deptId; }

    public String getDeptName()                 { return deptName; }
    public void setDeptName(String deptName)    { this.deptName = deptName; }

    public String getDeptCode()                 { return deptCode; }
    public void setDeptCode(String deptCode)    { this.deptCode = deptCode; }

    @Override
    public String toString() {
        return courseCode + " - " + courseName;
    }
}
