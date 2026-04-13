package com.fintrack.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Settlement Entity — Owner: Sanika Gupta
 * Design Pattern: State — submit/verify/reject transitions
 */
@Entity
@Table(name = "settlements")
@EntityListeners(AuditingEntityListener.class)
public class Settlement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_id", nullable = false)
    private User payee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(name = "payment_ref")
    private String paymentRef;

    @Column
    private String notes;

    @CreatedDate
    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    public enum SettlementStatus { PENDING, SUBMITTED, VERIFIED, REJECTED }
    public enum PaymentMethod    { CASH, UPI, BANK_TRANSFER, OTHER }

    public Settlement() {}

    // State Pattern transitions
    public void submit(String paymentRef, PaymentMethod method, String notes) {
        if (this.status != SettlementStatus.PENDING)
            throw new IllegalStateException("Can only submit a PENDING settlement.");
        this.paymentRef    = paymentRef;
        this.paymentMethod = method;
        this.notes         = notes;
        this.status        = SettlementStatus.SUBMITTED;
        this.submittedAt   = LocalDateTime.now();
    }

    public void verify(User verifier) {
        if (this.status != SettlementStatus.SUBMITTED)
            throw new IllegalStateException("Can only verify a SUBMITTED settlement.");
        this.status     = SettlementStatus.VERIFIED;
        this.verifiedBy = verifier;
        this.verifiedAt = LocalDateTime.now();
    }

    public void reject(User verifier, String reason) {
        if (this.status != SettlementStatus.SUBMITTED)
            throw new IllegalStateException("Can only reject a SUBMITTED settlement.");
        this.status     = SettlementStatus.REJECTED;
        this.verifiedBy = verifier;
        this.rejectedAt = LocalDateTime.now();
        this.notes      = reason;
    }

    // Getters
    public Long getId()                    { return id; }
    public Group getGroup()                { return group; }
    public User getPayer()                 { return payer; }
    public User getPayee()                 { return payee; }
    public BigDecimal getAmount()          { return amount; }
    public String getCurrency()            { return currency; }
    public SettlementStatus getStatus()    { return status; }
    public PaymentMethod getPaymentMethod(){ return paymentMethod; }
    public String getPaymentRef()          { return paymentRef; }
    public String getNotes()               { return notes; }
    public LocalDateTime getInitiatedAt()  { return initiatedAt; }
    public LocalDateTime getSubmittedAt()  { return submittedAt; }
    public LocalDateTime getVerifiedAt()   { return verifiedAt; }
    public LocalDateTime getRejectedAt()   { return rejectedAt; }
    public User getVerifiedBy()            { return verifiedBy; }

    // Setters
    public void setId(Long v)                    { this.id = v; }
    public void setGroup(Group v)                { this.group = v; }
    public void setPayer(User v)                 { this.payer = v; }
    public void setPayee(User v)                 { this.payee = v; }
    public void setAmount(BigDecimal v)          { this.amount = v; }
    public void setCurrency(String v)            { this.currency = v; }
    public void setStatus(SettlementStatus v)    { this.status = v; }
    public void setPaymentMethod(PaymentMethod v){ this.paymentMethod = v; }
    public void setPaymentRef(String v)          { this.paymentRef = v; }
    public void setNotes(String v)               { this.notes = v; }
    public void setInitiatedAt(LocalDateTime v)  { this.initiatedAt = v; }
    public void setVerifiedBy(User v)            { this.verifiedBy = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Settlement s = new Settlement();
        public Builder group(Group v)           { s.group = v;    return this; }
        public Builder payer(User v)            { s.payer = v;    return this; }
        public Builder payee(User v)            { s.payee = v;    return this; }
        public Builder amount(BigDecimal v)     { s.amount = v;   return this; }
        public Builder currency(String v)       { s.currency = v; return this; }
        public Builder status(SettlementStatus v){ s.status = v;  return this; }
        public Settlement build()               { return s; }
    }
}
