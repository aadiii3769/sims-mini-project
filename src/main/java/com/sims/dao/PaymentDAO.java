package com.sims.dao;

import com.sims.model.Payment;
import com.sims.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code PAYMENT} table.
 *
 * <p>Handles fee invoice creation, payment recording, and receipt lookup.
 * Outstanding balance is computed by the model's
 * {@link Payment#getOutstandingBalance()} rather than stored in the DB.
 */
public class PaymentDAO {

    private final Connection conn;

    public PaymentDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String SQL_FIND_BY_STUDENT =
        "SELECT p.TRANSACTION_ID, p.STUDENT_ID, p.FEE_TYPE, p.AMOUNT_DUE, " +
        "       p.AMOUNT_PAID, p.PAYMENT_STATUS, p.PAYMENT_DATE, p.DUE_DATE, " +
        "       p.PAYMENT_MODE, p.RECEIPT_NUMBER, p.REMARKS, u.FULL_NAME " +
        "FROM   PAYMENT p " +
        "JOIN   STUDENT st ON p.STUDENT_ID = st.STUDENT_ID " +
        "JOIN   USERS u    ON st.USER_ID   = u.USER_ID " +
        "WHERE  p.STUDENT_ID = ? " +
        "ORDER BY p.DUE_DATE DESC";

    private static final String SQL_FIND_BY_RECEIPT =
        "SELECT p.TRANSACTION_ID, p.STUDENT_ID, p.FEE_TYPE, p.AMOUNT_DUE, " +
        "       p.AMOUNT_PAID, p.PAYMENT_STATUS, p.PAYMENT_DATE, p.DUE_DATE, " +
        "       p.PAYMENT_MODE, p.RECEIPT_NUMBER, p.REMARKS, u.FULL_NAME " +
        "FROM   PAYMENT p " +
        "JOIN   STUDENT st ON p.STUDENT_ID = st.STUDENT_ID " +
        "JOIN   USERS u    ON st.USER_ID   = u.USER_ID " +
        "WHERE  p.RECEIPT_NUMBER = ?";

    private static final String SQL_INSERT =
        "INSERT INTO PAYMENT (STUDENT_ID, FEE_TYPE, AMOUNT_DUE, AMOUNT_PAID, " +
        "                     PAYMENT_STATUS, PAYMENT_DATE, DUE_DATE, " +
        "                     PAYMENT_MODE, RECEIPT_NUMBER, REMARKS) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_PAYMENT =
        "UPDATE PAYMENT " +
        "SET AMOUNT_PAID = ?, PAYMENT_STATUS = ?, PAYMENT_DATE = ?, " +
        "    PAYMENT_MODE = ?, RECEIPT_NUMBER = ? " +
        "WHERE TRANSACTION_ID = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    public List<Payment> findByStudent(long studentId) throws SQLException {
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Optional<Payment> findByReceiptNumber(String receiptNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_RECEIPT)) {
            ps.setString(1, receiptNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public void insert(Payment payment) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setLong(1, payment.getStudentId());
            ps.setString(2, payment.getFeeType());
            ps.setBigDecimal(3, payment.getAmountDue());
            ps.setBigDecimal(4, payment.getAmountPaid());
            ps.setString(5, payment.getPaymentStatus());
            if (payment.getPaymentDate() != null) {
                ps.setDate(6, Date.valueOf(payment.getPaymentDate()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            ps.setDate(7, Date.valueOf(payment.getDueDate()));
            ps.setString(8, payment.getPaymentMode());
            ps.setString(9, payment.getReceiptNumber());
            ps.setString(10, payment.getRemarks());
            ps.executeUpdate();
        }
    }

    /**
     * Records an actual payment against an existing invoice.
     *
     * @param transactionId the TRANSACTION_ID of the fee row
     * @param amountPaid    total amount paid (may be partial)
     * @param mode          payment mode (ONLINE | CASH | DD | WAIVER)
     * @param receiptNo     generated receipt number
     */
    public int recordPayment(long transactionId, BigDecimal amountPaid,
                             BigDecimal amountDue, String mode, String receiptNo)
            throws SQLException {
        String status = amountPaid.compareTo(amountDue) >= 0 ? "PAID" : "PARTIAL";
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PAYMENT)) {
            ps.setBigDecimal(1, amountPaid);
            ps.setString(2, status);
            ps.setDate(3, Date.valueOf(java.time.LocalDate.now()));
            ps.setString(4, mode);
            ps.setString(5, receiptNo);
            ps.setLong(6, transactionId);
            return ps.executeUpdate();
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setTransactionId(rs.getLong("TRANSACTION_ID"));
        p.setStudentId(rs.getLong("STUDENT_ID"));
        p.setFeeType(rs.getString("FEE_TYPE"));
        p.setAmountDue(rs.getBigDecimal("AMOUNT_DUE"));
        p.setAmountPaid(rs.getBigDecimal("AMOUNT_PAID"));
        p.setPaymentStatus(rs.getString("PAYMENT_STATUS"));
        Date pd = rs.getDate("PAYMENT_DATE");
        if (pd != null) p.setPaymentDate(pd.toLocalDate());
        Date dd = rs.getDate("DUE_DATE");
        if (dd != null) p.setDueDate(dd.toLocalDate());
        p.setPaymentMode(rs.getString("PAYMENT_MODE"));
        p.setReceiptNumber(rs.getString("RECEIPT_NUMBER"));
        p.setRemarks(rs.getString("REMARKS"));
        p.setStudentName(rs.getString("FULL_NAME"));
        return p;
    }
}
