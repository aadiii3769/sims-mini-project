package com.sims.view.student;

import com.sims.controller.PaymentController;
import com.sims.model.Payment;
import com.sims.model.Student;
import com.sims.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.util.EventObject;
import java.util.List;
import java.util.Optional;

/**
 * Student and Parent panel for viewing fee records, paying pending invoices,
 * and generating/viewing official receipts.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: The "Pay Now" and "View Receipt" table buttons,
 * along with the "Refresh" action button, emit Swing action events handled by
 * {@link java.awt.event.ActionListener} observers. These listeners gather parameters
 * from the user and delegate to {@link PaymentController}, cleanly separating the
 * GUI from domain and persistence logic per AGENTS.md Section 3.</p>
 *
 * <h2>Threading</h2>
 * <p>All database queries and payment mutations execute inside {@link SwingWorker}
 * off the Event Dispatch Thread (EDT). UI updates occur strictly in {@code done()}
 * on the EDT per AGENTS.md Section 5.</p>
 *
 * <h2>Layout Rules</h2>
 * <p>Built entirely using standard layout managers (BorderLayout, FlowLayout) without
 * any null layouts, per AGENTS.md Section 6.</p>
 */
public class PaymentPanel extends JPanel {

    private static final String[] COLS = {
        "Type", "Amount Due (Rs)", "Paid (Rs)", "Outstanding (Rs)", "Status", "Due Date", "Mode", "Receipt No.", "Action"
    };

    private final User              viewer;
    private final PaymentController paymentController;
    private final DefaultTableModel tableModel;
    private final JTable            table;
    private final JLabel            studentLabel;
    private final JLabel            outstandingLabel;
    private final JLabel            statusLabel;
    private final JButton           paySelectedBtn;

    private Student       currentStudent;
    private List<Payment> currentPayments;

    public PaymentPanel(User viewer) {
        super(new BorderLayout(0, 8));
        this.viewer            = viewer;
        this.paymentController = new PaymentController();
        this.tableModel        = buildTableModel();
        this.table             = buildTable();
        this.studentLabel      = new JLabel("Loading student profile...");
        this.outstandingLabel  = new JLabel("Outstanding: Rs —");
        this.statusLabel       = new JLabel("Loading fee records...");
        this.paySelectedBtn    = new JButton("Pay Selected");

        buildUI();
        loadStudentAndFees();
    }

    private void buildUI() {
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(buildToolbar(),         BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildBottomBar(),       BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        studentLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        paySelectedBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        paySelectedBtn.setBackground(new Color(25, 120, 40));
        paySelectedBtn.setForeground(Color.WHITE);
        paySelectedBtn.setFocusPainted(false);
        paySelectedBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                handleRowAction(row);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Please select an invoice row to pay.",
                    "No Invoice Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton refresh = new JButton("Refresh");
        refresh.setFont(new Font("SansSerif", Font.PLAIN, 12));
        refresh.addActionListener(e -> refreshFees());

        right.add(paySelectedBtn);
        right.add(refresh);

        bar.add(studentLabel, BorderLayout.WEST);
        bar.add(right,        BorderLayout.EAST);
        return bar;
    }

    private JPanel buildBottomBar() {
        JPanel outer = new JPanel(new BorderLayout(0, 4));

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        summaryPanel.setBackground(new Color(238, 244, 255));
        summaryPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(190, 215, 250)));

        outstandingLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        outstandingLabel.setForeground(new Color(160, 40, 40));
        summaryPanel.add(outstandingLabel);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusPanel.add(statusLabel);

        outer.add(summaryPanel, BorderLayout.NORTH);
        outer.add(statusPanel,  BorderLayout.SOUTH);
        return outer;
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setRowHeight(32);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom status-aware renderer for standard columns
        StatusRowRenderer statusRenderer = new StatusRowRenderer();
        for (int i = 0; i < 8; i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(statusRenderer);
        }

        // Action column with custom button renderer and editor
        ActionCellRendererEditor actionHandler = new ActionCellRendererEditor();
        t.getColumnModel().getColumn(8).setCellRenderer(actionHandler);
        t.getColumnModel().getColumn(8).setCellEditor(actionHandler);
        t.getColumnModel().getColumn(8).setPreferredWidth(120);

        // Double-click row shortcut
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = t.getSelectedRow();
                    if (row >= 0) handleRowAction(row);
                }
            }
        });

        return t;
    }

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLS, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 8; // Only the Action button column is interactive
            }
        };
    }

    private void loadStudentAndFees() {
        new SwingWorker<Optional<Student>, Void>() {
            @Override
            protected Optional<Student> doInBackground() {
                return paymentController.findStudentForViewer(viewer);
            }
            @Override
            protected void done() {
                try {
                    Optional<Student> opt = get();
                    if (opt.isEmpty()) {
                        studentLabel.setText("No linked student profile found.");
                        statusLabel.setText("Fee records unavailable.");
                        paySelectedBtn.setEnabled(false);
                        return;
                    }
                    currentStudent = opt.get();
                    studentLabel.setText("Student: " + currentStudent.getRollNumber()
                        + " — " + currentStudent.getFullName()
                        + " (" + currentStudent.getDepartment() + ")");
                    refreshFees();
                } catch (Exception ex) {
                    statusLabel.setText("Error loading profile.");
                    JOptionPane.showMessageDialog(PaymentPanel.this,
                        "Failed to load student profile:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshFees() {
        if (currentStudent == null) return;
        statusLabel.setText("Loading fee records...");
        tableModel.setRowCount(0);

        new SwingWorker<List<Payment>, Void>() {
            @Override
            protected List<Payment> doInBackground() {
                return paymentController.getStudentFees(currentStudent.getStudentId());
            }
            @Override
            protected void done() {
                try {
                    currentPayments = get();
                    for (Payment p : currentPayments) {
                        String actionText;
                        String status = p.getPaymentStatus();
                        if ("PENDING".equalsIgnoreCase(status) || "PARTIAL".equalsIgnoreCase(status)) {
                            actionText = "Pay Now";
                        } else if ("PAID".equalsIgnoreCase(status)) {
                            actionText = "View Receipt";
                        } else {
                            actionText = "Details";
                        }

                        tableModel.addRow(new Object[]{
                            p.getFeeType(),
                            String.format("%.2f", p.getAmountDue()),
                            String.format("%.2f", p.getAmountPaid()),
                            String.format("%.2f", p.getOutstandingBalance()),
                            p.getPaymentStatus(),
                            p.getDueDate() != null ? p.getDueDate().toString() : "-",
                            p.getPaymentMode() != null ? p.getPaymentMode() : "-",
                            p.getReceiptNumber() != null ? p.getReceiptNumber() : "-",
                            actionText
                        });
                    }

                    BigDecimal outstanding = paymentController.getTotalOutstanding(currentStudent.getStudentId());
                    outstandingLabel.setText("Total Outstanding Balance: Rs " + String.format("%.2f", outstanding));
                    statusLabel.setText(currentPayments.size() + " fee record(s) loaded. Click 'Pay Now' on pending invoices.");
                } catch (Exception ex) {
                    statusLabel.setText("Error loading fees.");
                    JOptionPane.showMessageDialog(PaymentPanel.this,
                        "Failed to load fee records:\n" + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleRowAction(int row) {
        if (currentPayments == null || row < 0 || row >= currentPayments.size()) return;
        Payment p = currentPayments.get(row);
        String status = p.getPaymentStatus();

        if ("PAID".equalsIgnoreCase(status)) {
            showReceiptDialog(p);
        } else if ("WAIVED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this,
                "This invoice has been waived by the administration.",
                "Invoice Waived", JOptionPane.INFORMATION_MESSAGE);
        } else {
            promptAndProcessPayment(p);
        }
    }

    private void promptAndProcessPayment(Payment p) {
        BigDecimal balance = p.getOutstandingBalance();
        String amtStr = JOptionPane.showInputDialog(this,
            "Invoice: " + p.getFeeType() + "\n"
            + "Total Due: Rs. " + String.format("%.2f", p.getAmountDue()) + "\n"
            + "Already Paid: Rs. " + String.format("%.2f", p.getAmountPaid()) + "\n"
            + "Outstanding: Rs. " + String.format("%.2f", balance) + "\n\n"
            + "Enter payment amount:",
            String.format("%.2f", balance));

        if (amtStr == null) return; // User cancelled

        BigDecimal amount;
        try {
            amount = new BigDecimal(amtStr.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
            if (amount.compareTo(balance) > 0) {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Entered amount (Rs. " + amount + ") exceeds current balance (Rs. " + balance + "). Proceed?",
                    "Confirm Overpayment", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid positive numerical amount.",
                "Invalid Amount", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] modes = {"ONLINE", "CASH", "DD", "WAIVER"};
        String mode = (String) JOptionPane.showInputDialog(this,
            "Select Payment Mode:", "Payment Mode",
            JOptionPane.PLAIN_MESSAGE, null, modes, modes[0]);
        if (mode == null) return; // User cancelled

        final BigDecimal finalAmount = amount;
        final String finalMode = mode;

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return paymentController.processPayment(
                    p.getTransactionId(), finalAmount, p.getAmountDue(), finalMode);
            }
            @Override
            protected void done() {
                try {
                    String receiptNo = get();
                    // Update model copy for immediate receipt display
                    p.setAmountPaid(finalAmount);
                    p.setPaymentMode(finalMode);
                    p.setReceiptNumber(receiptNo);
                    p.setPaymentDate(java.time.LocalDate.now());
                    p.setPaymentStatus(finalAmount.compareTo(p.getAmountDue()) >= 0 ? "PAID" : "PARTIAL");
                    p.setStudentName(currentStudent != null ? currentStudent.getFullName() : "");

                    refreshFees();
                    showReceiptDialog(p);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PaymentPanel.this,
                        "Payment processing failed:\n" + ex.getMessage(),
                        "Payment Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void showReceiptDialog(Payment p) {
        Window parentWin = SwingUtilities.getWindowAncestor(this);
        ReceiptDialog rd;
        if (parentWin instanceof Frame frame) {
            rd = new ReceiptDialog(frame, p);
        } else if (parentWin instanceof Dialog dialog) {
            rd = new ReceiptDialog(dialog, p);
        } else {
            rd = new ReceiptDialog((Frame) null, p);
        }
        rd.setVisible(true);
    }

    // ── Table Renderers & Editors ──────────────────────────────────────────────

    /** Status row color coding. */
    private static class StatusRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
            if (!isSelected) {
                Object statusVal = table.getModel().getValueAt(row, 4);
                String status = statusVal == null ? "" : statusVal.toString();
                switch (status.toUpperCase()) {
                    case "PAID"    -> { c.setBackground(new Color(230, 248, 230)); c.setForeground(new Color(25, 100, 30)); }
                    case "PENDING" -> { c.setBackground(new Color(255, 248, 225)); c.setForeground(new Color(140, 80, 0)); }
                    case "PARTIAL" -> { c.setBackground(new Color(255, 240, 215)); c.setForeground(new Color(150, 70, 0)); }
                    case "WAIVED"  -> { c.setBackground(new Color(245, 245, 245)); c.setForeground(Color.GRAY); }
                    default        -> { c.setBackground(Color.WHITE); c.setForeground(Color.BLACK); }
                }
            }
            return c;
        }
    }

    /**
     * Action column button renderer and editor.
     * Renders a styled button for each row and handles clicks.
     */
    private class ActionCellRendererEditor extends AbstractCellEditor
            implements TableCellRenderer, TableCellEditor {

        private final JButton button;
        private int currentRow = -1;

        public ActionCellRendererEditor() {
            this.button = new JButton();
            this.button.setFont(new Font("SansSerif", Font.BOLD, 11));
            this.button.setFocusPainted(false);
            this.button.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0) {
                    handleRowAction(currentRow);
                }
            });
        }

        private void configureButton(Object value) {
            String text = value != null ? value.toString() : "Action";
            button.setText(text);
            if ("Pay Now".equals(text)) {
                button.setBackground(new Color(30, 130, 50));
                button.setForeground(Color.WHITE);
            } else if ("View Receipt".equals(text)) {
                button.setBackground(new Color(30, 85, 160));
                button.setForeground(Color.WHITE);
            } else {
                button.setBackground(new Color(220, 225, 230));
                button.setForeground(Color.DARK_GRAY);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            configureButton(value);
            return button;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.currentRow = row;
            configureButton(value);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }
    }
}
