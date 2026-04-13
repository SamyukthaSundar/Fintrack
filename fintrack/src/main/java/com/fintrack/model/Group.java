package com.fintrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Group Entity — Owner: Saanvi Kakkar
 */
@Entity
@Table(name = "groups_table")
@EntityListeners(AuditingEntityListener.class)
public class Group {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private String currency = "INR";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<GroupMember> members = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Expense> expenses = new HashSet<>();

    public Group() {}

    // Getters
    public Long getId()                   { return id; }
    public String getName()               { return name; }
    public String getDescription()        { return description; }
    public User getCreatedBy()            { return createdBy; }
    public String getCurrency()           { return currency; }
    public Boolean getIsActive()          { return isActive; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getUpdatedAt()   { return updatedAt; }
    public Set<GroupMember> getMembers()  { return members; }
    public Set<Expense> getExpenses()     { return expenses; }

    // Setters
    public void setId(Long v)             { this.id = v; }
    public void setName(String v)         { this.name = v; }
    public void setDescription(String v)  { this.description = v; }
    public void setCreatedBy(User v)      { this.createdBy = v; }
    public void setCurrency(String v)     { this.currency = v; }
    public void setIsActive(Boolean v)    { this.isActive = v; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Group g = new Group();
        public Builder name(String v)         { g.name = v;        return this; }
        public Builder description(String v)  { g.description = v; return this; }
        public Builder createdBy(User v)      { g.createdBy = v;   return this; }
        public Builder currency(String v)     { g.currency = v;    return this; }
        public Builder isActive(Boolean v)    { g.isActive = v;    return this; }
        public Group build()                  { return g; }
    }
}
