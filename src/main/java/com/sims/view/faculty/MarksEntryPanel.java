package com.sims.view.faculty;

import com.sims.controller.MarksController;
import com.sims.model.Course;
import com.sims.model.Faculty;
import com.sims.model.Mark;
import com.sims.model.Student;
import com.sims.model.User;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Faculty panel for logging continuous assessment marks (CAT 1, CAT 2, Assignments/Quizzes),
 * filtering students by department curriculum and allocated courses.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: Interactive controls (semester selector,
 * course selector, student selection, CAT spinners, and the Save button) publish
 * Swing events observed by {@code ActionListener} / {@code ChangeListener} callbacks.
 * Those observers collect form state and delegate persistence to {@link MarksController},
 * strictly maintaining the View → Controller → DAO dependency boundary.</p>
 *
 * <h2>Layout</h2>
 * <p>Uses nested {@code BorderLayout} and {@code GridBagLayout} panels only —
 * no null or absolute positioning per AGENTS.md §6.</p>
 */
public class MarksEntryPanel extends JPanel {

    // ── Table columns ─────────────────────────────────────────────────────────
    private static final String[] STUDENT_COLS = {"Roll No", "Student Name", "Department", "Year"};
    private static final String[] MARKS_COLS   = {
        "Roll No", "Student Name", "CAT 1", "CAT 2", "Assignment", "Total", "Grade", "Grade Pt", "Status"
    };

    // ── Controllers & State ───────────────────────────────────────────────────
    private final JFrame          parentFrame;
    private final User            facultyUser;
    private final MarksController marksController;

    private Faculty               facultyProfile;
    private List<Student>         currentStudents = new ArrayList<>();
    private Student               selectedStudent;
    private Course                selectedCourse;
    private Mark                  existingMark;

    // ── Form controls ─────────────────────────────────────────────────────────
    private final JComboBox<Integer> semesterCombo;
    private final JComboBox<Course>  courseCombo;
    private final JTextField         academicYearField;
    private final JSpinner           cat1Spinner;
    private final JSpinner           cat2Spinner;
    private final JSpinner           assignmentSpinner;
    private final JLabel             totalPreviewLabel;
    private final JLabel             facultyInfoLabel;
    private final JLabel             courseInfoLabel;

    // ── Tables ────────────────────────────────────────────────────────────────
    private final DefaultTableModel  studentModel;
    private final JTable             studentTable;
    private final DefaultTableModel  marksModel;
    private final JTable             marksTable;
    private final JLabel             statusLabel;

    public MarksEntryPanel(JFrame parentFrame, User facultyUser) {
        super(new BorderLayout(0, 0));
        this.parentFrame       = parentFrame;
        this.facultyUser       = facultyUser;
        this.marksController   = new MarksController();

        this.semesterCombo     = buildSemesterCombo();
        this.courseCombo       = new JComboBox<>();
        this.academicYearField = new JTextField(LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() % 100 + 1), 7);
        this.cat1Spinner       = buildMarkSpinner(50.0);
        this.cat2Spinner       = buildMarkSpinner(50.0);
        this.assignmentSpinner = buildMarkSpinner(20.0);
        this.totalPreviewLabel = new JLabel("Total: 0.0  |  Grade: –  |  Grade Point: –");
        this.facultyInfoLabel  = new JLabel("Faculty: " + facultyUser.getFullName());
        this.courseInfoLabel   = new JLabel("Select course to begin assessment entry.");

        this.studentModel      = buildStudentModel();
        this.studentTable      = new JTable(studentModel);
        this.marksModel        = buildMarksModel();
        this.marksTable        = new JTable(marksModel);
        this.statusLabel       = new JLabel("Initializing faculty workspace...");

        wireListeners();
        buildUI();
        initFacultyProfile();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        add(buildLeftPanel(),  BorderLayout.WEST);
        add(buildRightPanel(), BorderLayout.CENTER);
        add(buildStatusBar(),  BorderLayout.SOUTH);
    }

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        panel.setPreferredSize(new Dimension(340, 0));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel("Enrolled Students");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        facultyInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        facultyInfoLabel.setForeground(new Color(60, 90, 140));
        header.add(title);
        header.add(facultyInfoLabel);

        studentTable.setRowHeight(24);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        panel.add(buildEntryForm(),  BorderLayout.NORTH);
        panel.add(buildMarksGrid(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildEntryForm() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        TitledBorder border = BorderFactory.createTitledBorder("CAT & Internal Assessment Entry");
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        wrapper.setBorder(border);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 6, 4, 6);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Semester selector
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        form.add(new JLabel("Target Semester:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(semesterCombo, gbc);

        // Course selector
        gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
        form.add(new JLabel("Department Course:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(courseCombo, gbc);

        // Academic Year
        gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
        form.add(new JLabel("Academic Year:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(academicYearField, gbc);

        // Marks entry row (CAT 1, CAT 2, Assignment)
        gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
        form.add(new JLabel("Assessments:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;

        JPanel marksRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        marksRow.add(new JLabel("CAT 1 (0–50):"));
        marksRow.add(cat1Spinner);
        marksRow.add(new JLabel("CAT 2 (0–50):"));
        marksRow.add(cat2Spinner);
        marksRow.add(new JLabel("Assignment (0–20):"));
        marksRow.add(assignmentSpinner);
        form.add(marksRow, gbc);

        // Live calculation preview
        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2;
        totalPreviewLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        totalPreviewLabel.setForeground(new Color(30, 80, 150));
        form.add(totalPreviewLabel, gbc);

        // Save button
        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2;
        JButton saveBtn = new JButton("Save / Update Assessment Marks");
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        saveBtn.setBackground(new Color(230, 242, 255));
        saveBtn.addActionListener(this::handleSave);
        form.add(saveBtn, gbc);

        wrapper.add(form, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildMarksGrid() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        marksTable.setRowHeight(24);
        marksTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        marksTable.setDefaultRenderer(Object.class, new MarksGridRenderer());

        courseInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(courseInfoLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(marksTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(240, 242, 245));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        bar.add(statusLabel);
        return bar;
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void wireListeners() {
        // Semester change → reload department & allocated courses
        semesterCombo.addActionListener(e -> onSemesterChanged());

        // Course change → reload students & existing course marks
        courseCombo.addActionListener(e -> onCourseChanged());

        // Student selection → pre-fill assessment values
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onStudentSelected();
            }
        });

        // Table row click in marks table → pre-fill form
        marksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onMarksRowSelected();
            }
        });

        // Live calculation preview on spinner changes
        cat1Spinner.addChangeListener(e -> updatePreview());
        cat2Spinner.addChangeListener(e -> updatePreview());
        assignmentSpinner.addChangeListener(e -> updatePreview());
    }

    // ── Initialization & Data Loading ─────────────────────────────────────────

    private void initFacultyProfile() {
        new SwingWorker<Optional<Faculty>, Void>() {
            @Override
            protected Optional<Faculty> doInBackground() {
                return marksController.findFacultyProfileByUserId(facultyUser.getUserId());
            }
            @Override
            protected void done() {
                try {
                    Optional<Faculty> opt = get();
                    if (opt.isPresent()) {
                        facultyProfile = opt.get();
                        facultyInfoLabel.setText("Faculty: " + facultyProfile.getFullName()
                            + " | Dept: " + facultyProfile.getDeptCode());
                    } else {
                        facultyInfoLabel.setText("Faculty: " + facultyUser.getFullName());
                    }
                    onSemesterChanged();
                } catch (Exception ex) {
                    statusLabel.setText("Error initializing faculty profile.");
                }
            }
        }.execute();
    }

    private void onSemesterChanged() {
        int semester = (Integer) semesterCombo.getSelectedItem();
        statusLabel.setText("Loading courses for Semester " + semester + "...");

        new SwingWorker<List<Course>, Void>() {
            @Override
            protected List<Course> doInBackground() {
                List<Course> list = new ArrayList<>();
                // If faculty profile exists, first check their allocated courses for this semester
                if (facultyProfile != null) {
                    list = marksController.getFacultyAllocatedCoursesBySemester(
                            facultyProfile.getFacultyId(), semester);
                }
                // If no allocated courses, load all courses for their department (or common Sem 1 courses)
                if (list.isEmpty()) {
                    Long deptId = (facultyProfile != null) ? facultyProfile.getDeptId() : null;
                    list = marksController.getCoursesByDeptAndSemester(deptId, semester);
                }
                return list;
            }
            @Override
            protected void done() {
                try {
                    List<Course> courses = get();
                    courseCombo.removeAllItems();
                    for (Course c : courses) {
                        courseCombo.addItem(c);
                    }
                    if (courses.isEmpty()) {
                        courseInfoLabel.setText("No courses available for Semester " + semester);
                        studentModel.setRowCount(0);
                        marksModel.setRowCount(0);
                        statusLabel.setText("No courses mapped for Semester " + semester);
                    } else {
                        courseCombo.setSelectedIndex(0);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error loading courses for semester.");
                }
            }
        }.execute();
    }

    private void onCourseChanged() {
        selectedCourse = (Course) courseCombo.getSelectedItem();
        if (selectedCourse == null) return;

        int semester = (Integer) semesterCombo.getSelectedItem();
        courseInfoLabel.setText("Course: " + selectedCourse.getCourseCode() + " - "
            + selectedCourse.getCourseName() + " (" + selectedCourse.getCredits() + " Credits)");

        loadEligibleStudents(selectedCourse, semester);
        loadCourseMarks(selectedCourse, semester);
    }

    private void loadEligibleStudents(Course course, int semester) {
        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() {
                return marksController.getStudentsForCourseEntry(course.getDeptId(), semester);
            }
            @Override
            protected void done() {
                try {
                    currentStudents = get();
                    studentModel.setRowCount(0);
                    for (Student s : currentStudents) {
                        studentModel.addRow(new Object[]{
                            s.getRollNumber(), s.getFullName(),
                            s.getDepartment(), "Year " + s.getYear()
                        });
                    }
                    statusLabel.setText(currentStudents.size() + " eligible student(s) loaded.");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading students.");
                }
            }
        }.execute();
    }

    private void loadCourseMarks(Course course, int semester) {
        new SwingWorker<List<Mark>, Void>() {
            @Override
            protected List<Mark> doInBackground() {
                return marksController.getMarksByCourseAndSemester(course.getCourseId(), semester);
            }
            @Override
            protected void done() {
                try {
                    List<Mark> marks = get();
                    marksModel.setRowCount(0);
                    for (Mark m : marks) {
                        marksModel.addRow(new Object[]{
                            m.getRollNumber(),
                            m.getStudentName(),
                            String.format("%.1f", m.getCat1Marks()),
                            String.format("%.1f", m.getCat2Marks()),
                            String.format("%.1f", m.getAssignmentMarks()),
                            String.format("%.1f", m.getTotalInternal()),
                            m.getSemesterGrade(),
                            String.format("%.1f", m.getGradePoint()),
                            m.isCompleted() ? "Completed" : "In Progress"
                        });
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error loading marks grid.");
                }
            }
        }.execute();
    }

    private void onStudentSelected() {
        int row = studentTable.getSelectedRow();
        if (row < 0 || currentStudents == null || row >= currentStudents.size()) {
            selectedStudent = null;
            return;
        }
        selectedStudent = currentStudents.get(row);
        statusLabel.setText("Selected Student: " + selectedStudent.getFullName() + " (" + selectedStudent.getRollNumber() + ")");

        // Check if student already has marks for this course & semester
        if (selectedCourse != null) {
            int semester = (Integer) semesterCombo.getSelectedItem();
            new SwingWorker<Optional<Mark>, Void>() {
                @Override
                protected Optional<Mark> doInBackground() {
                    return marksController.findExistingMark(
                            selectedStudent.getStudentId(), selectedCourse.getCourseId(), semester);
                }
                @Override
                protected void done() {
                    try {
                        Optional<Mark> opt = get();
                        if (opt.isPresent()) {
                            existingMark = opt.get();
                            cat1Spinner.setValue(existingMark.getCat1Marks());
                            cat2Spinner.setValue(existingMark.getCat2Marks());
                            assignmentSpinner.setValue(existingMark.getAssignmentMarks());
                            if (existingMark.getAcademicYear() != null) {
                                academicYearField.setText(existingMark.getAcademicYear());
                            }
                        } else {
                            existingMark = null;
                            cat1Spinner.setValue(0.0);
                            cat2Spinner.setValue(0.0);
                            assignmentSpinner.setValue(0.0);
                        }
                        updatePreview();
                    } catch (Exception ignored) {}
                }
            }.execute();
        }
    }

    private void onMarksRowSelected() {
        int row = marksTable.getSelectedRow();
        if (row < 0 || currentStudents == null) return;
        String roll = (String) marksModel.getValueAt(row, 0);
        for (int i = 0; i < currentStudents.size(); i++) {
            if (currentStudents.get(i).getRollNumber().equals(roll)) {
                studentTable.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void updatePreview() {
        double cat1  = ((Number) cat1Spinner.getValue()).doubleValue();
        double cat2  = ((Number) cat2Spinner.getValue()).doubleValue();
        double assg  = ((Number) assignmentSpinner.getValue()).doubleValue();
        double total = cat1 + cat2 + assg;
        double gp    = marksController.deriveGradePoint(total);
        String grade = marksController.getLetterGrade(gp);
        totalPreviewLabel.setText(String.format(
            "Total Internal: %.1f / 120  |  Grade: %s  |  Grade Point: %.1f", total, grade, gp));
    }

    private void handleSave(ActionEvent e) {
        if (selectedStudent == null) {
            JOptionPane.showMessageDialog(parentFrame,
                "Please select a student from the list first.", "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedCourse == null) {
            JOptionPane.showMessageDialog(parentFrame,
                "Please select a target course.", "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int semester       = (Integer) semesterCombo.getSelectedItem();
        String academicYr  = academicYearField.getText().trim();
        double cat1        = ((Number) cat1Spinner.getValue()).doubleValue();
        double cat2        = ((Number) cat2Spinner.getValue()).doubleValue();
        double assg        = ((Number) assignmentSpinner.getValue()).doubleValue();

        Mark mark = (existingMark != null) ? existingMark : new Mark();
        mark.setStudentId(selectedStudent.getStudentId());
        mark.setCourseId(selectedCourse.getCourseId());
        mark.setFacultyId(facultyUser.getUserId());
        mark.setSemesterNo(semester);
        mark.setAcademicYear(academicYr.isEmpty() ? "2025-26" : academicYr);
        mark.setCat1Marks(cat1);
        mark.setCat2Marks(cat2);
        mark.setAssignmentMarks(assg);
        // Active semester mark is in progress (isCompleted = false)
        mark.setCompleted(false);

        try {
            marksController.saveMarks(mark);
            statusLabel.setText("Marks saved for " + selectedStudent.getFullName()
                + " — " + selectedCourse.getCourseCode());
            loadCourseMarks(selectedCourse, semester);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(parentFrame,
                "Save failed:\n" + ex.getMessage(), "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helper builders ───────────────────────────────────────────────────────

    private JComboBox<Integer> buildSemesterCombo() {
        JComboBox<Integer> combo = new JComboBox<>();
        for (int s = 1; s <= 8; s++) combo.addItem(s);
        combo.setSelectedItem(5); // Default to current active odd semester 5
        return combo;
    }

    private JSpinner buildMarkSpinner(double max) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, max, 0.5));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.0");
        spinner.setEditor(editor);
        return spinner;
    }

    private DefaultTableModel buildStudentModel() {
        return new DefaultTableModel(STUDENT_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private DefaultTableModel buildMarksModel() {
        return new DefaultTableModel(MARKS_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    // ── Grid Renderer ─────────────────────────────────────────────────────────

    private static class MarksGridRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (column >= 2) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            if (!isSelected) {
                Object gradeVal = table.getModel().getValueAt(row, 6);
                if ("F".equals(gradeVal)) {
                    c.setBackground(new Color(255, 230, 230));
                    c.setForeground(new Color(170, 20, 20));
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }
}
