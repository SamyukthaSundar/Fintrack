package com.fintrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Expense Entity — Owner: Samyuktha S
 */
@Entity
@Table(name = "expenses")
@EntityListeners(AuditingEntityListener.class)
public class Expense {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @NotBlank @Size(max = 200)
    @Column(nullable = false)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull @DecimalMin("0.01")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false)
    private SplitType splitType = SplitType.EQUAL;

    @Enumerated(EnumType.STRING)
    private Category category = Category.OTHER;

    @Column(name = "receipt_image")
    private String receiptImage;

    @Column(name = "ocr_raw_text", columnDefinition = "TEXT")
    private String ocrRawText;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "is_settled", nullable = false)
    private Boolean isSettled = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<ExpenseSplit> splits = new HashSet<>();

    public enum SplitType  { EQUAL, PERCENTAGE, EXACT, WEIGHTED }
    public enum Category   { FOOD, TRANSPORT, ACCOMMODATION, ENTERTAINMENT, UTILITIES, SHOPPING, HEALTHCARE, OTHER }

    public Expense() {}

    // Getters
    public Long getId()             { return id; }
    public Group getGroup()         { return group; }
    public User getPaidBy()         { return paidBy; }
    public String getTitle()        { return title; }
    public String getDescription()  { return description; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency()     { return currency; }
    public SplitType getSplitType() { return splitType; }
    public Category getCategory()   { return category; }
    public String getReceiptImage() { return receiptImage; }
    public String getOcrRawText()   { return ocrRawText; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public Boolean getIsSettled()   { return isSettled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Set<ExpenseSplit> getSplits() { return splits; }

    // Setters
    public void setId(Long v)                { this.id = v; }
    public void setGroup(Group v)            { this.group = v; }
    public void setPaidBy(User v)            { this.paidBy = v; }
    public void setTitle(String v)           { this.title = v; }
    public void setDescription(String v)     { this.description = v; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }
    public void setCurrency(String v)        { this.currency = v; }
    public void setSplitType(SplitType v)    { this.splitType = v; }
    public void setCategory(Category v)      { this.category = v; }
    public void setReceiptImage(String v)    { this.receiptImage = v; }
    public void setOcrRawText(String v)      { this.ocrRawText = v; }
    public void setExpenseDate(LocalDate v)  { this.expenseDate = v; }
    public void setIsSettled(Boolean v)      { this.isSettled = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Expense e = new Expense();
        public Builder group(Group v)           { e.group = v;       return this; }
        public Builder paidBy(User v)           { e.paidBy = v;      return this; }
        public Builder title(String v)          { e.title = v;       return this; }
        public Builder description(String v)    { e.description = v; return this; }
        public Builder totalAmount(BigDecimal v){ e.totalAmount = v;  return this; }
        public Builder currency(String v)       { e.currency = v;    return this; }
        public Builder splitType(SplitType v)   { e.splitType = v;   return this; }
        public Builder category(Category v)     { e.category = v;    return this; }
        public Builder expenseDate(LocalDate v) { e.expenseDate = v; return this; }
        public Expense build()                  { return e; }
    }
}
