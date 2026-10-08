package com.sims.view.student;

import com.sims.controller.TranscriptController;
import com.sims.controller.TranscriptController.SemesterSummary;
import com.sims.model.Mark;
import com.sims.model.Student;
import com.sims.model.User;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Student (and Parent) panel for viewing the complete academic transcript.
 *
 * <p>Displays semester-wise {@code JTable} sections separated by semester
 * headers, a CGPA summary footer, "Download as Text" export, and a "Print"
 * action using {@code java.awt.print.PrinterJob}.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: The Refresh, Download, and Print buttons
 * each register {@link java.awt.event.ActionListener} callbacks (Observer
 * pattern). Those observers delegate all DB access to
 * {@link TranscriptController}, keeping SQL and grouping logic out of the
 * View, per AGENTS.md §3.</p>
 *
 * <h2>Threading</h2>
 * <p>All database queries execute inside {@link SwingWorker} off the EDT;
 * UI mutations happen only in {@code done()} on the EDT, per AGENTS.md §5.</p>
 *
 * <h2>Layout Rules</h2>
 * <p>Uses only {@link BorderLayout}, {@link BoxLayout}, {@link FlowLayout},
 * and {@link GridBagLayout} — no null/absolute layouts per AGENTS.md §6.</p>
 */
public class TranscriptPanel extends JPanel {

    private static final String[] MARK_COLS =
        { "Subject", "CAT 1", "CAT 2", "CAT 3", "Total", "Grade", "GP" };

    // ── State ─────────────────────────────────────────────────────────────────
    private final User                  viewer;
    private final TranscriptController  controller;
    private final JPanel                bodyPanel;   // holds semester sub-panels
    private final JLabel                studentLabel;
    private final JLabel                cgpaLabel;
    private final JLabel                statusLabel;
    private final JButton               refreshBtn;
    private final JButton               downloadBtn;
    private final JButton               printBtn;

    private Student                     currentStudent;
    private Map<Integer, SemesterSummary> lastTranscript;
    private double                      lastCGPA;

    // ── Constructor ───────────────────────────────────────────────────────────

    public TranscriptPanel(User viewer) {
        super(new BorderLayout(0, 0));
        this.viewer      = viewer;
        this.controller  = new TranscriptController();
        this.studentLabel = new JLabel("Loading student profile…");
        this.cgpaLabel    = buildCGPALabel();
        this.statusLabel  = new JLabel("Loading transcript…");
        this.refreshBtn   = createStyledButton("⟳ Refresh",  new Color(50, 100, 180), new Color(70, 130, 210));
        this.downloadBtn  = createStyledButton("↓ Download",  new Color(30, 120, 60),  new Color(45, 150, 80));
        this.printBtn     = createStyledButton("⎙ Print",     new Color(100, 50, 160), new Color(130, 70, 200));

        downloadBtn.setEnabled(false);
        printBtn.setEnabled(false);

        this.bodyPanel = new JPanel();
        this.bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        this.bodyPanel.setBackground(Color.WHITE);

        wireListeners();
        buildUI();
        loadStudentAndTranscript();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(buildToolbar(),      BorderLayout.NORTH);
        add(buildScrollArea(),   BorderLayout.CENTER);
        add(buildFooter(),       BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        studentLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.add(refreshBtn);
        btnPanel.add(downloadBtn);
        btnPanel.add(printBtn);

        bar.add(studentLabel, BorderLayout.WEST);
        bar.add(btnPanel,     BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildScrollArea() {
        JScrollPane scroll = new JScrollPane(bodyPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 215, 220)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildFooter() {
        JPanel outer = new JPanel(new BorderLayout(0, 4));

        // CGPA band
        JPanel cgpaBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        cgpaBar.setBackground(new Color(230, 240, 255));
        cgpaBar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(170, 200, 255)),
            new EmptyBorder(2, 4, 2, 4)));
        cgpaBar.add(cgpaLabel);
        outer.add(cgpaBar, BorderLayout.NORTH);

        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusBar.add(statusLabel);
        outer.add(statusBar, BorderLayout.SOUTH);

        return outer;
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void wireListeners() {
        refreshBtn.addActionListener(e -> loadStudentAndTranscript());
        downloadBtn.addActionListener(e -> handleDownload());
        printBtn.addActionListener(e -> handlePrint());
    }

    // ── Data loaders ──────────────────────────────────────────────────────────

    private void loadStudentAndTranscript() {
        refreshBtn.setEnabled(false);
        statusLabel.setText("Loading student profile…");
        bodyPanel.removeAll();
        bodyPanel.revalidate();

        new SwingWorker<Optional<Student>, Void>() {
            @Override
            protected Optional<Student> doInBackground() {
                return controller.findStudentForViewer(viewer);
            }
            @Override
            protected void done() {
                try {
                    Optional<Student> opt = get();
                    if (opt.isEmpty()) {
                        studentLabel.setText("No linked student profile found.");
                        statusLabel.setText("Transcript unavailable.");
                        refreshBtn.setEnabled(true);
                        return;
                    }
                    currentStudent = opt.get();
                    studentLabel.setText(currentStudent.getRollNumber()
                        + " – " + currentStudent.getFullName()
                        + "   [" + currentStudent.getDepartment() + "]");
                    loadTranscript();
                } catch (Exception ex) {
                    statusLabel.setText("Error loading student profile.");
                    refreshBtn.setEnabled(true);
                    JOptionPane.showMessageDialog(TranscriptPanel.this,
                        "Failed to load profile:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadTranscript() {
        statusLabel.setText("Loading transcript…");

        new SwingWorker<TranscriptData, Void>() {
            @Override
            protected TranscriptData doInBackground() {
                Map<Integer, SemesterSummary> t = controller.generateTranscript(
                        currentStudent.getStudentId());
                double cgpa = controller.computeCGPA(currentStudent.getStudentId());
                return new TranscriptData(t, cgpa);
            }
            @Override
            protected void done() {
                try {
                    TranscriptData data = get();
                    lastTranscript = data.transcript();
                    lastCGPA = data.cgpa();
                    renderTranscript(lastTranscript, lastCGPA);
                    downloadBtn.setEnabled(!lastTranscript.isEmpty());
                    printBtn.setEnabled(!lastTranscript.isEmpty());
                    refreshBtn.setEnabled(true);
                } catch (Exception ex) {
                    statusLabel.setText("Error loading transcript.");
                    refreshBtn.setEnabled(true);
                    JOptionPane.showMessageDialog(TranscriptPanel.this,
                        "Failed to load transcript:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void renderTranscript(Map<Integer, SemesterSummary> transcript, double cgpa) {
        bodyPanel.removeAll();

        if (transcript.isEmpty()) {
            JLabel empty = new JLabel("No academic records found for this student.");
            empty.setFont(new Font("SansSerif", Font.ITALIC, 13));
            empty.setForeground(Color.GRAY);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 16, 0, 0));
            bodyPanel.add(empty);
        } else {
            for (SemesterSummary sem : transcript.values()) {
                bodyPanel.add(buildSemesterSection(sem));
                bodyPanel.add(Box.createVerticalStrut(12));
            }
        }

        cgpaLabel.setText(String.format("Overall CGPA:  %.2f / 10.00", cgpa));
        int total = transcript.values().stream()
                .mapToInt(s -> s.marks().size()).sum();
        statusLabel.setText(String.format(
                "%d semester(s) · %d subject(s) loaded.", transcript.size(), total));

        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    private JPanel buildSemesterSection(SemesterSummary sem) {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setBackground(Color.WHITE);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        section.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Semester header label
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setBackground(new Color(40, 70, 120));
        header.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel hdr = new JLabel(String.format(
                "  Semester %d     GPA: %.2f     Pass: %d   Fail: %d",
                sem.semester(), sem.gpa(), sem.passCount(), sem.failCount()));
        hdr.setFont(new Font("SansSerif", Font.BOLD, 13));
        hdr.setForeground(Color.WHITE);
        header.add(hdr);

        // Marks table
        DefaultTableModel model = new DefaultTableModel(MARK_COLS, 0) {
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
        table.setDefaultRenderer(Object.class, new TranscriptRowRenderer());

        // Right-align numeric columns (1–6)
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i <= 6; i++) table.getColumnModel().getColumn(i).setCellRenderer(rightAlign);

        section.add(header, BorderLayout.NORTH);
        section.add(table.getTableHeader(), BorderLayout.CENTER); // not ideal but need scroll
        // Wrap in proper scroll so header shows
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(900, 26 + table.getRowHeight() * (sem.marks().size() + 1)));
        sp.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 230)));

        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(sp, BorderLayout.CENTER);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        return wrapper;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void handleDownload() {
        if (currentStudent == null || lastTranscript == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Transcript");
        String suggested = currentStudent.getRollNumber() + "_transcript.txt";
        chooser.setSelectedFile(new File(suggested));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File target = chooser.getSelectedFile();
        String text = controller.exportTranscriptText(currentStudent, lastTranscript, lastCGPA);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(target))) {
            bw.write(text);
            JOptionPane.showMessageDialog(this,
                "Transcript saved to:\n" + target.getAbsolutePath(),
                "Download Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Failed to save file:\n" + ex.getMessage(),
                "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handlePrint() {
        if (currentStudent == null || lastTranscript == null) return;
        String text = controller.exportTranscriptText(currentStudent, lastTranscript, lastCGPA);
        JTextArea printArea = new JTextArea(text);
        printArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        try {
            printArea.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                "Print failed:\n" + ex.getMessage(),
                "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private JLabel buildCGPALabel() {
        JLabel lbl = new JLabel("CGPA: —");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(new Color(30, 70, 150));
        return lbl;
    }

    /**
     * Creates a styled action button consistent with Phase 5 UI patterns.
     * Uses custom {@code paintComponent} to render a rounded fill, ensuring
     * legible foreground text under the Windows Look and Feel.
     */
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

    /** Colours failing rows (grade point 0.0) in a soft red. */
    private static class TranscriptRowRenderer extends DefaultTableCellRenderer {
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

    /** Internal value holder for the SwingWorker result. */
    private record TranscriptData(Map<Integer, SemesterSummary> transcript, double cgpa) {}
}
