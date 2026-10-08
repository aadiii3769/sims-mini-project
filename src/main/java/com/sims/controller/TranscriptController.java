package com.sims.controller;

import com.sims.dao.MarksDAO;
import com.sims.dao.StudentDAO;
import com.sims.model.Mark;
import com.sims.model.Student;
import com.sims.model.User;
import com.sims.model.UserRole;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Business logic controller for academic transcript generation and archival.
 *
 * <h2>Design Pattern – Behavioral: Observer (indirect)</h2>
 * <p><b>Academic Justification</b>: Swing {@code ActionListener} observers in
 * {@code TranscriptPanel} and {@code TranscriptAdminPanel} invoke this controller
 * when students/admins request transcript data or text exports. All DB access,
 * grouping, and GPA arithmetic are isolated here — the View layer receives only
 * clean {@link SemesterSummary} value objects, maintaining the View → Controller
 * → DAO dependency direction mandated by AGENTS.md §3.</p>
 *
 * <h2>No Transaction Demarcation Needed</h2>
 * <p>This controller only issues read-only queries; no {@code commit()} or
 * {@code rollback()} is required.</p>
 */
public class TranscriptController {

    private final MarksDAO   marksDAO;
    private final StudentDAO studentDAO;

    // ── Reuse grade derivation from MarksController ──────────────────────────

    private static final MarksController marksController = new MarksController();

    public TranscriptController() {
        this.marksDAO   = new MarksDAO();
        this.studentDAO = new StudentDAO();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads all marks for a student and groups them by semester in ascending
     * order. Each semester entry carries the list of marks plus computed GPA.
     *
     * @param studentId the target student's primary key
     * @return ordered map: semester number → {@link SemesterSummary}
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public Map<Integer, SemesterSummary> generateTranscript(long studentId) {
        try {
            List<Mark> all = marksDAO.findAllByStudent(studentId);
            all.forEach(this::populateDerived);

            // Group by semester — TreeMap keeps ascending key order
            Map<Integer, List<Mark>> bySem = new TreeMap<>();
            for (Mark m : all) {
                bySem.computeIfAbsent(m.getSemester(), k -> new ArrayList<>()).add(m);
            }

            Map<Integer, SemesterSummary> result = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<Mark>> entry : bySem.entrySet()) {
                int sem = entry.getKey();
                List<Mark> marks = entry.getValue();
                double gpa = computeSemesterGPA(marks);
                int passCount = (int) marks.stream().filter(m -> m.getGradePoint() > 0).count();
                int failCount = marks.size() - passCount;
                result.put(sem, new SemesterSummary(sem, marks, gpa, passCount, failCount));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate transcript: " + e.getMessage(), e);
        }
    }

    /**
     * Computes the CGPA (arithmetic mean of all grade points) for a student
     * across all semesters. Rounded to two decimal places.
     *
     * @param studentId the target student's primary key
     * @return CGPA value (0.0 if no records exist)
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public double computeCGPA(long studentId) {
        try {
            double raw = marksDAO.computeCGPA(studentId);
            return Math.round(raw * 100.0) / 100.0;
        } catch (SQLException e) {
            throw new RuntimeException("CGPA computation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves the student profile for the given viewer account.
     * STUDENT role → linked student; PARENT role → child student.
     *
     * @param viewer currently logged-in user
     * @return {@code Optional<Student>} — empty if not found or wrong role
     * @throws RuntimeException wrapping any {@link SQLException}
     */
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

    /**
     * Loads all students (used by the admin panel to populate the student list).
     *
     * @return list of all students ordered by roll number
     * @throws RuntimeException wrapping any {@link SQLException}
     */
    public List<Student> findAllStudents() {
        try {
            return studentDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load student list: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a formatted text transcript suitable for saving to a {@code .txt}
     * file or printing via {@code JTextArea}. The format mirrors a typical
     * university academic transcript layout.
     *
     * @param student   the student whose transcript is to be generated
     * @param transcript the semester-grouped data from {@link #generateTranscript}
     * @param cgpa       the student's cumulative GPA
     * @return formatted multi-line String
     */
    public String exportTranscriptText(Student student,
                                       Map<Integer, SemesterSummary> transcript,
                                       double cgpa) {
        StringBuilder sb = new StringBuilder();
        String line80 = "=".repeat(80);
        String line80d = "-".repeat(80);

        sb.append(line80).append("\n");
        sb.append(centre("STUDENT INFORMATION MANAGEMENT SYSTEM", 80)).append("\n");
        sb.append(centre("ACADEMIC TRANSCRIPT", 80)).append("\n");
        sb.append(line80).append("\n\n");

        // Student details block
        sb.append(String.format("%-20s: %s%n", "Student Name",  student.getFullName()));
        sb.append(String.format("%-20s: %s%n", "Roll Number",   student.getRollNumber()));
        sb.append(String.format("%-20s: %s%n", "Department",    student.getDepartment()));
        sb.append(String.format("%-20s: Year %d  |  Section %s%n",
                "Class", student.getYear(), student.getSection()));
        sb.append(String.format("%-20s: %s%n", "Generated On",
                LocalDate.now().toString()));
        sb.append("\n");

        if (transcript.isEmpty()) {
            sb.append("No academic records found.\n");
        } else {
            for (SemesterSummary sem : transcript.values()) {
                sb.append(line80d).append("\n");
                sb.append(String.format("  SEMESTER %d%n", sem.semester()));
                sb.append(line80d).append("\n");

                // Column headers
                sb.append(String.format("  %-35s %6s %6s %6s %7s %5s %6s%n",
                        "Subject", "CAT1", "CAT2", "CAT3", "Total", "Grade", "GP"));
                sb.append("  " + "-".repeat(77)).append("\n");

                for (Mark m : sem.marks()) {
                    sb.append(String.format("  %-35s %6.1f %6.1f %6.1f %7.1f %5s %6.1f%n",
                            truncate(m.getSubject(), 35),
                            m.getCat1Marks(), m.getCat2Marks(), m.getCat3Marks(),
                            m.getTotalMarks(), m.getLetterGrade(), m.getGradePoint()));
                }

                sb.append("  " + "-".repeat(77)).append("\n");
                sb.append(String.format(
                        "  Subjects: %-3d  Pass: %-3d  Fail: %-3d  Semester GPA: %.2f%n",
                        sem.marks().size(), sem.passCount(), sem.failCount(), sem.gpa()));
                sb.append("\n");
            }
        }

        sb.append(line80).append("\n");
        sb.append(String.format("  CUMULATIVE GPA (CGPA)  :  %.2f / 10.00%n", cgpa));
        sb.append(line80).append("\n");
        sb.append("\n* Grades follow Anna University UG 10-point scale.\n");
        sb.append("* O=10, A+=9, A=8, B+=7, B=6, F=0 (Fail)\n");

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Fills in transient totalMarks and letterGrade on a Mark read from DB. */
    private void populateDerived(Mark m) {
        double total = m.getCat1Marks() + m.getCat2Marks() + m.getCat3Marks();
        m.setTotalMarks(total);
        m.setLetterGrade(marksController.getLetterGrade(m.getGradePoint()));
    }

    /** Computes arithmetic mean of grade points in a semester. */
    private double computeSemesterGPA(List<Mark> marks) {
        if (marks == null || marks.isEmpty()) return 0.0;
        double sum = marks.stream().mapToDouble(Mark::getGradePoint).sum();
        return Math.round((sum / marks.size()) * 100.0) / 100.0;
    }

    /** Centers a string within a fixed width using space padding. */
    private static String centre(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    /** Truncates a string to max {@code len} characters (adds "…" if cut). */
    private static String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len - 1) + "…";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Value objects
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Immutable snapshot of one semester's academic performance.
     *
     * @param semester  semester number (1–8)
     * @param marks     list of marks for this semester (with derived fields)
     * @param gpa       semester GPA rounded to 2 dp
     * @param passCount number of subjects passed (grade point > 0)
     * @param failCount number of subjects failed  (grade point = 0)
     */
    public record SemesterSummary(
            int semester,
            List<Mark> marks,
            double gpa,
            int passCount,
            int failCount
    ) {}
}
