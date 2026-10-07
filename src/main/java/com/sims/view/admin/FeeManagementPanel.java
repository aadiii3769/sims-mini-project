package com.sims.view.admin;

import com.sims.controller.PaymentController;
import com.sims.controller.StudentController;
import com.sims.model.Payment;
import com.sims.model.Student;
import com.sims.view.student.ReceiptDialog;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Administrator panel for fee invoice management and student billing.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: User actions (student table row selection,
 * search button clicks, "Create Invoice" triggers) publish events handled by
 * {@link java.awt.event.ActionListener} and {@link javax.swing.event.ListSelectionListener}
 * observers. These handlers delegate data retrieval and mutations to
 * {@link PaymentController} and {@link StudentController}, maintaining MVC separation.</p>
 *
 * <h2>Threading</h2>
 * <p>All database lookups and invoice insertions are executed via {@link SwingWorker}
 * off the Event Dispatch Thread (EDT) per AGENTS.md Section 5.</p>
 *
 * <h2>Layout Rules</h2>
 * <p>Built strictly with standard layout managers ({@link BorderLayout}, {@link GridBagLayout},
 * {@link FlowLayout}, {@link JSplitPane}) without null layouts per AGENTS.md Section 6.</p>
 */
public class FeeManagementPanel extends JPanel {

    private static final String[] STUDENT_COLS = {
        "Roll No", "Full Name", "Department", "Year"
    };

    private static final String[] INVOICE_COLS = {
        "Tx ID", "Fee Type", "Due (Rs)", "Paid (Rs)", "Balance (Rs)", "Status", "Due Date", "Paid Date", "Mode", "Receipt No"
    };

    private final JFrame            parentFrame;
    private final PaymentController paymentController;
    private final StudentController studentController;

    // Student list components (left pane)
    private final DefaultTableModel studentTableModel;
    private final JTable            studentTable;
    private final JTextField        searchField;
    private List<Student>           loadedStudents;
    private Student                 selectedStudent;

    // Invoice components (right pane)
    private final DefaultTableModel invoiceTableModel;
    private final JTable            invoiceTable;
    private final JLabel            studentBannerLabel;
    private final JLabel            outstandingLabel;
    private final JButton           createInvoiceBtn;
    private final JButton           refreshInvoicesBtn;
    private List<Payment>           loadedInvoices;

    public FeeManagementPanel(JFrame parentFrame) {
        super(new BorderLayout(0, 0));
        this.parentFrame       = parentFrame;
        this.paymentController = new PaymentController();
        this.studentController = new StudentController();

        this.studentTableModel = new DefaultTableModel(STUDENT_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        this.studentTable      = new JTable(studentTableModel);
        this.searchField       = new JTextField(15);

        this.invoiceTableModel = new DefaultTableModel(INVOICE_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        this.invoiceTable      = new JTable(invoiceTableModel);
        this.studentBannerLabel= new JLabel("Select a student to view fee details");
        this.outstandingLabel  = new JLabel("Outstanding: Rs 0.00");
        this.createInvoiceBtn  = createStyledButton("Create Invoice", new Color(25, 120, 45), new Color(35, 145, 55));
        this.createInvoiceBtn.setEnabled(false);
        this.refreshInvoicesBtn= createStyledButton("Refresh", new Color(60, 75, 95), new Color(80, 95, 115));
        this.refreshInvoicesBtn.setEnabled(false);

        buildUI();
        loadStudents("");
    }

    public FeeManagementPanel() {
        this(null);
    }

    private void buildUI() {
        setBorder(new EmptyBorder(10, 12, 10, 12));

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            buildLeftStudentPane(),
            buildRightInvoicePane()
        );
        splitPane.setDividerLocation(380);
        splitPane.setContinuousLayout(true);

        add(splitPane, BorderLayout.CENTER);
    }

    // ── Left: Student Selector Pane ──────────────────────────────────────────

    private JPanel buildLeftStudentPane() {
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setBorder(BorderFactory.createTitledBorder("Students"));

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(6, 0));
        searchBar.setBorder(new EmptyBorder(4, 4, 4, 4));

        JButton searchBtn = createStyledButton("Search", new Color(45, 90, 160), new Color(60, 110, 185));
        searchBtn.addActionListener(e -> loadStudents(searchField.getText().trim()));
        searchField.addActionListener(e -> loadStudents(searchField.getText().trim()));

        JButton showAllBtn = createStyledButton("All", new Color(90, 100, 115), new Color(110, 120, 135));
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            loadStudents("");
        });

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnGroup.add(searchBtn);
        btnGroup.add(showAllBtn);

        searchBar.add(new JLabel("Find: "), BorderLayout.WEST);
        searchBar.add(searchField,          BorderLayout.CENTER);
        searchBar.add(btnGroup,             BorderLayout.EAST);

        // Student table
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setRowHeight(26);
        studentTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        studentTable.setShowGrid(true);
        studentTable.setGridColor(new Color(230, 233, 238));
        studentTable.setDefaultRenderer(Object.class, new CleanDataCellRenderer(SwingConstants.LEFT));

        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = studentTable.getSelectedRow();
                if (row >= 0 && loadedStudents != null && row < loadedStudents.size()) {
                    selectedStudent = loadedStudents.get(row);
                    onStudentSelected(selectedStudent);
                }
            }
        });

        left.add(searchBar, BorderLayout.NORTH);
        left.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        return left;
    }

    // ── Right: Invoices and Billing Pane ─────────────────────────────────────

    private JPanel buildRightInvoicePane() {
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setBorder(BorderFactory.createTitledBorder("Fee Records & Invoices"));

        // Top info & actions bar
        JPanel topBar = new JPanel(new BorderLayout(8, 6));
        topBar.setBorder(new EmptyBorder(4, 6, 4, 6));

        JPanel bannerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        studentBannerLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        studentBannerLabel.setForeground(new Color(30, 50, 90));

        outstandingLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        outstandingLabel.setForeground(new Color(160, 40, 40));

        bannerPanel.add(studentBannerLabel);
        bannerPanel.add(outstandingLabel);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        createInvoiceBtn.addActionListener(e -> openCreateInvoiceDialog());
        refreshInvoicesBtn.addActionListener(e -> {
            if (selectedStudent != null) {
                loadInvoicesForStudent(selectedStudent);
            }
        });

        actionBtns.add(createInvoiceBtn);
        actionBtns.add(refreshInvoicesBtn);

        topBar.add(bannerPanel, BorderLayout.WEST);
        topBar.add(actionBtns,   BorderLayout.EAST);

        // Invoice table
        invoiceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        invoiceTable.setRowHeight(28);
        invoiceTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        invoiceTable.setShowGrid(true);
        invoiceTable.setGridColor(new Color(230, 233, 238));

        TableCellRenderer textRenderer   = new CleanDataCellRenderer(SwingConstants.LEFT);
        TableCellRenderer numRenderer    = new CleanDataCellRenderer(SwingConstants.RIGHT);
        TableCellRenderer centerRenderer = new CleanDataCellRenderer(SwingConstants.CENTER);
        TableCellRenderer statusRenderer = new StatusBadgeRenderer();

        invoiceTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Tx ID
        invoiceTable.getColumnModel().getColumn(1).setCellRenderer(textRenderer);   // Fee Type
        invoiceTable.getColumnModel().getColumn(2).setCellRenderer(numRenderer);    // Due
        invoiceTable.getColumnModel().getColumn(3).setCellRenderer(numRenderer);    // Paid
        invoiceTable.getColumnModel().getColumn(4).setCellRenderer(numRenderer);    // Balance
        invoiceTable.getColumnModel().getColumn(5).setCellRenderer(statusRenderer); // Status
        invoiceTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Due Date
        invoiceTable.getColumnModel().getColumn(7).setCellRenderer(centerRenderer); // Paid Date
        invoiceTable.getColumnModel().getColumn(8).setCellRenderer(centerRenderer); // Mode
        invoiceTable.getColumnModel().getColumn(9).setCellRenderer(centerRenderer); // Receipt No

        // Double-click to view receipt if PAID
        invoiceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = invoiceTable.getSelectedRow();
                    if (row >= 0 && loadedInvoices != null && row < loadedInvoices.size()) {
                        Payment p = loadedInvoices.get(row);
                        if ("PAID".equalsIgnoreCase(p.getPaymentStatus()) || p.getReceiptNumber() != null) {
                            p.setStudentName(selectedStudent != null ? selectedStudent.getFullName() : "");
                            ReceiptDialog rd = new ReceiptDialog(parentFrame, p);
                            rd.setVisible(true);
                        }
                    }
                }
            }
        });

        right.add(topBar, BorderLayout.NORTH);
        right.add(new JScrollPane(invoiceTable), BorderLayout.CENTER);
        return right;
    }

    // ── Logic: Student Loading & Selection ────────────────────────────────────

    private void loadStudents(String keyword) {
        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() {
                return studentController.searchStudents(keyword);
            }
            @Override
            protected void done() {
                try {
                    loadedStudents = get();
                    studentTableModel.setRowCount(0);
                    for (Student s : loadedStudents) {
                        studentTableModel.addRow(new Object[]{
                            s.getRollNumber(),
                            s.getFullName(),
                            s.getDepartment(),
                            s.getYear()
                        });
                    }
                    if (loadedStudents.isEmpty()) {
                        selectedStudent = null;
                        createInvoiceBtn.setEnabled(false);
                        createInvoiceBtn.repaint();
                        refreshInvoicesBtn.setEnabled(false);
                        refreshInvoicesBtn.repaint();
                        invoiceTableModel.setRowCount(0);
                        studentBannerLabel.setText("No students match search criteria.");
                        outstandingLabel.setText("Outstanding: Rs 0.00");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(FeeManagementPanel.this,
                        "Failed to load students:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void onStudentSelected(Student s) {
        studentBannerLabel.setText("Student: " + s.getRollNumber() + " — " + s.getFullName() + " (" + s.getDepartment() + ")");
        createInvoiceBtn.setEnabled(true);
        createInvoiceBtn.repaint();
        refreshInvoicesBtn.setEnabled(true);
        refreshInvoicesBtn.repaint();
        loadInvoicesForStudent(s);
    }

    private void loadInvoicesForStudent(Student s) {
        invoiceTableModel.setRowCount(0);
        new SwingWorker<List<Payment>, Void>() {
            @Override
            protected List<Payment> doInBackground() {
                return paymentController.getStudentFees(s.getStudentId());
            }
            @Override
            protected void done() {
                try {
                    loadedInvoices = get();
                    for (Payment p : loadedInvoices) {
                        invoiceTableModel.addRow(new Object[]{
                            p.getTransactionId(),
                            p.getFeeType(),
                            String.format("%.2f", p.getAmountDue()),
                            String.format("%.2f", p.getAmountPaid()),
                            String.format("%.2f", p.getOutstandingBalance()),
                            p.getPaymentStatus(),
                            p.getDueDate() != null ? p.getDueDate().toString() : "-",
                            p.getPaymentDate() != null ? p.getPaymentDate().toString() : "-",
                            p.getPaymentMode() != null ? p.getPaymentMode() : "-",
                            p.getReceiptNumber() != null ? p.getReceiptNumber() : "-"
                        });
                    }

                    BigDecimal totalOut = paymentController.getTotalOutstanding(s.getStudentId());
                    outstandingLabel.setText("Total Outstanding Balance: Rs " + String.format("%.2f", totalOut));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(FeeManagementPanel.this,
                        "Failed to load invoices:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Logic: Create Invoice Dialog ──────────────────────────────────────────

    private void openCreateInvoiceDialog() {
        if (selectedStudent == null) {
            JOptionPane.showMessageDialog(this, "Please select a student first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(parentFrame, "Create Fee Invoice", true);
        dialog.setSize(440, 370);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new CompoundBorder(
            new EmptyBorder(14, 16, 14, 16),
            new LineBorder(new Color(220, 225, 230), 1, true)
        ));
        form.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Student label
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        form.add(new JLabel("Student:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JLabel studLbl = new JLabel(selectedStudent.getRollNumber() + " - " + selectedStudent.getFullName());
        studLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        form.add(studLbl, gbc);

        // Fee Type
        String[] feeTypes = {"TUITION", "HOSTEL", "EXAM", "BUS", "LIBRARY", "LAB", "OTHER"};
        JComboBox<String> feeTypeCombo = new JComboBox<>(feeTypes);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        form.add(new JLabel("Fee Type:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(feeTypeCombo, gbc);

        // Amount Due
        JTextField amountField = new JTextField(10);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        form.add(new JLabel("Amount Due (Rs):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(amountField, gbc);

        // Due Date (Spinner with date model 30 days from now)
        Date defaultDueDate = Date.from(LocalDate.now().plusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());
        SpinnerDateModel dateModel = new SpinnerDateModel(defaultDueDate, null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner dueDateSpinner = new JSpinner(dateModel);
        dueDateSpinner.setEditor(new JSpinner.DateEditor(dueDateSpinner, "dd-MMM-yyyy"));
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        form.add(new JLabel("Due Date:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(dueDateSpinner, gbc);

        // Remarks
        JTextField remarksField = new JTextField(15);
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        form.add(new JLabel("Remarks (Opt):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(remarksField, gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton saveBtn = createStyledButton("Save Invoice", new Color(25, 120, 45), new Color(35, 145, 55));
        JButton cancelBtn = createStyledButton("Cancel", new Color(110, 115, 125), new Color(130, 135, 145));
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            String amtText = amountField.getText().trim();
            if (amtText.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Amount Due is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amtText);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Amount Due must be a positive number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Date selDate = (Date) dueDateSpinner.getValue();
            LocalDate dueDate = selDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            Payment invoice = new Payment();
            invoice.setStudentId(selectedStudent.getStudentId());
            invoice.setFeeType((String) feeTypeCombo.getSelectedItem());
            invoice.setAmountDue(amount);
            invoice.setDueDate(dueDate);
            invoice.setRemarks(remarksField.getText().trim());

            saveBtn.setEnabled(false);
            saveBtn.repaint();
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    paymentController.createInvoice(invoice);
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        dialog.dispose();
                        JOptionPane.showMessageDialog(FeeManagementPanel.this,
                            "Invoice created successfully for " + selectedStudent.getFullName(),
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadInvoicesForStudent(selectedStudent);
                    } catch (Exception ex) {
                        saveBtn.setEnabled(true);
                        saveBtn.repaint();
                        JOptionPane.showMessageDialog(dialog,
                            "Failed to create invoice:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── Button and Table Helpers ─────────────────────────────────────────────

    private JButton createStyledButton(String text, Color normalColor, Color hoverColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill;
                if (!isEnabled()) {
                    fill = new Color(225, 228, 232);
                    setForeground(new Color(145, 150, 160));
                } else if (getModel().isPressed()) {
                    fill = normalColor.darker();
                    setForeground(Color.WHITE);
                } else if (getModel().isRollover()) {
                    fill = hoverColor;
                    setForeground(Color.WHITE);
                } else {
                    fill = normalColor;
                    setForeground(Color.WHITE);
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

    private static class CleanDataCellRenderer extends DefaultTableCellRenderer {
        public CleanDataCellRenderer(int horizontalAlignment) {
            setHorizontalAlignment(horizontalAlignment);
            setBorder(new EmptyBorder(0, 8, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                c.setForeground(new Color(30, 35, 45));
            }
            return c;
        }
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        public StatusBadgeRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("SansSerif", Font.BOLD, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (!isSelected) {
                String status = value == null ? "" : value.toString().toUpperCase();
                switch (status) {
                    case "PAID" -> {
                        c.setBackground(new Color(225, 246, 228));
                        c.setForeground(new Color(20, 110, 35));
                    }
                    case "PENDING" -> {
                        c.setBackground(new Color(254, 243, 215));
                        c.setForeground(new Color(160, 85, 0));
                    }
                    case "PARTIAL" -> {
                        c.setBackground(new Color(254, 236, 210));
                        c.setForeground(new Color(175, 75, 0));
                    }
                    case "WAIVED" -> {
                        c.setBackground(new Color(240, 242, 245));
                        c.setForeground(new Color(110, 115, 125));
                    }
                    default -> {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }
            }
            return c;
        }
    }
}
