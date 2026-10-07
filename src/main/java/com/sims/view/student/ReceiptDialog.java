package com.sims.view.student;

import com.sims.model.Payment;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;

/**
 * Modal dialog displaying an official fee receipt with print capability.
 *
 * <h2>Design Pattern – Behavioral: Observer</h2>
 * <p><b>Academic Justification</b>: The "Print Receipt" and "Close" buttons register
 * {@link java.awt.event.ActionListener} instances (Observer pattern). User interactions
 * trigger print execution and dialog disposal without coupling view presentation to
 * the caller.</p>
 *
 * <h2>Layout Rules</h2>
 * <p>Uses standard {@link BorderLayout}, {@link GridBagLayout}, and {@link FlowLayout}
 * with empty borders for padding per AGENTS.md Section 6.</p>
 */
public class ReceiptDialog extends JDialog {

    private final Payment payment;
    private JTextArea receiptTextArea;

    public ReceiptDialog(Frame owner, Payment payment) {
        super(owner, "Official Fee Receipt - SIMS", true);
        this.payment = payment;
        initUI();
    }

    public ReceiptDialog(Dialog owner, Payment payment) {
        super(owner, "Official Fee Receipt - SIMS", true);
        this.payment = payment;
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 620);
        setMinimumSize(new Dimension(460, 500));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(25, 50, 85));
        header.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("STUDENT INFORMATION MANAGEMENT SYSTEM", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("OFFICIAL FEE PAYMENT RECEIPT", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(200, 220, 255));

        header.add(title, BorderLayout.NORTH);
        header.add(sub,   BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setBorder(new EmptyBorder(16, 20, 10, 20));
        wrapper.setBackground(Color.WHITE);

        // Visual receipt card
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(210, 215, 225), 1, true),
            new EmptyBorder(14, 16, 14, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addReceiptField(card, gbc, row++, "Receipt Number:",
            payment.getReceiptNumber() != null ? payment.getReceiptNumber() : "N/A", true, new Color(20, 80, 160));
        addReceiptField(card, gbc, row++, "Transaction ID:",
            String.valueOf(payment.getTransactionId()), false, Color.DARK_GRAY);
        addReceiptField(card, gbc, row++, "Student Name:",
            payment.getStudentName() != null ? payment.getStudentName() : "Student #" + payment.getStudentId(), false, Color.BLACK);
        addReceiptField(card, gbc, row++, "Student ID:",
            String.valueOf(payment.getStudentId()), false, Color.DARK_GRAY);
        addReceiptField(card, gbc, row++, "Fee Category:",
            payment.getFeeType(), false, Color.BLACK);

        String paymentDateStr = payment.getPaymentDate() != null
            ? payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
            : java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        addReceiptField(card, gbc, row++, "Payment Date:", paymentDateStr, false, Color.BLACK);

        String dueDateStr = payment.getDueDate() != null
            ? payment.getDueDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
            : "N/A";
        addReceiptField(card, gbc, row++, "Due Date:", dueDateStr, false, Color.BLACK);

        addReceiptField(card, gbc, row++, "Payment Mode:",
            payment.getPaymentMode() != null ? payment.getPaymentMode() : "ONLINE", false, Color.BLACK);

        addReceiptField(card, gbc, row++, "Amount Due:",
            "Rs. " + String.format("%.2f", payment.getAmountDue()), false, Color.DARK_GRAY);

        addReceiptField(card, gbc, row++, "Amount Paid:",
            "Rs. " + String.format("%.2f", payment.getAmountPaid()), true, new Color(25, 120, 40));

        addReceiptField(card, gbc, row++, "Outstanding Balance:",
            "Rs. " + String.format("%.2f", payment.getOutstandingBalance()), false,
            payment.getOutstandingBalance().signum() > 0 ? new Color(180, 40, 40) : Color.DARK_GRAY);

        addReceiptField(card, gbc, row++, "Payment Status:",
            payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "PAID", true,
            "PAID".equalsIgnoreCase(payment.getPaymentStatus()) ? new Color(25, 120, 40) : new Color(180, 100, 20));

        if (payment.getRemarks() != null && !payment.getRemarks().isBlank()) {
            addReceiptField(card, gbc, row++, "Remarks:", payment.getRemarks(), false, Color.DARK_GRAY);
        }

        // Hidden printable text area initialized with receipt content
        receiptTextArea = new JTextArea(buildPlainTextReceipt());
        receiptTextArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        receiptTextArea.setEditable(false);

        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private void addReceiptField(JPanel panel, GridBagConstraints gbc, int row,
                                 String labelText, String valueText, boolean bold, Color valueColor) {
        gbc.gridy = row;

        gbc.gridx  = 0;
        gbc.weightx = 0.35;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(new Color(90, 95, 105));
        panel.add(lbl, gbc);

        gbc.gridx  = 1;
        gbc.weightx = 0.65;
        JLabel val = new JLabel(valueText);
        val.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 12));
        val.setForeground(valueColor);
        panel.add(val, gbc);
    }

    private JPanel buildActions() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBackground(new Color(245, 247, 250));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)));

        JButton printBtn = createStyledButton("Print Receipt", new Color(30, 85, 155), new Color(45, 105, 185));
        printBtn.addActionListener(e -> handlePrint());

        JButton closeBtn = createStyledButton("Close", new Color(110, 115, 125), new Color(130, 135, 145));
        closeBtn.addActionListener(e -> dispose());

        bar.add(printBtn);
        bar.add(closeBtn);
        return bar;
    }

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

    private void handlePrint() {
        try {
            MessageFormat header = new MessageFormat("SIMS Fee Receipt - " + payment.getReceiptNumber());
            MessageFormat footer = new MessageFormat("- Page {0} -");
            boolean complete = receiptTextArea.print(header, footer, true, null, null, true);
            if (complete) {
                JOptionPane.showMessageDialog(this,
                    "Receipt print job sent successfully.",
                    "Print Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                "Printing could not be completed:\n" + ex.getMessage(),
                "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildPlainTextReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("         STUDENT INFORMATION MANAGEMENT SYSTEM (SIMS)        \n");
        sb.append("                   OFFICIAL PAYMENT RECEIPT                  \n");
        sb.append("============================================================\n\n");
        sb.append(String.format("Receipt Number    : %s\n", payment.getReceiptNumber()));
        sb.append(String.format("Transaction ID    : %d\n", payment.getTransactionId()));
        sb.append(String.format("Student Name      : %s\n", payment.getStudentName() != null ? payment.getStudentName() : ""));
        sb.append(String.format("Student ID        : %d\n", payment.getStudentId()));
        sb.append(String.format("Fee Category      : %s\n", payment.getFeeType()));
        sb.append(String.format("Payment Date      : %s\n", payment.getPaymentDate()));
        sb.append(String.format("Due Date          : %s\n", payment.getDueDate()));
        sb.append(String.format("Payment Mode      : %s\n", payment.getPaymentMode()));
        sb.append(String.format("Amount Due        : Rs. %.2f\n", payment.getAmountDue()));
        sb.append(String.format("Amount Paid       : Rs. %.2f\n", payment.getAmountPaid()));
        sb.append(String.format("Outstanding Bal   : Rs. %.2f\n", payment.getOutstandingBalance()));
        sb.append(String.format("Payment Status    : %s\n", payment.getPaymentStatus()));
        if (payment.getRemarks() != null && !payment.getRemarks().isBlank()) {
            sb.append(String.format("Remarks           : %s\n", payment.getRemarks()));
        }
        sb.append("\n============================================================\n");
        sb.append("          Thank you for your payment. System Generated.       \n");
        sb.append("============================================================\n");
        return sb.toString();
    }
}
