package com.fintrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * GroupMember — Owner: Saanvi Kakkar
 */
@Entity
@Table(name = "group_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
public class GroupMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.MEMBER;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    public enum MemberRole { ADMIN, MEMBER }

    public GroupMember() {}

    // Getters
    public Long getId()            { return id; }
    public Group getGroup()        { return group; }
    public User getUser()          { return user; }
    public MemberRole getRole()    { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }

    // Setters
    public void setId(Long v)              { this.id = v; }
    public void setGroup(Group v)          { this.group = v; }
    public void setUser(User v)            { this.user = v; }
    public void setRole(MemberRole v)      { this.role = v; }
    public void setJoinedAt(LocalDateTime v){ this.joinedAt = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final GroupMember m = new GroupMember();
        public Builder group(Group v)       { m.group = v;    return this; }
        public Builder user(User v)         { m.user = v;     return this; }
        public Builder role(MemberRole v)   { m.role = v;     return this; }
        public GroupMember build()          { return m; }
    }
}
