package com.sims.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * POJO mapping to the {@code PAYMENT} database table.
 */
public class Payment {

    private long       transactionId;
    private long       studentId;
    private String     feeType;          // TUITION | HOSTEL | EXAM | BUS | OTHER
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private String     paymentStatus;    // PAID | PENDING | PARTIAL | WAIVED
    private LocalDate  paymentDate;
    private LocalDate  dueDate;
    private String     paymentMode;      // ONLINE | CASH | DD | WAIVER
    private String     receiptNumber;
    private String     remarks;

    // Joined field
    private String     studentName;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Payment() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getTransactionId()                          { return transactionId; }
    public void setTransactionId(long transactionId)        { this.transactionId = transactionId; }

    public long getStudentId()                              { return studentId; }
    public void setStudentId(long studentId)                { this.studentId = studentId; }

    public String getFeeType()                              { return feeType; }
    public void setFeeType(String feeType)                  { this.feeType = feeType; }

    public BigDecimal getAmountDue()                        { return amountDue; }
    public void setAmountDue(BigDecimal amountDue)          { this.amountDue = amountDue; }

    public BigDecimal getAmountPaid()                       { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid)        { this.amountPaid = amountPaid; }

    public String getPaymentStatus()                        { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus)      { this.paymentStatus = paymentStatus; }

    public LocalDate getPaymentDate()                       { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate)       { this.paymentDate = paymentDate; }

    public LocalDate getDueDate()                           { return dueDate; }
    public void setDueDate(LocalDate dueDate)               { this.dueDate = dueDate; }

    public String getPaymentMode()                          { return paymentMode; }
    public void setPaymentMode(String paymentMode)          { this.paymentMode = paymentMode; }

    public String getReceiptNumber()                        { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber)      { this.receiptNumber = receiptNumber; }

    public String getRemarks()                              { return remarks; }
    public void setRemarks(String remarks)                  { this.remarks = remarks; }

    public String getStudentName()                          { return studentName; }
    public void setStudentName(String studentName)          { this.studentName = studentName; }

    /**
     * Convenience: returns outstanding balance.
     * Computed at runtime — not persisted (3NF).
     */
    public BigDecimal getOutstandingBalance() {
        if (amountDue == null) return BigDecimal.ZERO;
        if (amountPaid == null) return amountDue;
        if (amountPaid.compareTo(amountDue) >= 0) {
            return BigDecimal.ZERO;
        }
        return amountDue.subtract(amountPaid);
    }
}
