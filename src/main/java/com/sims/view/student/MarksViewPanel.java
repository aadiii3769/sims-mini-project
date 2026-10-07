package com.sims.view.student;

import com.sims.controller.MarksController;
import com.sims.model.Mark;
import com.sims.model.Student;
import com.sims.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Student and Parent panel for viewing CAT marks, historical academic transcripts,
 * semester GPA, and cumulative CGPA.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: The semester {@code JComboBox} selector and
 * the Refresh button publish Swing events observed by {@code ActionListener}
 * callbacks. Those observers delegate data retrieval to {@link MarksController},
 * preserving the View → Controller → DAO dependency direction.</p>
 */
public class MarksViewPanel extends JPanel {

    private static final String[] MARKS_COLS = {
        "Code", "Course Name", "Credits", "CAT 1", "CAT 2", "Assignment",
        "Total Internal", "Grade", "Grade Point", "Status"
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private final User               viewer;
    private final MarksController    marksController;
    private final DefaultTableModel  marksModel;
    private final JTable             marksTable;
    private final JComboBox<Integer> semesterCombo;
    private final JLabel             studentLabel;
    private final JLabel             semesterStatusBadge;
    private final JLabel             gpaLabel;
    private final JLabel             cgpaLabel;
    private final JLabel             statusLabel;

    private Student currentStudent;

    public MarksViewPanel(User viewer) {
        super(new BorderLayout(0, 8));
        this.viewer              = viewer;
        this.marksController     = new MarksController();
        this.marksModel          = buildMarksModel();
        this.marksTable          = buildMarksTable();
        this.semesterCombo       = buildSemesterCombo();
        this.studentLabel        = new JLabel("Loading student profile...");
        this.semesterStatusBadge = new JLabel("Status: —");
        this.gpaLabel            = new JLabel("Semester GPA: —");
        this.cgpaLabel           = new JLabel("CGPA: —");
        this.statusLabel         = new JLabel("Loading marks...");

        semesterCombo.addActionListener(e -> refreshMarks());

        buildUI();
        loadStudentAndMarks();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(buildToolbar(),    BorderLayout.NORTH);
        add(new JScrollPane(marksTable), BorderLayout.CENTER);
        add(buildSummaryBar(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel outer = new JPanel(new BorderLayout(8, 6));
        outer.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        // Left info: student details and status badge
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        studentLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        semesterStatusBadge.setFont(new Font("SansSerif", Font.PLAIN, 12));
        semesterStatusBadge.setForeground(new Color(40, 100, 180));
        infoPanel.add(studentLabel);
        infoPanel.add(semesterStatusBadge);

        // Right controls: semester dropdown and refresh
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.add(new JLabel("Semester:"));
        controls.add(semesterCombo);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshMarks());
        controls.add(refresh);

        outer.add(infoPanel, BorderLayout.WEST);
        outer.add(controls,  BorderLayout.EAST);
        return outer;
    }

    private JPanel buildSummaryBar() {
        JPanel outer = new JPanel(new BorderLayout(0, 4));

        // GPA / CGPA line
        JPanel gpaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        gpaPanel.setBackground(new Color(235, 244, 255));
        gpaPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(180, 210, 255)));

        gpaLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        gpaLabel.setForeground(new Color(30, 80, 150));
        cgpaLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        cgpaLabel.setForeground(new Color(30, 80, 150));
        gpaPanel.add(gpaLabel);
        gpaPanel.add(new JLabel(" | "));
        gpaPanel.add(cgpaLabel);

        // Status bar
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusPanel.add(statusLabel);

        outer.add(gpaPanel,    BorderLayout.NORTH);
        outer.add(statusPanel, BorderLayout.SOUTH);
        return outer;
    }

    // ── Table builder ─────────────────────────────────────────────────────────

    private JTable buildMarksTable() {
        JTable table = new JTable(marksModel);
        table.setRowHeight(26);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setDefaultRenderer(Object.class, new MarksRowRenderer());

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(85);   // Code
        table.getColumnModel().getColumn(1).setPreferredWidth(260);  // Name
        table.getColumnModel().getColumn(2).setPreferredWidth(60);   // Credits
        table.getColumnModel().getColumn(3).setPreferredWidth(70);   // CAT1
        table.getColumnModel().getColumn(4).setPreferredWidth(70);   // CAT2
        table.getColumnModel().getColumn(5).setPreferredWidth(90);   // Assignment
        table.getColumnModel().getColumn(6).setPreferredWidth(95);   // Total
        table.getColumnModel().getColumn(7).setPreferredWidth(65);   // Grade
        table.getColumnModel().getColumn(8).setPreferredWidth(85);   // Grade Pt
        table.getColumnModel().getColumn(9).setPreferredWidth(100);  // Status

        return table;
    }

    // ── Data loaders ──────────────────────────────────────────────────────────

    private void loadStudentAndMarks() {
        new SwingWorker<Optional<Student>, Void>() {
            @Override
            protected Optional<Student> doInBackground() {
                return marksController.findStudentForViewer(viewer);
            }
            @Override
            protected void done() {
                try {
                    Optional<Student> student = get();
                    if (student.isEmpty()) {
                        studentLabel.setText("No linked student profile found.");
                        statusLabel.setText("Marks unavailable.");
                        return;
                    }
                    currentStudent = student.get();
                    studentLabel.setText(currentStudent.getRollNumber()
                        + " – " + currentStudent.getFullName()
                        + " (" + currentStudent.getDepartment() + ")");
                    // Default to current active semester
                    semesterCombo.setSelectedItem(currentStudent.getCurrentSemester());
                    refreshMarks();
                } catch (Exception ex) {
                    statusLabel.setText("Error loading student profile.");
                    JOptionPane.showMessageDialog(MarksViewPanel.this,
                        "Failed to load marks profile:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshMarks() {
        if (currentStudent == null) return;
        Integer semObj = (Integer) semesterCombo.getSelectedItem();
        int semester = (semObj != null) ? semObj : currentStudent.getCurrentSemester();

        // Update semester badge
        int activeSem = currentStudent.getCurrentSemester();
        if (semester == activeSem) {
            semesterStatusBadge.setText("Mode: Active Odd Semester (Live CAT Assessments & Internal Marks)");
            semesterStatusBadge.setForeground(new Color(20, 120, 40));
        } else if (semester < activeSem) {
            semesterStatusBadge.setText("Mode: Completed Semester (Official Historical Academic Transcript)");
            semesterStatusBadge.setForeground(new Color(30, 80, 160));
        } else {
            semesterStatusBadge.setText("Mode: Future Semester (Not Enrolled / Pending)");
            semesterStatusBadge.setForeground(Color.DARK_GRAY);
        }

        statusLabel.setText("Loading marks for Semester " + semester + "...");
        marksModel.setRowCount(0);

        new SwingWorker<MarksData, Void>() {
            @Override
            protected MarksData doInBackground() {
                List<Mark> marks = marksController.getMarksByStudentAndSemester(
                        currentStudent.getStudentId(), semester);
                double semGPA = marksController.getSemesterGPA(
                        currentStudent.getStudentId(), semester);
                double cgpa   = marksController.getCGPA(currentStudent.getStudentId());
                return new MarksData(marks, semGPA, cgpa);
            }
            @Override
            protected void done() {
                try {
                    MarksData data = get();
                    for (Mark m : data.marks()) {
                        String statusStr = m.isCompleted() ? "Completed" : "In Progress";
                        marksModel.addRow(new Object[]{
                            m.getCourseCode() != null ? m.getCourseCode() : "—",
                            m.getCourseName() != null ? m.getCourseName() : m.getSubject(),
                            m.getCredits() > 0 ? m.getCredits() : 3,
                            String.format("%.1f", m.getCat1Marks()),
                            String.format("%.1f", m.getCat2Marks()),
                            String.format("%.1f", m.getAssignmentMarks()),
                            String.format("%.1f", m.getTotalInternal()),
                            m.getSemesterGrade() != null ? m.getSemesterGrade() : "—",
                            String.format("%.1f", m.getGradePoint()),
                            statusStr
                        });
                    }

                    if (data.marks().isEmpty()) {
                        gpaLabel.setText("Semester " + semester + " GPA: —");
                        statusLabel.setText("No course assessment records found for Semester " + semester + ".");
                    } else {
                        gpaLabel.setText("Semester " + semester + " GPA: " + String.format("%.2f", data.semGPA()));
                        statusLabel.setText(data.marks().size() + " subject(s) recorded for Semester " + semester + ".");
                    }
                    cgpaLabel.setText("Cumulative CGPA: " + String.format("%.2f", data.cgpa()));
                } catch (Exception ex) {
                    statusLabel.setText("Error loading marks.");
                    JOptionPane.showMessageDialog(MarksViewPanel.this,
                        "Failed to load marks:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private JComboBox<Integer> buildSemesterCombo() {
        JComboBox<Integer> combo = new JComboBox<>();
        for (int s = 1; s <= 8; s++) combo.addItem(s);
        return combo;
    }

    private DefaultTableModel buildMarksModel() {
        return new DefaultTableModel(MARKS_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    // ── Cell renderer ─────────────────────────────────────────────────────────

    private static class MarksRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            // Center align credits, marks, grades, status
            if (column >= 2) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            if (!isSelected) {
                Object gpVal = table.getModel().getValueAt(row, 8);
                Object gradeVal = table.getModel().getValueAt(row, 7);
                double gp = 1.0;
                try {
                    gp = Double.parseDouble(gpVal == null ? "1" : gpVal.toString());
                } catch (NumberFormatException ignored) {}

                if ("F".equals(gradeVal) || gp <= 0.0) {
                    c.setBackground(new Color(255, 230, 230));
                    c.setForeground(new Color(170, 20, 20));
                } else if ("Completed".equals(table.getModel().getValueAt(row, 9))) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(row % 2 == 0 ? new Color(245, 255, 245) : new Color(238, 252, 238));
                    c.setForeground(new Color(10, 80, 20));
                }
            }
            return c;
        }
    }

    // ── Record ────────────────────────────────────────────────────────────────
    private record MarksData(List<Mark> marks, double semGPA, double cgpa) {}
}
