package com.fintrack.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ExpenseCreateDto {

    @NotNull private Long groupId;
    @NotNull private Long paidById;
    @NotBlank @Size(max = 200) private String title;
    private String description;
    @DecimalMin("0.01") private BigDecimal totalAmount;
    @NotBlank private String splitType;
    @NotBlank private String category;
    private LocalDate expenseDate;
    private List<Long> participantIds;
    private Map<Long, BigDecimal> splitData;

    public ExpenseCreateDto() {}

    public Long getGroupId()                       { return groupId; }
    public Long getPaidById()                      { return paidById; }
    public String getTitle()                       { return title; }
    public String getDescription()                 { return description; }
    public BigDecimal getTotalAmount()             { return totalAmount; }
    public String getSplitType()                   { return splitType; }
    public String getCategory()                    { return category; }
    public LocalDate getExpenseDate()              { return expenseDate; }
    public List<Long> getParticipantIds()          { return participantIds; }
    public Map<Long, BigDecimal> getSplitData()    { return splitData; }

    public void setGroupId(Long v)                 { this.groupId = v; }
    public void setPaidById(Long v)                { this.paidById = v; }
    public void setTitle(String v)                 { this.title = v; }
    public void setDescription(String v)           { this.description = v; }
    public void setTotalAmount(BigDecimal v)       { this.totalAmount = v; }
    public void setSplitType(String v)             { this.splitType = v; }
    public void setCategory(String v)              { this.category = v; }
    public void setExpenseDate(LocalDate v)        { this.expenseDate = v; }
    public void setParticipantIds(List<Long> v)    { this.participantIds = v; }
    public void setSplitData(Map<Long, BigDecimal> v) { this.splitData = v; }
}
