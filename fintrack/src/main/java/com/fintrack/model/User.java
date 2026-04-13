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
 * User Entity — Owner: Saanvi Kakkar
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank @Size(max = 100)
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<GroupMember> groupMemberships = new HashSet<>();

    @OneToMany(mappedBy = "paidBy", fetch = FetchType.LAZY)
    private Set<Expense> paidExpenses = new HashSet<>();

    public enum Role { USER, ADMIN }

    public User() {}

    // Getters
    public Long getId()                           { return id; }
    public String getUsername()                   { return username; }
    public String getEmail()                      { return email; }
    public String getPassword()                   { return password; }
    public String getFullName()                   { return fullName; }
    public Role getRole()                         { return role; }
    public String getAvatarUrl()                  { return avatarUrl; }
    public Boolean getIsActive()                  { return isActive; }
    public LocalDateTime getCreatedAt()           { return createdAt; }
    public LocalDateTime getUpdatedAt()           { return updatedAt; }
    public Set<GroupMember> getGroupMemberships() { return groupMemberships; }
    public Set<Expense> getPaidExpenses()         { return paidExpenses; }

    // Setters
    public void setId(Long id)                    { this.id = id; }
    public void setUsername(String v)             { this.username = v; }
    public void setEmail(String v)                { this.email = v; }
    public void setPassword(String v)             { this.password = v; }
    public void setFullName(String v)             { this.fullName = v; }
    public void setRole(Role v)                   { this.role = v; }
    public void setAvatarUrl(String v)            { this.avatarUrl = v; }
    public void setIsActive(Boolean v)            { this.isActive = v; }
    public void setCreatedAt(LocalDateTime v)     { this.createdAt = v; }
    public void setUpdatedAt(LocalDateTime v)     { this.updatedAt = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final User u = new User();
        public Builder username(String v)  { u.username = v;  return this; }
        public Builder email(String v)     { u.email = v;     return this; }
        public Builder password(String v)  { u.password = v;  return this; }
        public Builder fullName(String v)  { u.fullName = v;  return this; }
        public Builder role(Role v)        { u.role = v;      return this; }
        public Builder isActive(Boolean v) { u.isActive = v;  return this; }
        public User build()                { return u; }
    }

    @Override public String toString() { return "User{id=" + id + ", username='" + username + "'}"; }
}
