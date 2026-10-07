package com.sims.view.student;

import com.sims.controller.AttendanceController;
import com.sims.controller.AttendanceController.MonthlySummary;
import com.sims.model.Attendance;
import com.sims.model.Student;
import com.sims.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Student and parent attendance history screen with monthly percentage summary.
 *
 * <h2>Design Pattern - Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: Month, year, subject, scope, and refresh actions
 * are observed through Swing {@code ActionListener} callbacks. Those observers
 * refresh the table models by delegating to {@link AttendanceController},
 * preserving the View -> Controller -> DAO dependency direction.</p>
 */
public class AttendanceViewPanel extends JPanel {

    private static final String[] SUMMARY_COLUMNS = {"Subject", "Present", "Absent", "Total", "Percentage"};
    private static final String[] LOG_COLUMNS = {"Date", "Subject", "Status", "Remarks"};

    private final User viewer;
    private final AttendanceController attendanceController;
    private final DefaultTableModel summaryModel;
    private final DefaultTableModel logModel;
    private final JTable summaryTable;
    private final JTable logTable;
    private final JComboBox<String> subjectCombo;
    private final JComboBox<String> scopeCombo;
    private final JComboBox<Integer> monthCombo;
    private final JSpinner yearSpinner;
    private final JLabel studentLabel;
    private final JLabel statusLabel;

    private Student currentStudent;

    public AttendanceViewPanel(User viewer) {
        super(new BorderLayout(0, 10));
        this.viewer = viewer;
        this.attendanceController = new AttendanceController();
        this.summaryModel = buildSummaryModel();
        this.logModel = buildLogModel();
        this.summaryTable = buildSummaryTable();
        this.logTable = new JTable(logModel);
        this.subjectCombo = buildSubjectCombo();
        this.scopeCombo = buildScopeCombo();
        this.monthCombo = buildMonthCombo();
        this.yearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
        this.studentLabel = new JLabel("Loading student...");
        this.statusLabel = new JLabel("Loading attendance...");

        subjectCombo.addActionListener(e -> refreshLogs());
        scopeCombo.addActionListener(e -> refreshLogs());

        buildUI();
        loadStudentAndAttendance();
    }

    private void buildUI() {
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(buildToolbar(), BorderLayout.NORTH);

        JPanel tables = new JPanel(new BorderLayout(0, 10));
        tables.add(buildSummarySection(), BorderLayout.NORTH);
        tables.add(buildLogSection(), BorderLayout.CENTER);
        add(tables, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        studentLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.add(new JLabel("Month:"));
        controls.add(monthCombo);
        controls.add(new JLabel("Year:"));
        controls.add(yearSpinner);

        monthCombo.addActionListener(e -> refreshMonthlySummary());
        yearSpinner.addChangeListener(e -> refreshMonthlySummary());

        toolbar.add(studentLabel, BorderLayout.WEST);
        toolbar.add(controls, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel buildSummarySection() {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        JLabel title = new JLabel("Monthly Summary");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        section.add(title, BorderLayout.NORTH);

        summaryTable.setPreferredScrollableViewportSize(new Dimension(summaryTable.getPreferredSize().width, 95));
        section.add(new JScrollPane(summaryTable), BorderLayout.CENTER);
        return section;
    }

    private JPanel buildLogSection() {
        JPanel section = new JPanel(new BorderLayout(0, 4));

        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 0));

        JLabel title = new JLabel("Attendance Log");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        headerPanel.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.add(new JLabel("Subject:"));
        controls.add(subjectCombo);
        controls.add(new JLabel("Show:"));
        controls.add(scopeCombo);

        JButton refreshLogsBtn = new JButton("Refresh");
        refreshLogsBtn.addActionListener(e -> refreshLogs());
        controls.add(refreshLogsBtn);

        headerPanel.add(controls, BorderLayout.EAST);
        section.add(headerPanel, BorderLayout.NORTH);

        logTable.setRowHeight(24);
        logTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        section.add(new JScrollPane(logTable), BorderLayout.CENTER);
        return section;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        footer.add(statusLabel);
        return footer;
    }

    private JComboBox<Integer> buildMonthCombo() {
        JComboBox<Integer> combo = new JComboBox<>();
        for (int month = 1; month <= 12; month++) {
            combo.addItem(month);
        }
        combo.setSelectedItem(LocalDate.now().getMonthValue());
        return combo;
    }

    private JComboBox<String> buildSubjectCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem("All Subjects");
        for (String subject : AttendanceController.SUBJECTS) {
            combo.addItem(subject);
        }
        return combo;
    }

    private JComboBox<String> buildScopeCombo() {
        return new JComboBox<>(new String[] {
            "Last 5 Logs",
            "Last 10 Logs",
            "All Logs (Full History)"
        });
    }

    private DefaultTableModel buildSummaryModel() {
        return new DefaultTableModel(SUMMARY_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private DefaultTableModel buildLogModel() {
        return new DefaultTableModel(LOG_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable buildSummaryTable() {
        JTable table = new JTable(summaryModel);
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
            ) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Object percentageValue = table.getModel().getValueAt(row, 4);
                double percentage = parsePercentage(percentageValue);
                if (!isSelected && percentage > 0.0 && percentage < 75.0) {
                    component.setForeground(new Color(170, 30, 30));
                    component.setBackground(new Color(255, 235, 235));
                } else if (!isSelected) {
                    component.setForeground(Color.BLACK);
                    component.setBackground(Color.WHITE);
                }
                return component;
            }
        });
        return table;
    }

    private void loadStudentAndAttendance() {
        new SwingWorker<Optional<Student>, Void>() {
            @Override
            protected Optional<Student> doInBackground() {
                return attendanceController.findStudentForViewer(viewer);
            }

            @Override
            protected void done() {
                try {
                    Optional<Student> student = get();
                    if (student.isEmpty()) {
                        studentLabel.setText("No linked student profile found.");
                        statusLabel.setText("Attendance unavailable.");
                        return;
                    }
                    currentStudent = student.get();
                    studentLabel.setText(currentStudent.getRollNumber() + " - " + currentStudent.getFullName());
                    refreshAttendance();
                } catch (Exception ex) {
                    statusLabel.setText("Error loading student.");
                    JOptionPane.showMessageDialog(
                        AttendanceViewPanel.this,
                        "Failed to load attendance profile:\n" + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void refreshAttendance() {
        refreshMonthlySummary();
        refreshLogs();
    }

    private void refreshMonthlySummary() {
        if (currentStudent == null) {
            return;
        }

        int month = (Integer) monthCombo.getSelectedItem();
        int year = (Integer) yearSpinner.getValue();
        summaryModel.setRowCount(0);

        new SwingWorker<List<MonthlySummary>, Void>() {
            @Override
            protected List<MonthlySummary> doInBackground() {
                List<MonthlySummary> summaries = new ArrayList<>();
                for (String subject : AttendanceController.SUBJECTS) {
                    summaries.add(attendanceController.getMonthlySummary(
                        currentStudent.getStudentId(),
                        subject,
                        month,
                        year
                    ));
                }
                return summaries;
            }

            @Override
            protected void done() {
                try {
                    List<MonthlySummary> summaries = get();
                    for (MonthlySummary summary : summaries) {
                        summaryModel.addRow(new Object[] {
                            summary.getSubject(),
                            summary.getPresent(),
                            summary.getAbsent(),
                            summary.getTotal(),
                            String.format("%.1f%%", summary.getPercentage())
                        });
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error loading monthly summary.");
                    JOptionPane.showMessageDialog(
                        AttendanceViewPanel.this,
                        "Failed to load monthly summary:\n" + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void refreshLogs() {
        if (currentStudent == null) {
            return;
        }

        String selectedSubject = (String) subjectCombo.getSelectedItem();
        String selectedScope = (String) scopeCombo.getSelectedItem();
        Integer limit = parseLimit(selectedScope);

        statusLabel.setText("Loading attendance logs...");
        logModel.setRowCount(0);

        new SwingWorker<List<Attendance>, Void>() {
            @Override
            protected List<Attendance> doInBackground() {
                return attendanceController.getAttendanceHistory(
                    currentStudent.getStudentId(),
                    selectedSubject,
                    limit
                );
            }

            @Override
            protected void done() {
                try {
                    List<Attendance> logs = get();
                    for (Attendance attendance : logs) {
                        logModel.addRow(new Object[] {
                            attendance.getLogDate(),
                            attendance.getSubject(),
                            attendance.getStatus(),
                            attendance.getRemarks() != null ? attendance.getRemarks() : ""
                        });
                    }
                    statusLabel.setText(logs.size() + " log record(s) shown.");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading attendance logs.");
                    JOptionPane.showMessageDialog(
                        AttendanceViewPanel.this,
                        "Failed to load attendance logs:\n" + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private Integer parseLimit(String scope) {
        if (scope == null) {
            return 5;
        }
        if (scope.contains("5")) {
            return 5;
        }
        if (scope.contains("10")) {
            return 10;
        }
        return null; // All / Full History
    }

    private double parsePercentage(Object value) {
        if (value == null) {
            return 0.0;
        }
        String text = value.toString().replace("%", "");
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
