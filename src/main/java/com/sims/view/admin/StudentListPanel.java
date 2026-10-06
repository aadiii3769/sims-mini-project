package com.sims.view.admin;

import com.sims.controller.StudentController;
import com.sims.model.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel displaying the full student list with search, add, and edit functionality.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: Button clicks, mouse double-clicks on table
 * rows, and search field changes all fire events handled by anonymous
 * {@code ActionListener} / {@code MouseAdapter} implementations (Observer
 * pattern). The panel reacts to these events by delegating to
 * {@link StudentController} — it never touches the DAO directly.</p>
 *
 * <h2>Threading</h2>
 * <p>All database calls are executed in a {@link SwingWorker}'s
 * {@code doInBackground()} method (off the EDT). The resulting data is
 * applied to the {@link DefaultTableModel} inside {@code done()} which runs
 * back on the EDT, per AGENTS.md Section 5.2.</p>
 */
public class StudentListPanel extends JPanel {

    // ── Column definitions ───────────────────────────────────────────────────
    private static final String[] COLUMNS =
        {"Roll No", "Full Name", "Department", "Year", "Section", "Email"};

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final JFrame              parentFrame;
    private final StudentController   studentController;

    // ── UI Components ─────────────────────────────────────────────────────────
    private final DefaultTableModel   tableModel;
    private final JTable              table;
    private final JTextField          searchField;
    private final JLabel              statusLabel;

    // ── Cache of current rows for double-click lookup ─────────────────────────
    private List<Student>             currentStudents;

    public StudentListPanel(JFrame parentFrame) {
        super(new BorderLayout(0, 0));
        this.parentFrame       = parentFrame;
        this.studentController = new StudentController();
        this.tableModel        = buildTableModel();
        this.table             = buildTable();
        this.searchField       = new JTextField(20);
        this.statusLabel       = new JLabel("Loading…");

        buildUI();
        loadStudents("");          // initial load
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildFooter(),   BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // Left: title
        JLabel title = new JLabel("Student List");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));

        // Right: search + buttons
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        searchField.setToolTipText("Search by name, roll number, or department");
        JButton searchBtn  = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh");
        JButton addBtn     = createStyledButton("+ Add New Student", new Color(34, 139, 34), new Color(46, 160, 46));

        controls.add(new JLabel("Search:"));
        controls.add(searchField);
        controls.add(searchBtn);
        controls.add(refreshBtn);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(addBtn);

        bar.add(title,    BorderLayout.WEST);
        bar.add(controls, BorderLayout.EAST);

        // ── Observers (ActionListeners) ─────────────────────────────────────
        searchBtn.addActionListener(e -> loadStudents(searchField.getText()));
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadStudents("");
        });
        searchField.addActionListener(e -> loadStudents(searchField.getText())); // Enter key
        addBtn.addActionListener(e -> openFormDialog(null));   // null = CREATE mode

        return bar;
    }

    /**
     * Creates a custom painted button with anti-aliasing, rounded corners,
     * and rollover/press states that works reliably across all Swing Look and Feels
     * (especially Windows L&F where default JButtons ignore setBackground with white text).
     */
    private JButton createStyledButton(String text, Color normalColor, Color hoverColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill;
                if (getModel().isPressed()) {
                    fill = normalColor.darker();
                } else if (getModel().isRollover()) {
                    fill = hoverColor;
                } else {
                    fill = normalColor;
                }
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
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
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return btn;
    }

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(24);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        t.setGridColor(new Color(220, 220, 220));
        t.setShowGrid(true);

        // Column widths
        t.getColumnModel().getColumn(0).setPreferredWidth(90);   // Roll No
        t.getColumnModel().getColumn(1).setPreferredWidth(200);  // Full Name
        t.getColumnModel().getColumn(2).setPreferredWidth(220);  // Department
        t.getColumnModel().getColumn(3).setPreferredWidth(50);   // Year
        t.getColumnModel().getColumn(4).setPreferredWidth(60);   // Section
        t.getColumnModel().getColumn(5).setPreferredWidth(200);  // Email

        // Double-click → open EDIT mode
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentStudents != null) {
                    int row = t.getSelectedRow();
                    if (row >= 0 && row < currentStudents.size()) {
                        openFormDialog(currentStudents.get(row));
                    }
                }
            }
        });

        return t;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusLabel.setForeground(Color.DARK_GRAY);
        footer.add(statusLabel);
        return footer;
    }

    // ── Data Loading (SwingWorker — off EDT) ──────────────────────────────────

    /**
     * Loads students matching the given keyword into the table.
     * Database call runs in {@code doInBackground()} (off EDT);
     * UI update runs in {@code done()} (on EDT).
     *
     * @param keyword search term; blank = show all
     */
    public void loadStudents(String keyword) {
        statusLabel.setText("Loading…");
        tableModel.setRowCount(0);

        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() {
                return studentController.searchStudents(keyword);
            }

            @Override
            protected void done() {
                try {
                    currentStudents = get();
                    for (Student s : currentStudents) {
                        tableModel.addRow(new Object[]{
                            s.getRollNumber(),
                            s.getFullName(),
                            s.getDepartment(),
                            s.getYear(),
                            s.getSection() != null ? s.getSection() : "",
                            s.getEmail()
                        });
                    }
                    statusLabel.setText(currentStudents.size() + " student(s) found.");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading students.");
                    JOptionPane.showMessageDialog(
                        StudentListPanel.this,
                        "Failed to load students:\n" + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    // ── Dialog Launcher ───────────────────────────────────────────────────────

    /**
     * Opens {@link StudentFormDialog} in the appropriate mode.
     *
     * @param student {@code null} for CREATE mode; populated Student for EDIT mode
     */
    private void openFormDialog(Student student) {
        StudentFormDialog dialog = new StudentFormDialog(parentFrame, student, studentController);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadStudents(searchField.getText());   // refresh table
        }
    }
}
