package com.sims.view.faculty;

import com.sims.controller.AttendanceController;
import com.sims.controller.MarksController;
import com.sims.controller.StudentController;
import com.sims.model.Mark;
import com.sims.model.Student;
import com.sims.model.User;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.List;

/**
 * Faculty panel for entering and updating student CAT marks.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: All interactive controls (student search,
 * semester selector, subject selector, CAT spinners, and the Save button)
 * publish Swing events observed by {@code ActionListener} /
 * {@code ChangeListener} callbacks.  Those observers collect form state and
 * delegate persistence to {@link MarksController}, keeping SQL out of the UI
 * and satisfying the MVC boundary required for the SIMS lab evaluation.</p>
 *
 * <h2>Layout</h2>
 * <p>Uses nested {@code BorderLayout} and {@code GridBagLayout} panels only —
 * no null/absolute layout, per AGENTS.md §6.</p>
 */
public class MarksEntryPanel extends JPanel {

    // ── Table columns ─────────────────────────────────────────────────────────
    private static final String[] STUDENT_COLS = {"Roll No", "Name", "Department", "Year"};
    private static final String[] MARKS_COLS   = {"Subject", "CAT1", "CAT2", "CAT3", "Total", "Grade", "GPA"};

    // ── Controllers ───────────────────────────────────────────────────────────
    private final JFrame          parentFrame;
    private final User            facultyUser;
    private final MarksController marksController;
    private final StudentController studentController;

    // ── Form controls ─────────────────────────────────────────────────────────
    private final JComboBox<Integer> semesterCombo;
    private final JComboBox<String>  subjectCombo;
    private final JTextField         academicYearField;
    private final JSpinner           cat1Spinner;
    private final JSpinner           cat2Spinner;
    private final JSpinner           cat3Spinner;
    private final JLabel             totalPreviewLabel;
    private final JLabel             gradePreviewLabel;

    // ── Tables ────────────────────────────────────────────────────────────────
    private final DefaultTableModel  studentModel;
    private final JTable             studentTable;
    private final DefaultTableModel  marksModel;
    private final JTable             marksTable;

    private final JLabel             statusLabel;
    private List<Student>            currentStudents;
    private Student                  selectedStudent;
    private Mark                     existingMark;   // non-null when editing

    public MarksEntryPanel(JFrame parentFrame, User facultyUser) {
        super(new BorderLayout(0, 0));
        this.parentFrame      = parentFrame;
        this.facultyUser      = facultyUser;
        this.marksController  = new MarksController();
        this.studentController = new StudentController();

        this.semesterCombo    = buildSemesterCombo();
        this.subjectCombo     = new JComboBox<>(AttendanceController.SUBJECTS);
        this.academicYearField = new JTextField(LocalDate.now().getYear() + "-" + (LocalDate.now().getYear() % 100 + 1), 7);
        this.cat1Spinner      = buildMarkSpinner();
        this.cat2Spinner      = buildMarkSpinner();
        this.cat3Spinner      = buildMarkSpinner();
        this.totalPreviewLabel = new JLabel("Total: 0.0  |  Grade: –");
        this.gradePreviewLabel = new JLabel("");
        this.studentModel     = buildStudentModel();
        this.studentTable     = new JTable(studentModel);
        this.marksModel       = buildMarksModel();
        this.marksTable       = new JTable(marksModel);
        this.statusLabel      = new JLabel("Select a student to enter marks.");

        wireListeners();
        buildUI();
        loadStudents();
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
        panel.setPreferredSize(new Dimension(320, 0));

        JLabel title = new JLabel("Student List");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));

        studentTable.setRowHeight(24);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        panel.add(buildEntryForm(),  BorderLayout.NORTH);
        panel.add(buildMarksTable(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildEntryForm() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        TitledBorder border = BorderFactory.createTitledBorder("Enter / Update Marks");
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        wrapper.setBorder(border);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 6, 4, 6);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Semester
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        form.add(new JLabel("Semester:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(semesterCombo, gbc);

        // Subject
        gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
        form.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(subjectCombo, gbc);

        // Academic Year
        gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
        form.add(new JLabel("Academic Year:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(academicYearField, gbc);

        // CAT1
        gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
        form.add(new JLabel("CAT 1 (0–50):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(cat1Spinner, gbc);

        // CAT2
        gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
        form.add(new JLabel("CAT 2 (0–50):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(cat2Spinner, gbc);

        // CAT3
        gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
        form.add(new JLabel("CAT 3 (0–50):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(cat3Spinner, gbc);

        // Total / Grade preview
        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2;
        totalPreviewLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        totalPreviewLabel.setForeground(new Color(30, 80, 150));
        form.add(totalPreviewLabel, gbc);

        // Save button
        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2;
        JButton saveBtn = new JButton("Save Marks");
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        saveBtn.addActionListener(this::handleSave);
        form.add(saveBtn, gbc);

        wrapper.add(form, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildMarksTable() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JLabel title = new JLabel("Existing Marks (Current Semester)");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        marksTable.setRowHeight(24);
        marksTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        marksTable.setDefaultRenderer(Object.class, new FailingRowRenderer());
        panel.add(title, BorderLayout.NORTH);
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
        // Student selection → load existing marks
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onStudentSelected();
            }
        });

        // Spinner changes → live total preview
        cat1Spinner.addChangeListener(e -> updatePreview());
        cat2Spinner.addChangeListener(e -> updatePreview());
        cat3Spinner.addChangeListener(e -> updatePreview());

        // Semester change → reload marks table
        semesterCombo.addActionListener(e -> refreshMarksTable());
    }

    private void onStudentSelected() {
        int row = studentTable.getSelectedRow();
        if (row < 0 || currentStudents == null || row >= currentStudents.size()) {
            selectedStudent = null;
            return;
        }
        selectedStudent = currentStudents.get(row);
        statusLabel.setText("Selected: " + selectedStudent.getFullName());
        refreshMarksTable();
    }

    private void updatePreview() {
        double cat1  = ((Number) cat1Spinner.getValue()).doubleValue();
        double cat2  = ((Number) cat2Spinner.getValue()).doubleValue();
        double cat3  = ((Number) cat3Spinner.getValue()).doubleValue();
        double total = cat1 + cat2 + cat3;
        double gp    = marksController.deriveGradePoint(total);
        String grade = marksController.getLetterGrade(gp);
        totalPreviewLabel.setText(String.format(
            "Total: %.1f / 150  |  Grade Point: %.1f  |  Letter: %s", total, gp, grade));
    }

    private void handleSave(ActionEvent e) {
        if (selectedStudent == null) {
            JOptionPane.showMessageDialog(parentFrame,
                "Please select a student from the list.", "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String subject     = (String) subjectCombo.getSelectedItem();
        int    semester    = (Integer) semesterCombo.getSelectedItem();
        String academicYr  = academicYearField.getText().trim();
        double cat1        = ((Number) cat1Spinner.getValue()).doubleValue();
        double cat2        = ((Number) cat2Spinner.getValue()).doubleValue();
        double cat3        = ((Number) cat3Spinner.getValue()).doubleValue();

        // Find existing record for this student/semester/subject
        existingMark = findExistingMark(subject, semester);

        Mark mark = (existingMark != null) ? existingMark : new Mark();
        mark.setStudentId(selectedStudent.getStudentId());
        mark.setFacultyId(facultyUser.getUserId());
        mark.setSubject(subject);
        mark.setSemester(semester);
        mark.setAcademicYear(academicYr);
        mark.setCat1Marks(cat1);
        mark.setCat2Marks(cat2);
        mark.setCat3Marks(cat3);

        try {
            marksController.saveMarks(mark);
            statusLabel.setText("Marks saved for " + selectedStudent.getFullName()
                + " — " + subject);
            refreshMarksTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(parentFrame,
                "Save failed:\n" + ex.getMessage(), "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Data loaders ──────────────────────────────────────────────────────────

    private void loadStudents() {
        statusLabel.setText("Loading students...");
        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() throws Exception {
                return studentController.searchStudents("");
            }
            @Override
            protected void done() {
                try {
                    currentStudents = get();
                    studentModel.setRowCount(0);
                    for (Student s : currentStudents) {
                        studentModel.addRow(new Object[]{
                            s.getRollNumber(), s.getFullName(),
                            s.getDepartment(), s.getYear()
                        });
                    }
                    statusLabel.setText(currentStudents.size() + " student(s) loaded.");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading students.");
                    JOptionPane.showMessageDialog(parentFrame,
                        "Failed to load students:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshMarksTable() {
        if (selectedStudent == null) return;
        int semester = (Integer) semesterCombo.getSelectedItem();

        new SwingWorker<List<Mark>, Void>() {
            @Override
            protected List<Mark> doInBackground() throws Exception {
                return marksController.getMarksBySemester(selectedStudent.getStudentId(), semester);
            }
            @Override
            protected void done() {
                try {
                    List<Mark> marks = get();
                    marksModel.setRowCount(0);
                    double semGPA = marksController.getSemesterGPA(selectedStudent.getStudentId(), semester);
                    for (Mark m : marks) {
                        marksModel.addRow(new Object[]{
                            m.getSubject(),
                            m.getCat1Marks(),
                            m.getCat2Marks(),
                            m.getCat3Marks(),
                            m.getTotalMarks(),
                            m.getLetterGrade(),
                            String.format("%.2f", m.getGradePoint())
                        });
                    }
                    statusLabel.setText("Semester " + semester + " GPA: "
                        + String.format("%.2f", semGPA)
                        + "  |  " + marks.size() + " subject(s)");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading marks.");
                }
            }
        }.execute();
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    private Mark findExistingMark(String subject, int semester) {
        if (selectedStudent == null) return null;
        try {
            return marksController.getMarksBySemester(
                    selectedStudent.getStudentId(), semester)
                .stream()
                .filter(m -> subject.equals(m.getSubject()))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private JComboBox<Integer> buildSemesterCombo() {
        JComboBox<Integer> combo = new JComboBox<>();
        for (int s = 1; s <= 8; s++) combo.addItem(s);
        combo.setSelectedItem(5);
        return combo;
    }

    private JSpinner buildMarkSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 50.0, 0.5));
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

    // ── Cell renderer ─────────────────────────────────────────────────────────

    /** Highlights rows where grade point = 0 (Fail) in light red. */
    private static class FailingRowRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                Object gpVal = table.getModel().getValueAt(row, 6);
                try {
                    double gp = Double.parseDouble(gpVal == null ? "1" : gpVal.toString());
                    if (gp <= 0.0) {
                        c.setBackground(new Color(255, 220, 220));
                        c.setForeground(new Color(160, 30, 30));
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                } catch (NumberFormatException e) {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }
}
