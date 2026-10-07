package com.sims.controller;

import com.sims.dao.PaymentDAO;
import com.sims.dao.StudentDAO;
import com.sims.model.Payment;
import com.sims.model.Student;
import com.sims.model.User;
import com.sims.model.UserRole;
import com.sims.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic controller for fee invoicing and payment processing.
 *
 * <h2>Design Pattern – Behavioral: Observer (indirect)</h2>
 * <p><b>Academic Justification</b>: View-layer ActionListener callbacks
 * (Observer pattern) trigger this controller when an admin creates an invoice or
 * a student submits a payment. All JDBC work and transaction demarcation are
 * confined here — the Views stay thin with zero persistence logic,
 * per AGENTS.md Section 3.</p>
 *
 * <h2>Design Pattern – Structural: Data Access Object (DAO) (consumer)</h2>
 * <p><b>Academic Justification</b>: Encapsulates database operations by delegating
 * CRUD actions to {@link PaymentDAO} and {@link StudentDAO}, decoupling
 * business rules from raw SQL statements.</p>
 *
 * <h2>Transaction Demarcation</h2>
 * <p>PaymentDAO executes DML but does NOT commit. This controller
 * calls {@code conn.commit()} after successful DAO operations, or
 * {@code conn.rollback()} on any failure, per AGENTS.md Section 4.3.</p>
 */
public class PaymentController {

    private final PaymentDAO paymentDAO;
    private final StudentDAO studentDAO;
    private final Connection conn;

    public PaymentController() {
        this.conn       = DBConnection.getInstance().getConnection();
        this.paymentDAO = new PaymentDAO();
        this.studentDAO = new StudentDAO();
    }

    /**
     * Creates a new fee invoice for a student.
     * Validates that amountDue is greater than 0, feeType and dueDate are specified,
     * initialises status to PENDING and amountPaid to 0, then commits atomically.
     *
     * @param invoice the fee invoice to persist
     * @throws IllegalArgumentException on validation failure
     * @throws RuntimeException on database failure; transaction is rolled back
     */
    public void createInvoice(Payment invoice) {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice is required.");
        }
        if (invoice.getStudentId() <= 0) {
            throw new IllegalArgumentException("Valid student ID is required.");
        }
        if (invoice.getFeeType() == null || invoice.getFeeType().isBlank()) {
            throw new IllegalArgumentException("Fee type is required.");
        }
        if (invoice.getAmountDue() == null || invoice.getAmountDue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount due must be greater than 0.");
        }
        if (invoice.getDueDate() == null) {
            throw new IllegalArgumentException("Due date is required.");
        }

        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.setPaymentStatus("PENDING");
        invoice.setPaymentDate(null);
        invoice.setPaymentMode(null);
        invoice.setReceiptNumber(null);

        try {
            paymentDAO.insert(invoice);
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new RuntimeException("Invoice creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Records an actual payment against an existing invoice.
     * Generates a unique receipt number, updates the invoice record,
     * and commits the transaction atomically.
     *
     * @param transactionId the invoice TRANSACTION_ID
     * @param amountPaid    payment amount
     * @param amountDue     the original amount due (for status computation)
     * @param mode          payment mode (ONLINE | CASH | DD | WAIVER)
     * @return the generated receipt number
     */
    public String processPayment(long transactionId, BigDecimal amountPaid,
                                 BigDecimal amountDue, String mode) {
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount paid must be greater than 0.");
        }
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("Payment mode is required.");
        }

        int year = LocalDate.now().getYear();
        String receiptNo = String.format("RCPT-%d-%06d", year, transactionId);

        try {
            paymentDAO.recordPayment(transactionId, amountPaid, amountDue, mode, receiptNo);
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
        return receiptNo;
    }

    /**
     * Overload: processes payment by looking up amountDue from DB if not supplied.
     *
     * @param transactionId the invoice TRANSACTION_ID
     * @param amountPaid    payment amount
     * @param mode          payment mode
     * @return the generated receipt number
     */
    public String processPayment(long transactionId, BigDecimal amountPaid, String mode) {
        BigDecimal amountDue = getAmountDueForTransaction(transactionId);
        return processPayment(transactionId, amountPaid, amountDue, mode);
    }

    /**
     * Retrieves all fee records for a student ordered by due date descending.
     *
     * @param studentId the student ID
     * @return list of Payment records
     */
    public List<Payment> getStudentFees(long studentId) {
        try {
            return paymentDAO.findByStudent(studentId);
        } catch (SQLException e) {
            throw new RuntimeException("Fee lookup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Computes the total outstanding fee balance for a student across all PENDING
     * and PARTIAL invoices.
     *
     * @param studentId the student ID
     * @return total outstanding balance
     */
    public BigDecimal getTotalOutstanding(long studentId) {
        try {
            return paymentDAO.findByStudent(studentId).stream()
                    .filter(p -> "PENDING".equalsIgnoreCase(p.getPaymentStatus())
                              || "PARTIAL".equalsIgnoreCase(p.getPaymentStatus()))
                    .map(Payment::getOutstandingBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (SQLException e) {
            throw new RuntimeException("Outstanding balance computation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves the linked Student entity for a logged-in user (STUDENT or PARENT).
     *
     * @param viewer the logged-in User
     * @return Optional containing the linked Student if found
     */
    public Optional<Student> findStudentForViewer(User viewer) {
        if (viewer == null || viewer.getRole() == null) {
            return Optional.empty();
        }
        try {
            if (viewer.getRole() == UserRole.STUDENT) {
                return studentDAO.findByUserId(viewer.getUserId());
            }
            if (viewer.getRole() == UserRole.PARENT) {
                return studentDAO.findByParentUserId(viewer.getUserId());
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Student lookup failed: " + e.getMessage(), e);
        }
    }

    private BigDecimal getAmountDueForTransaction(long transactionId) {
        String sql = "SELECT AMOUNT_DUE FROM PAYMENT WHERE TRANSACTION_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("AMOUNT_DUE");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query amount due for transaction " + transactionId + ": " + e.getMessage(), e);
        }
        throw new IllegalArgumentException("No payment record found with ID " + transactionId);
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            System.err.println("[WARN] Payment rollback failed: " + ex.getMessage());
        }
    }
}
