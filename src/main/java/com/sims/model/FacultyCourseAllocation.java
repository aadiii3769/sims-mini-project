package com.sims.model;

/**
 * POJO mapping to the {@code FACULTY_COURSE_ALLOCATION} database table.
 */
public class FacultyCourseAllocation {

    private long   allocationId;
    private long   facultyId;
    private long   courseId;
    private String academicYear;

    // Joined fields
    private String courseName;
    private String courseCode;
    private int    semester;
    private Long   deptId;
    private String facultyName;

    public FacultyCourseAllocation() {}

    public long getAllocationId()               { return allocationId; }
    public void setAllocationId(long id)        { this.allocationId = id; }

    public long getFacultyId()                  { return facultyId; }
    public void setFacultyId(long facultyId)    { this.facultyId = facultyId; }

    public long getCourseId()                   { return courseId; }
    public void setCourseId(long courseId)      { this.courseId = courseId; }

    public String getAcademicYear()             { return academicYear; }
    public void setAcademicYear(String year)    { this.academicYear = year; }

    public String getCourseName()               { return courseName; }
    public void setCourseName(String name)      { this.courseName = name; }

    public String getCourseCode()               { return courseCode; }
    public void setCourseCode(String code)      { this.courseCode = code; }

    public int getSemester()                    { return semester; }
    public void setSemester(int sem)            { this.semester = sem; }

    public Long getDeptId()                     { return deptId; }
    public void setDeptId(Long deptId)          { this.deptId = deptId; }

    public String getFacultyName()              { return facultyName; }
    public void setFacultyName(String name)     { this.facultyName = name; }
}
