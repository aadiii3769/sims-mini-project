package com.sims.view.admin;

import com.sims.controller.TranscriptController;
import com.sims.controller.TranscriptController.SemesterSummary;
import com.sims.model.Mark;
import com.sims.model.Student;
import com.sims.view.student.TranscriptPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Administrator panel for searching students and viewing or printing their
 * complete academic transcripts.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: Search field changes, table row selections,
 * and "View Transcript" / "Print All" button clicks are handled by
 * {@link java.awt.event.ActionListener} and
 * {@link javax.swing.event.ListSelectionListener} observers.  Those observers
 * delegate to {@link TranscriptController} for all data access — the View
 * layer only displays what the controller returns, per AGENTS.md §3.</p>
 *
 * <h2>Threading</h2>
 * <p>Student list loading and transcript generation run in {@link SwingWorker}
 * off the EDT; UI mutations happen only in {@code done()} on the EDT per
 * AGENTS.md §5.</p>
 *
 * <h2>Layout Rules</h2>
 * <p>Uses only {@link BorderLayout}, {@link GridBagLayout}, {@link FlowLayout},
 * and {@link JSplitPane} — no null/absolute layouts per AGENTS.md §6.</p>
 */
public class TranscriptAdminPanel extends JPanel {

    private static final String[] STUDENT_COLS =
        { "Roll No", "Full Name", "Department", "Year", "Section" };

    // ── State ─────────────────────────────────────────────────────────────────
    private final JFrame               parentFrame;
    private final TranscriptController controller;

    private final DefaultTableModel    studentTableModel;
    private final JTable               studentTable;
    private final JTextField           searchField;
    private final JLabel               statusLabel;
    private final JButton              viewBtn;
    private final JButton              printAllBtn;
    private final JButton              refreshBtn;

    private List<Student>              allStudents  = new ArrayList<>();
    private List<Student>              shown        = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public TranscriptAdminPanel(JFrame parentFrame) {
        super(new BorderLayout(0, 8));
        this.parentFrame  = parentFrame;
        this.controller   = new TranscriptController();

        this.studentTableModel = new DefaultTableModel(STUDENT_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        this.studentTable = buildStudentTable();
        this.searchField  = new JTextField(20);
        this.statusLabel  = new JLabel("Loading students…");
        this.viewBtn      = createStyledButton("📄 View Transcript",
                                new Color(40, 90, 170), new Color(60, 120, 210));
        this.printAllBtn  = createStyledButton("⎙ Print Selected",
                                new Color(100, 50, 160), new Color(130, 70, 200));
        this.refreshBtn   = createStyledButton("⟳ Refresh",
                                new Color(50, 130, 60), new Color(70, 160, 80));

        viewBtn.setEnabled(false);
        printAllBtn.setEnabled(false);

        wireListeners();
        buildUI();
        loadStudents();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel heading = new JLabel("Academic Transcript Viewer");
        heading.setFont(new Font("SansSerif", Font.BOLD, 15));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JLabel searchLbl = new JLabel("Search:");
        right.add(searchLbl);
        right.add(searchField);
        right.add(refreshBtn);
        right.add(viewBtn);
        right.add(printAllBtn);

        bar.add(heading, BorderLayout.WEST);
        bar.add(right,   BorderLayout.EAST);
        return bar;
    }

    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));

        // Instruction card
        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        info.setBackground(new Color(235, 245, 255));
        info.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 250)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        JLabel infoLbl = new JLabel(
            "<html><b>Instructions:</b> Search for a student by name or roll number, "
            + "select a row, then click <i>View Transcript</i> to open the full academic record.</html>");
        infoLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        info.add(infoLbl);
        panel.add(info, BorderLayout.NORTH);

        // Student table
        JScrollPane scroll = new JScrollPane(studentTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 225)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        bar.add(statusLabel);
        return bar;
    }

    // ── Table builder ─────────────────────────────────────────────────────────

    private JTable buildStudentTable() {
        JTable table = new JTable(studentTableModel);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(220, 230, 245));
        return table;
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void wireListeners() {
        // Row selection → enable action buttons
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean sel = studentTable.getSelectedRow() >= 0;
                viewBtn.setEnabled(sel);
                printAllBtn.setEnabled(sel);
            }
        });

        // Double-click row → immediately open transcript
        studentTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && studentTable.getSelectedRow() >= 0) {
                    openTranscriptDialog(false);
                }
            }
        });

        // Search field (case-insensitive filter on every keystroke)
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filterStudents(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filterStudents(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filterStudents(); }
        });

        refreshBtn.addActionListener(e -> loadStudents());
        viewBtn.addActionListener(e -> openTranscriptDialog(false));
        printAllBtn.addActionListener(e -> openTranscriptDialog(true));
    }

    // ── Data loaders ──────────────────────────────────────────────────────────

    private void loadStudents() {
        refreshBtn.setEnabled(false);
        statusLabel.setText("Loading students…");
        studentTableModel.setRowCount(0);

        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() {
                return controller.findAllStudents();
            }
            @Override
            protected void done() {
                try {
                    allStudents = get();
                    shown = new ArrayList<>(allStudents);
                    populateTable(shown);
                    statusLabel.setText(allStudents.size() + " student(s) loaded.");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading students.");
                    JOptionPane.showMessageDialog(TranscriptAdminPanel.this,
                        "Failed to load students:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void filterStudents() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            shown = new ArrayList<>(allStudents);
        } else {
            shown = new ArrayList<>();
            for (Student s : allStudents) {
                if (s.getRollNumber().toLowerCase().contains(query)
                        || (s.getFullName() != null
                            && s.getFullName().toLowerCase().contains(query))
                        || s.getDepartment().toLowerCase().contains(query)) {
                    shown.add(s);
                }
            }
        }
        populateTable(shown);
        statusLabel.setText(shown.size() + " student(s) found.");
    }

    private void populateTable(List<Student> students) {
        studentTableModel.setRowCount(0);
        for (Student s : students) {
            studentTableModel.addRow(new Object[]{
                s.getRollNumber(),
                s.getFullName(),
                s.getDepartment(),
                s.getYear(),
                s.getSection()
            });
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Opens a modal dialog embedding a {@link TranscriptPanel} (for viewing)
     * or triggers an immediate print action, for the selected student.
     *
     * @param printImmediately if {@code true}, the transcript is sent to print
     *                         instead of displayed visually
     */
    private void openTranscriptDialog(boolean printImmediately) {
        int row = studentTable.getSelectedRow();
        if (row < 0 || row >= shown.size()) return;
        Student student = shown.get(row);

        statusLabel.setText("Generating transcript for " + student.getRollNumber() + "…");
        viewBtn.setEnabled(false);
        printAllBtn.setEnabled(false);

        new SwingWorker<TranscriptPayload, Void>() {
            @Override
            protected TranscriptPayload doInBackground() {
                Map<Integer, SemesterSummary> t =
                    controller.generateTranscript(student.getStudentId());
                double cgpa = controller.computeCGPA(student.getStudentId());
                return new TranscriptPayload(student, t, cgpa);
            }
            @Override
            protected void done() {
                try {
                    TranscriptPayload p = get();

                    if (printImmediately) {
                        printTranscript(p);
                    } else {
                        showTranscriptDialog(p);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TranscriptAdminPanel.this,
                        "Failed to load transcript:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    statusLabel.setText(allStudents.size() + " student(s) loaded.");
                    int sel = studentTable.getSelectedRow();
                    viewBtn.setEnabled(sel >= 0);
                    printAllBtn.setEnabled(sel >= 0);
                }
            }
        }.execute();
    }

    /** Presents the transcript in a modal {@link JDialog}. */
    private void showTranscriptDialog(TranscriptPayload p) {
        JDialog dialog = new JDialog(parentFrame,
            "Transcript — " + p.student().getRollNumber()
            + " · " + p.student().getFullName(), true);
        dialog.setSize(980, 720);
        dialog.setMinimumSize(new Dimension(800, 550));
        dialog.setLocationRelativeTo(parentFrame);

        // Embed a read-only transcript body (reuse renderTranscript logic)
        JPanel body = buildTranscriptBody(p);
        dialog.add(body, BorderLayout.CENTER);

        // Bottom bar: Close + Print buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        JButton printBtn  = createStyledButton("⎙ Print",
            new Color(100, 50, 160), new Color(130, 70, 200));
        JButton closeBtn  = createStyledButton("Close",
            new Color(100, 100, 100), new Color(130, 130, 130));
        printBtn.addActionListener(e -> printTranscript(p));
        closeBtn.addActionListener(e -> dialog.dispose());
        btns.add(printBtn);
        btns.add(closeBtn);
        dialog.add(btns, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    /**
     * Builds the visual body of the transcript for display inside the admin
     * dialog.  Re-uses the same semester-section layout as
     * {@link TranscriptPanel}.
     */
    private JPanel buildTranscriptBody(TranscriptPayload p) {
        JPanel root = new JPanel(new BorderLayout(0, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 58, 95));
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel nameLabel = new JLabel(p.student().getRollNumber()
            + " — " + p.student().getFullName()
            + "   [" + p.student().getDepartment() + "]");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);
        header.add(nameLabel, BorderLayout.WEST);

        JLabel cgpaHdr = new JLabel(String.format("CGPA: %.2f / 10.00   ", p.cgpa()));
        cgpaHdr.setFont(new Font("SansSerif", Font.BOLD, 13));
        cgpaHdr.setForeground(new Color(180, 220, 255));
        header.add(cgpaHdr, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Body: semester sections in a scrollable BoxLayout panel
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);

        if (p.transcript().isEmpty()) {
            JLabel empty = new JLabel("No academic records found.");
            empty.setFont(new Font("SansSerif", Font.ITALIC, 13));
            empty.setForeground(Color.GRAY);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 16, 0, 0));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(empty);
        } else {
            for (SemesterSummary sem : p.transcript().values()) {
                body.add(buildSemSection(sem));
                body.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scroll = new JScrollPane(body);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        root.add(scroll, BorderLayout.CENTER);

        return root;
    }

    /** Builds a single semester section panel (header + table). */
    private JPanel buildSemSection(SemesterSummary sem) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(Color.WHITE);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));

        // Coloured header
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hdr.setBackground(new Color(40, 70, 120));
        hdr.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        JLabel lbl = new JLabel(String.format(
                "  Semester %d     GPA: %.2f     Pass: %d   Fail: %d",
                sem.semester(), sem.gpa(), sem.passCount(), sem.failCount()));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(Color.WHITE);
        hdr.add(lbl);

        // Table
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Subject","CAT1","CAT2","CAT3","Total","Grade","GP"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Mark m : sem.marks()) {
            model.addRow(new Object[]{
                m.getSubject(),
                String.format("%.1f", m.getCat1Marks()),
                String.format("%.1f", m.getCat2Marks()),
                String.format("%.1f", m.getCat3Marks()),
                String.format("%.1f", m.getTotalMarks()),
                m.getLetterGrade(),
                String.format("%.1f", m.getGradePoint())
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(220, 230, 245));
        // Fail row renderer
        table.setDefaultRenderer(Object.class, new FailRowRenderer());

        int rows = sem.marks().size();
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(900, 26 + table.getRowHeight() * (rows + 1)));
        sp.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 230)));

        wrapper.add(hdr, BorderLayout.NORTH);
        wrapper.add(sp,  BorderLayout.CENTER);
        return wrapper;
    }

    /** Sends the transcript text to the printer via {@code JTextArea.print()}. */
    private void printTranscript(TranscriptPayload p) {
        String text = controller.exportTranscriptText(
                p.student(), p.transcript(), p.cgpa());
        JTextArea area = new JTextArea(text);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        try {
            area.print();
        } catch (java.awt.print.PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                "Print failed:\n" + ex.getMessage(),
                "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static JButton createStyledButton(String label, Color base, Color hover) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = isEnabled()
                    ? (getModel().isPressed() ? base.darker()
                        : (getModel().isRollover() ? hover : base))
                    : new Color(180, 180, 180);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return btn;
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    private static class FailRowRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
            if (!isSelected) {
                Object gpVal = table.getModel().getValueAt(row, 6);
                double gp = 1.0;
                try { gp = Double.parseDouble(gpVal == null ? "1" : gpVal.toString()); }
                catch (NumberFormatException ignored) {}

                if (gp <= 0.0) {
                    c.setBackground(new Color(255, 218, 218));
                    c.setForeground(new Color(160, 30, 30));
                } else if (row % 2 == 0) {
                    c.setBackground(new Color(247, 249, 252));
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }

    /** Bundles transcript data for a selected student. */
    private record TranscriptPayload(
            Student student,
            Map<Integer, SemesterSummary> transcript,
            double cgpa) {}
}
