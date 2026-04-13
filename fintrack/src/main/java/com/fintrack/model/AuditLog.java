package com.fintrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * AuditLog Entity — Owner: Saanvi Kakkar
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public AuditLog() {}

    // Getters
    public Long getId()           { return id; }
    public User getUser()         { return user; }
    public String getAction()     { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId()     { return entityId; }
    public String getOldValue()   { return oldValue; }
    public String getNewValue()   { return newValue; }
    public String getIpAddress()  { return ipAddress; }
    public String getUserAgent()  { return userAgent; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // Setters
    public void setUser(User v)          { this.user = v; }
    public void setAction(String v)      { this.action = v; }
    public void setEntityType(String v)  { this.entityType = v; }
    public void setEntityId(Long v)      { this.entityId = v; }
    public void setOldValue(String v)    { this.oldValue = v; }
    public void setNewValue(String v)    { this.newValue = v; }
    public void setIpAddress(String v)   { this.ipAddress = v; }
    public void setUserAgent(String v)   { this.userAgent = v; }
    public void setTimestamp(LocalDateTime v) { this.timestamp = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final AuditLog a = new AuditLog();
        public Builder user(User v)         { a.user = v;       return this; }
        public Builder action(String v)     { a.action = v;     return this; }
        public Builder entityType(String v) { a.entityType = v; return this; }
        public Builder entityId(Long v)     { a.entityId = v;   return this; }
        public Builder oldValue(String v)   { a.oldValue = v;   return this; }
        public Builder newValue(String v)   { a.newValue = v;   return this; }
        public Builder ipAddress(String v)  { a.ipAddress = v;  return this; }
        public Builder userAgent(String v)  { a.userAgent = v;  return this; }
        public AuditLog build()             { return a; }
    }
}
