package com.sims.model;

/**
 * POJO mapping to the {@code MARKS} database table.
 *
 * <p>Supports continuous assessment marks (CAT 1, CAT 2, Assignments/Quizzes),
 * total internal marks, semester grade, grade point, and completion flag
 * (for completed prior transcripts vs active ongoing odd semester).</p>
 */
public class Mark {

    private long    recordId;
    private long    studentId;
    private long    courseId;
    private double  cat1Marks;
    private double  cat2Marks;
    private double  assignmentMarks;
    private double  totalInternal;
    private String  semesterGrade;
    private double  gradePoint;
    private int     semesterNo;
    private boolean isCompleted;
    private Long    facultyId;
    private String  academicYear;

    // ── Joined fields from COURSE, STUDENT, USERS ────────────────────────────
    private String  courseName;
    private String  courseCode;
    private int     credits;
    private String  studentName;
    private String  rollNumber;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Mark() {}

    public Mark(long studentId, long courseId, double cat1, double cat2, double assignment,
                double totalInternal, String grade, double gradePoint, int semesterNo, boolean completed) {
        this.studentId       = studentId;
        this.courseId        = courseId;
        this.cat1Marks       = cat1;
        this.cat2Marks       = cat2;
        this.assignmentMarks = assignment;
        this.totalInternal   = totalInternal;
        this.semesterGrade   = grade;
        this.gradePoint      = gradePoint;
        this.semesterNo      = semesterNo;
        this.isCompleted     = completed;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getRecordId()                           { return recordId; }
    public void setRecordId(long recordId)              { this.recordId = recordId; }

    public long getStudentId()                          { return studentId; }
    public void setStudentId(long studentId)            { this.studentId = studentId; }

    public long getCourseId()                           { return courseId; }
    public void setCourseId(long courseId)              { this.courseId = courseId; }

    public double getCat1Marks()                        { return cat1Marks; }
    public void setCat1Marks(double cat1Marks)          { this.cat1Marks = cat1Marks; }

    public double getCat2Marks()                        { return cat2Marks; }
    public void setCat2Marks(double cat2Marks)          { this.cat2Marks = cat2Marks; }

    public double getAssignmentMarks()                  { return assignmentMarks; }
    public void setAssignmentMarks(double marks)        { this.assignmentMarks = marks; }

    public double getTotalInternal()                    { return totalInternal; }
    public void setTotalInternal(double totalInternal)  { this.totalInternal = totalInternal; }

    public String getSemesterGrade()                    { return semesterGrade; }
    public void setSemesterGrade(String semesterGrade)  { this.semesterGrade = semesterGrade; }

    public double getGradePoint()                       { return gradePoint; }
    public void setGradePoint(double gradePoint)        { this.gradePoint = gradePoint; }

    public int getSemesterNo()                          { return semesterNo; }
    public void setSemesterNo(int semesterNo)           { this.semesterNo = semesterNo; }

    public boolean isCompleted()                        { return isCompleted; }
    public void setCompleted(boolean completed)         { isCompleted = completed; }

    public Long getFacultyId()                          { return facultyId; }
    public void setFacultyId(Long facultyId)            { this.facultyId = facultyId; }

    public String getAcademicYear()                     { return academicYear; }
    public void setAcademicYear(String academicYear)    { this.academicYear = academicYear; }

    public String getCourseName()                       { return courseName; }
    public void setCourseName(String courseName)        { this.courseName = courseName; }

    public String getCourseCode()                       { return courseCode; }
    public void setCourseCode(String courseCode)        { this.courseCode = courseCode; }

    public int getCredits()                             { return credits; }
    public void setCredits(int credits)                 { this.credits = credits; }

    public String getStudentName()                      { return studentName; }
    public void setStudentName(String studentName)      { this.studentName = studentName; }

    public String getRollNumber()                       { return rollNumber; }
    public void setRollNumber(String rollNumber)        { this.rollNumber = rollNumber; }

    // ── Backwards-compatibility aliases ──────────────────────────────────────

    public String getSubject() {
        if (courseName != null && !courseName.isBlank()) {
            return courseCode != null ? courseCode + " - " + courseName : courseName;
        }
        return courseCode != null ? courseCode : "Course #" + courseId;
    }
    public void setSubject(String subject)              { this.courseName = subject; }

    public int getSemester()                            { return semesterNo; }
    public void setSemester(int semester)               { this.semesterNo = semester; }

    public double getCat3Marks()                        { return assignmentMarks; }
    public void setCat3Marks(double marks)              { this.assignmentMarks = marks; }

    public double getTotalMarks()                       { return totalInternal; }
    public void setTotalMarks(double total)             { this.totalInternal = total; }

    public String getLetterGrade()                      { return semesterGrade; }
    public void setLetterGrade(String grade)            { this.semesterGrade = grade; }
}
