package com.fintrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification Entity — Observer Pattern target
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "ref_type")
    private String refType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum NotificationType {
        EXPENSE_ADDED, SETTLEMENT_REQUEST, SETTLEMENT_VERIFIED,
        SETTLEMENT_REJECTED, GROUP_INVITE, DEBT_REMINDER
    }

    public Notification() {}

    // Getters
    public Long getId()                   { return id; }
    public User getUser()                 { return user; }
    public NotificationType getType()     { return type; }
    public String getTitle()              { return title; }
    public String getMessage()            { return message; }
    public Boolean getIsRead()            { return isRead; }
    public Long getRefId()                { return refId; }
    public String getRefType()            { return refType; }
    public LocalDateTime getCreatedAt()   { return createdAt; }

    // Setters
    public void setId(Long v)                  { this.id = v; }
    public void setUser(User v)                { this.user = v; }
    public void setType(NotificationType v)    { this.type = v; }
    public void setTitle(String v)             { this.title = v; }
    public void setMessage(String v)           { this.message = v; }
    public void setIsRead(Boolean v)           { this.isRead = v; }
    public void setRefId(Long v)               { this.refId = v; }
    public void setRefType(String v)           { this.refType = v; }
    public void setCreatedAt(LocalDateTime v)  { this.createdAt = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Notification n = new Notification();
        public Builder user(User v)               { n.user = v;    return this; }
        public Builder type(NotificationType v)   { n.type = v;    return this; }
        public Builder title(String v)            { n.title = v;   return this; }
        public Builder message(String v)          { n.message = v; return this; }
        public Builder refId(Long v)              { n.refId = v;   return this; }
        public Builder refType(String v)          { n.refType = v; return this; }
        public Notification build()               { return n; }
    }
}
