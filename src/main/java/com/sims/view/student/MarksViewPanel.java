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
 * Student (and Parent) panel for viewing CAT marks, semester GPA, and CGPA.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: The semester {@code JComboBox} and the
 * Refresh button publish Swing events observed by {@code ActionListener}
 * callbacks.  Those observers delegate all DB access to
 * {@link MarksController}, preserving the View → Controller → DAO direction
 * mandated by AGENTS.md §3.</p>
 *
 * <h2>Layout</h2>
 * <p>Nested {@code BorderLayout} panels with a fixed summary row at the
 * bottom — no null layout, per AGENTS.md §6.</p>
 */
public class MarksViewPanel extends JPanel {

    private static final String[] MARKS_COLS = {
        "Subject", "CAT 1", "CAT 2", "CAT 3", "Total", "Grade", "Grade Point"
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private final User             viewer;
    private final MarksController  marksController;
    private final DefaultTableModel marksModel;
    private final JTable           marksTable;
    private final JComboBox<Integer> semesterCombo;
    private final JLabel           studentLabel;
    private final JLabel           gpaLabel;
    private final JLabel           cgpaLabel;
    private final JLabel           statusLabel;

    private Student currentStudent;

    public MarksViewPanel(User viewer) {
        super(new BorderLayout(0, 8));
        this.viewer          = viewer;
        this.marksController = new MarksController();
        this.marksModel      = buildMarksModel();
        this.marksTable      = buildMarksTable();
        this.semesterCombo   = buildSemesterCombo();
        this.studentLabel    = new JLabel("Loading student...");
        this.gpaLabel        = new JLabel("Semester GPA: —");
        this.cgpaLabel       = new JLabel("CGPA: —");
        this.statusLabel     = new JLabel("Loading marks...");

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
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        studentLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.add(new JLabel("Semester:"));
        controls.add(semesterCombo);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshMarks());
        controls.add(refresh);

        toolbar.add(studentLabel, BorderLayout.WEST);
        toolbar.add(controls, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel buildSummaryBar() {
        JPanel outer = new JPanel(new BorderLayout(0, 4));

        // GPA / CGPA line
        JPanel gpaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
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
                        + " – " + currentStudent.getFullName());
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
        int semester = (Integer) semesterCombo.getSelectedItem();
        statusLabel.setText("Loading marks...");
        marksModel.setRowCount(0);

        new SwingWorker<MarksData, Void>() {
            @Override
            protected MarksData doInBackground() {
                List<Mark> marks = marksController.getMarksBySemester(
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
                        marksModel.addRow(new Object[]{
                            m.getSubject(),
                            m.getCat1Marks(),
                            m.getCat2Marks(),
                            m.getCat3Marks(),
                            m.getTotalMarks(),
                            m.getLetterGrade(),
                            String.format("%.1f", m.getGradePoint())
                        });
                    }
                    gpaLabel.setText("Semester " + semester + " GPA: "
                        + String.format("%.2f", data.semGPA()));
                    cgpaLabel.setText("CGPA: " + String.format("%.2f", data.cgpa()));
                    statusLabel.setText(data.marks().size() + " subject(s) for Semester " + semester + ".");
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
        combo.setSelectedItem(5);
        return combo;
    }

    private DefaultTableModel buildMarksModel() {
        return new DefaultTableModel(MARKS_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    // ── Cell renderer ─────────────────────────────────────────────────────────

    /** Colours failing rows (grade point = 0.0) in light red. */
    private static class MarksRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                Object gpVal = table.getModel().getValueAt(row, 6);
                double gp = 1.0;
                try { gp = Double.parseDouble(gpVal == null ? "1" : gpVal.toString()); }
                catch (NumberFormatException ignored) {}

                if (gp <= 0.0) {
                    c.setBackground(new Color(255, 220, 220));
                    c.setForeground(new Color(160, 30, 30));
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }

    // ── Record ────────────────────────────────────────────────────────────────
    private record MarksData(List<Mark> marks, double semGPA, double cgpa) {}
}
