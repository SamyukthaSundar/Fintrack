package com.fintrack.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ExpenseSplit Entity — Owner: Samyuktha S
 */
@Entity
@Table(name = "expense_splits",
       uniqueConstraints = @UniqueConstraint(columnNames = {"expense_id", "user_id"}))
public class ExpenseSplit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(name = "settled_amount", precision = 12, scale = 2)
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ExpenseSplit() {}

    // Getters
    public Long getId()               { return id; }
    public Expense getExpense()       { return expense; }
    public User getUser()             { return user; }
    public BigDecimal getAmount()     { return amount; }
    public BigDecimal getPercentage() { return percentage; }
    public BigDecimal getWeight()     { return weight; }
    public Boolean getIsPaid()        { return isPaid; }
    public BigDecimal getSettledAmount() { return settledAmount != null ? settledAmount : BigDecimal.ZERO; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Returns the remaining unpaid amount for this split.
     * This accounts for partial settlements.
     */
    public BigDecimal getRemainingAmount() {
        BigDecimal settled = settledAmount != null ? settledAmount : BigDecimal.ZERO;
        return amount.subtract(settled).max(BigDecimal.ZERO);
    }

    /**
     * Records a settlement against this split.
     * Returns the amount actually applied (may be less than requested if split is almost paid).
     */
    public BigDecimal recordSettlement(BigDecimal settlementAmount) {
        BigDecimal remaining = getRemainingAmount();
        BigDecimal toApply = settlementAmount.min(remaining);
        settledAmount = getSettledAmount().add(toApply);
        if (getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            isPaid = true;
        }
        return toApply;
    }

    // Setters
    public void setId(Long v)               { this.id = v; }
    public void setExpense(Expense v)       { this.expense = v; }
    public void setUser(User v)             { this.user = v; }
    public void setAmount(BigDecimal v)     { this.amount = v; }
    public void setPercentage(BigDecimal v) { this.percentage = v; }
    public void setWeight(BigDecimal v)     { this.weight = v; }
    public void setIsPaid(Boolean v)        { this.isPaid = v; }
    public void setSettledAmount(BigDecimal v) { this.settledAmount = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final ExpenseSplit s = new ExpenseSplit();
        public Builder expense(Expense v)       { s.expense = v;    return this; }
        public Builder user(User v)             { s.user = v;       return this; }
        public Builder amount(BigDecimal v)     { s.amount = v;     return this; }
        public Builder percentage(BigDecimal v) { s.percentage = v; return this; }
        public Builder weight(BigDecimal v)     { s.weight = v;     return this; }
        public Builder settledAmount(BigDecimal v) { s.settledAmount = v; return this; }
        public ExpenseSplit build()             { return s; }
    }
}
