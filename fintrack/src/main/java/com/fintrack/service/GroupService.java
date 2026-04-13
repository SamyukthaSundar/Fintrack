package com.fintrack.service;

import com.fintrack.model.Group;
import com.fintrack.model.GroupMember;

import java.util.List;
import java.util.Optional;

/**
 * GroupService Interface
 * Owner: Saanvi Kakkar
 */
public interface GroupService {
    Group createGroup(String name, String description, String currency, Long creatorUserId);
    Optional<Group> findById(Long groupId);
    List<Group> findGroupsForUser(Long userId);
    Group addMember(Long groupId, Long userId, Long requestingUserId);
    void removeMember(Long groupId, Long userId, Long requestingUserId);
    List<GroupMember> getMembers(Long groupId);
    Group updateGroup(Long groupId, String name, String description, Long requestingUserId);
    void deleteGroup(Long groupId, Long requestingUserId);
    boolean isMember(Long groupId, Long userId);
}
