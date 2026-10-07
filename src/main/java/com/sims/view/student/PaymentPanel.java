package com.sims.view.student;

import com.sims.controller.PaymentController;
import com.sims.model.Payment;
import com.sims.model.Student;
import com.sims.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
        this.paySelectedBtn    = createStyledButton("Pay Selected", new Color(25, 120, 40), new Color(35, 150, 50));
        this.paySelectedBtn.setEnabled(false);

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

        paySelectedBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                handleRowAction(row);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Please select an invoice row from the table first.",
                    "No Invoice Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton refresh = createStyledButton("Refresh", new Color(60, 75, 95), new Color(80, 95, 115));
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
        t.setRowHeight(34);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setShowGrid(true);
        t.setGridColor(new Color(230, 233, 238));

        // Assign dedicated, contrast-rich cell renderers
        TableCellRenderer textRenderer = new CleanDataCellRenderer(SwingConstants.LEFT);
        TableCellRenderer numRenderer  = new CleanDataCellRenderer(SwingConstants.RIGHT);
        TableCellRenderer centerRenderer = new CleanDataCellRenderer(SwingConstants.CENTER);
        TableCellRenderer statusRenderer = new StatusBadgeRenderer();

        t.getColumnModel().getColumn(0).setCellRenderer(textRenderer);     // Type
        t.getColumnModel().getColumn(1).setCellRenderer(numRenderer);      // Due
        t.getColumnModel().getColumn(2).setCellRenderer(numRenderer);      // Paid
        t.getColumnModel().getColumn(3).setCellRenderer(numRenderer);      // Outstanding
        t.getColumnModel().getColumn(4).setCellRenderer(statusRenderer);   // Status
        t.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);   // Due Date
        t.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);   // Mode
        t.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);   // Receipt No

        // Action column with custom button renderer and editor
        ActionCellRendererEditor actionHandler = new ActionCellRendererEditor();
        t.getColumnModel().getColumn(8).setCellRenderer(actionHandler);
        t.getColumnModel().getColumn(8).setCellEditor(actionHandler);
        t.getColumnModel().getColumn(8).setPreferredWidth(130);

        // Update toolbar button when selection changes
        t.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = t.getSelectedRow();
                if (row >= 0 && currentPayments != null && row < currentPayments.size()) {
                    Payment p = currentPayments.get(row);
                    if ("PAID".equalsIgnoreCase(p.getPaymentStatus())) {
                        paySelectedBtn.setText("View Receipt");
                        paySelectedBtn.setEnabled(true);
                    } else if ("PENDING".equalsIgnoreCase(p.getPaymentStatus()) || "PARTIAL".equalsIgnoreCase(p.getPaymentStatus())) {
                        paySelectedBtn.setText("Pay Selected");
                        paySelectedBtn.setEnabled(true);
                    } else {
                        paySelectedBtn.setText("Pay Selected");
                        paySelectedBtn.setEnabled(false);
                    }
                } else {
                    paySelectedBtn.setText("Pay Selected");
                    paySelectedBtn.setEnabled(false);
                }
                paySelectedBtn.repaint();
            }
        });

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
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this,
                "This invoice has no outstanding balance to pay.",
                "Zero Balance", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String amtStr = JOptionPane.showInputDialog(this,
            "Invoice: " + p.getFeeType() + "\n"
            + "Total Due: Rs. " + String.format("%.2f", p.getAmountDue()) + "\n"
            + "Already Paid: Rs. " + String.format("%.2f", p.getAmountPaid()) + "\n"
            + "Outstanding Balance: Rs. " + String.format("%.2f", balance) + "\n\n"
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
                JOptionPane.showMessageDialog(this,
                    "Payment cannot exceed current outstanding balance (Rs. " + String.format("%.2f", balance) + ").",
                    "Excess Amount", JOptionPane.WARNING_MESSAGE);
                return;
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
                    BigDecimal newPaid = p.getAmountPaid().add(finalAmount);
                    p.setAmountPaid(newPaid);
                    p.setPaymentMode(finalMode);
                    p.setReceiptNumber(receiptNo);
                    p.setPaymentDate(java.time.LocalDate.now());
                    p.setPaymentStatus(newPaid.compareTo(p.getAmountDue()) >= 0 ? "PAID" : "PARTIAL");
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

    // ── Custom Painted Buttons (Cross-L&F Compatible) ──────────────────────────

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

    // ── Table Renderers & Editors ──────────────────────────────────────────────

    /** Renders data columns with high-contrast text and subtle row striping. */
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

    /** Status column badge renderer with rich, distinct status colors. */
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

    /**
     * Action column button renderer and editor.
     * Custom paints vibrant, accessible action buttons (Green "Pay Now" and Blue "View Receipt")
     * that render beautifully across all Swing Look and Feels.
     */
    private class ActionCellRendererEditor extends AbstractCellEditor
            implements TableCellRenderer, TableCellEditor {

        private final TableActionButton renderButton;
        private final TableActionButton editorButton;
        private int currentRow = -1;

        public ActionCellRendererEditor() {
            this.renderButton = new TableActionButton();
            this.editorButton = new TableActionButton();
            this.editorButton.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0) {
                    handleRowAction(currentRow);
                }
            });
        }

        private void configureButton(TableActionButton btn, Object value) {
            String text = value != null ? value.toString() : "Action";
            btn.setText(text);
            if ("Pay Now".equals(text)) {
                btn.setColors(new Color(25, 125, 45), new Color(35, 150, 55));
            } else if ("View Receipt".equals(text)) {
                btn.setColors(new Color(30, 95, 175), new Color(45, 115, 205));
            } else {
                btn.setColors(new Color(200, 205, 215), new Color(185, 190, 200));
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            configureButton(renderButton, value);
            return renderButton;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.currentRow = row;
            configureButton(editorButton, value);
            return editorButton;
        }

        @Override
        public Object getCellEditorValue() {
            return editorButton.getText();
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }
    }

    /** Custom painted table button with high visibility and hover states. */
    private static class TableActionButton extends JButton {
        private Color normalColor = new Color(25, 125, 45);
        private Color hoverColor  = new Color(35, 150, 55);

        public TableActionButton() {
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        }

        public void setColors(Color normal, Color hover) {
            this.normalColor = normal;
            this.hoverColor  = hover;
        }

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
            g2.fillRoundRect(4, 3, getWidth() - 8, getHeight() - 6, 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
