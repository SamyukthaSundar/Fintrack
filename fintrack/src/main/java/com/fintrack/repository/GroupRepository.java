package com.fintrack.repository;

import com.fintrack.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * GroupRepository
 * Owner: Saanvi Kakkar
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("""
        SELECT DISTINCT g FROM Group g
        JOIN g.members m
        WHERE m.user.id = :userId AND g.isActive = true
        ORDER BY g.createdAt DESC
        """)
    List<Group> findGroupsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM GroupMember m
        WHERE m.group.id = :groupId AND m.user.id = :userId
        """)
    boolean isMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
