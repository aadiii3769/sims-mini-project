package com.sims.model;

/**
 * POJO mapping to the {@code MARKS} database table.
 *
 * <p>{@code totalMarks} and {@code letterGrade} are transient computed fields —
 * they are NOT persisted (see 3NF justification in docs/DATABASE.md).
 */
public class Mark {

    private long   recordId;
    private long   studentId;
    private long   facultyId;
    private String subject;
    private int    semester;
    private double cat1Marks;
    private double cat2Marks;
    private double cat3Marks;
    private double gradePoint;
    private String academicYear;

    // ── Transient computed fields (not stored in DB) ──────────────────────
    private double totalMarks;   // = cat1 + cat2 + cat3, computed by controller
    private String letterGrade;  // e.g. "A+", "B", computed by controller

    // Joined field
    private String studentName;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Mark() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getRecordId()                       { return recordId; }
    public void setRecordId(long recordId)          { this.recordId = recordId; }

    public long getStudentId()                      { return studentId; }
    public void setStudentId(long studentId)        { this.studentId = studentId; }

    public long getFacultyId()                      { return facultyId; }
    public void setFacultyId(long facultyId)        { this.facultyId = facultyId; }

    public String getSubject()                      { return subject; }
    public void setSubject(String subject)          { this.subject = subject; }

    public int getSemester()                        { return semester; }
    public void setSemester(int semester)           { this.semester = semester; }

    public double getCat1Marks()                    { return cat1Marks; }
    public void setCat1Marks(double cat1Marks)      { this.cat1Marks = cat1Marks; }

    public double getCat2Marks()                    { return cat2Marks; }
    public void setCat2Marks(double cat2Marks)      { this.cat2Marks = cat2Marks; }

    public double getCat3Marks()                    { return cat3Marks; }
    public void setCat3Marks(double cat3Marks)      { this.cat3Marks = cat3Marks; }

    public double getGradePoint()                   { return gradePoint; }
    public void setGradePoint(double gradePoint)    { this.gradePoint = gradePoint; }

    public String getAcademicYear()                 { return academicYear; }
    public void setAcademicYear(String year)        { this.academicYear = year; }

    public double getTotalMarks()                   { return totalMarks; }
    public void setTotalMarks(double totalMarks)    { this.totalMarks = totalMarks; }

    public String getLetterGrade()                  { return letterGrade; }
    public void setLetterGrade(String grade)        { this.letterGrade = grade; }

    public String getStudentName()                  { return studentName; }
    public void setStudentName(String name)         { this.studentName = name; }
}
