package com.sims.view.faculty;

import com.sims.controller.AttendanceController;
import com.sims.controller.StudentController;
import com.sims.model.Attendance;
import com.sims.model.Student;
import com.sims.model.User;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Faculty panel for recording daily subject attendance.
 *
 * <h2>Design Pattern - Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: The Save, Refresh, subject selector, and
 * date controls publish UI events observed by {@code ActionListener}
 * callbacks. Those observers collect form state and delegate persistence to
 * {@link AttendanceController}.</p>
 */
public class AttendanceEntryPanel extends JPanel {

    private static final String[] COLUMNS = {"Roll No", "Full Name", "Department", "Year", "Section", "Status"};
    private static final String[] STATUSES = {"PRESENT", "ABSENT", "OD", "MEDICAL"};

    private final JFrame parentFrame;
    private final User facultyUser;
    private final AttendanceController attendanceController;
    private final StudentController studentController;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<String> subjectCombo;
    private final JSpinner dateSpinner;
    private final JLabel statusLabel;

    private List<Student> currentStudents;

    public AttendanceEntryPanel(JFrame parentFrame, User facultyUser) {
        super(new BorderLayout(0, 0));
        this.parentFrame = parentFrame;
        this.facultyUser = facultyUser;
        this.attendanceController = new AttendanceController();
        this.studentController = new StudentController();
        this.tableModel = buildTableModel();
        this.table = buildTable();
        this.subjectCombo = new JComboBox<>(AttendanceController.SUBJECTS);
        this.dateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, new Date(), java.util.Calendar.DAY_OF_MONTH));
        this.statusLabel = new JLabel("Loading students...");
        buildUI();
        loadStudents();
    }

    private void buildUI() {
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("Daily Attendance Entry");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        JButton refreshButton = new JButton("Refresh Students");
        JButton saveButton = new JButton("Save Attendance");

        controls.add(new JLabel("Subject:"));
        controls.add(subjectCombo);
        controls.add(new JLabel("Date:"));
        controls.add(dateSpinner);
        controls.add(refreshButton);
        controls.add(saveButton);

        refreshButton.addActionListener(e -> loadStudents());
        saveButton.addActionListener(e -> saveAttendance());

        panel.add(title, BorderLayout.WEST);
        panel.add(controls, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        panel.add(statusLabel);
        return panel;
    }

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };
    }

    private JTable buildTable() {
        JTable attendanceTable = new JTable(tableModel);
        attendanceTable.setRowHeight(24);
        attendanceTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        attendanceTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JComboBox<String> statusEditor = new JComboBox<>(STATUSES);
        attendanceTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(statusEditor));
        return attendanceTable;
    }

    private void loadStudents() {
        statusLabel.setText("Loading students...");
        tableModel.setRowCount(0);

        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() {
                return studentController.getAllStudents();
            }

            @Override
            protected void done() {
                try {
                    currentStudents = get();
                    for (Student student : currentStudents) {
                        tableModel.addRow(new Object[] {
                            student.getRollNumber(),
                            student.getFullName(),
                            student.getDepartment(),
                            student.getYear(),
                            student.getSection() != null ? student.getSection() : "",
                            "PRESENT"
                        });
                    }
                    statusLabel.setText(currentStudents.size() + " student(s) ready for attendance.");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading students.");
                    JOptionPane.showMessageDialog(
                        AttendanceEntryPanel.this,
                        "Failed to load students:\n" + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void saveAttendance() {
        if (currentStudents == null || currentStudents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students loaded.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Attendance> records = new ArrayList<>();
        LocalDate selectedDate = ((Date) dateSpinner.getValue()).toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        String subject = (String) subjectCombo.getSelectedItem();

        for (int row = 0; row < currentStudents.size(); row++) {
            Student student = currentStudents.get(row);
            Attendance attendance = new Attendance();
            attendance.setStudentId(student.getStudentId());
            attendance.setFacultyId(facultyUser.getUserId());
            attendance.setSubject(subject);
            attendance.setLogDate(selectedDate);
            attendance.setStatus(String.valueOf(tableModel.getValueAt(row, 5)));
            records.add(attendance);
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                attendanceController.markAttendance(records);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Attendance saved for " + records.size() + " student(s).");
                    JOptionPane.showMessageDialog(parentFrame, "Attendance saved successfully.");
                } catch (Exception ex) {
                    statusLabel.setText("Save failed.");
                    JOptionPane.showMessageDialog(
                        AttendanceEntryPanel.this,
                        "Failed to save attendance:\n" + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }
}
