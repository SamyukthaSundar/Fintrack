package com.fintrack.service.impl;

import com.fintrack.model.*;
import com.fintrack.observer.FinTrackEvent;
import com.fintrack.repository.*;
import com.fintrack.service.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * GroupServiceImpl — Owner: Saanvi Kakkar
 */
@Service
@Transactional
public class GroupServiceImpl implements GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupServiceImpl.class);

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public GroupServiceImpl(GroupRepository groupRepository,
                             GroupMemberRepository memberRepository,
                             UserRepository userRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository   = userRepository;
        this.eventPublisher   = eventPublisher;
    }

    @Override
    public Group createGroup(String name, String description, String currency, Long creatorUserId) {
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + creatorUserId));
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setCurrency(currency != null ? currency : "INR");
        group.setCreatedBy(creator);
        group = groupRepository.save(group);

        GroupMember adminMember = new GroupMember();
        adminMember.setGroup(group);
        adminMember.setUser(creator);
        adminMember.setRole(GroupMember.MemberRole.ADMIN);
        memberRepository.save(adminMember);
        log.info("Group '{}' created by '{}'", group.getName(), creator.getUsername());
        return group;
    }

    @Override @Transactional(readOnly = true)
    public Optional<Group> findById(Long groupId) { return groupRepository.findById(groupId); }

    @Override @Transactional(readOnly = true)
    public List<Group> findGroupsForUser(Long userId) { return groupRepository.findGroupsByUserId(userId); }

    @Override
    public Group addMember(Long groupId, Long userId, Long requestingUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        if (memberRepository.existsByGroupIdAndUserId(groupId, userId))
            throw new IllegalStateException("User is already a member.");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(GroupMember.MemberRole.MEMBER);
        memberRepository.save(member);
        eventPublisher.publishEvent(new FinTrackEvent(
                this, Notification.NotificationType.GROUP_INVITE, userId,
                "Added to Group: " + group.getName(),
                "You have been added to the group '" + group.getName() + "'.",
                groupId, "GROUP"));
        return group;
    }

    @Override
    public void removeMember(Long groupId, Long userId, Long requestingUserId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        memberRepository.delete(member);
    }

    @Override @Transactional(readOnly = true)
    public List<GroupMember> getMembers(Long groupId) {
        return memberRepository.findMembersWithUsers(groupId);
    }

    @Override
    public Group updateGroup(Long groupId, String name, String description, Long requestingUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        group.setName(name);
        group.setDescription(description);
        return groupRepository.save(group);
    }

    @Override
    public void deleteGroup(Long groupId, Long requestingUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        group.setIsActive(false);
        groupRepository.save(group);
    }

    @Override @Transactional(readOnly = true)
    public boolean isMember(Long groupId, Long userId) {
        return groupRepository.isMember(groupId, userId);
    }
}
