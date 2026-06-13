package com.jvxi.unity.service;

import com.jvxi.unity.model.*;
import com.jvxi.unity.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GroupService {

    @Autowired
    private ChatGroupRepository groupRepository;

    @Autowired
    private ChatGroupMemberRepository memberRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ChatService chatService;

    @Transactional
    public Map<String, Object> createGroup(String name, Long ownerId, List<Long> memberIds) {
        Set<Long> participantIds = new LinkedHashSet<>();
        participantIds.add(ownerId);
        ChatGroup group = new ChatGroup();
        group.setName(name);
        group.setOwnerId(ownerId);
        group = groupRepository.save(group);

        // 创建者为群主
        ChatGroupMember owner = new ChatGroupMember();
        owner.setGroupId(group.getId());
        owner.setUserId(ownerId);
        owner.setRole(GroupMemberRole.OWNER);
        owner.setNickname(userService.getUserById(ownerId).getNickname());
        memberRepository.save(owner);
        chatService.ensureGroupSession(ownerId, group.getId());

        // 添加成员
        if (memberIds != null) {
            for (Long memberId : memberIds) {
                if (!memberId.equals(ownerId)) {
                    addMemberInternal(group.getId(), memberId, GroupMemberRole.MEMBER);
                    chatService.ensureGroupSession(memberId, group.getId());
                    participantIds.add(memberId);
                }
            }
        }

        Map<String, Object> result = groupToMap(group);
        result.put("memberIds", new ArrayList<>(participantIds));
        return result;
    }

    @Transactional
    public List<Long> dissolveGroup(Long groupId, Long userId) {
        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));
        if (!group.getOwnerId().equals(userId)) {
            throw new RuntimeException("只有群主可以解散群");
        }
        List<ChatGroupMember> members = memberRepository.findByGroupId(groupId);
        List<Long> memberIds = members.stream().map(ChatGroupMember::getUserId).toList();
        memberRepository.deleteAll(members);
        chatService.removeAllGroupSessions(groupId);
        groupRepository.delete(group);
        return memberIds;
    }

    @Transactional
    public List<Long> addMembers(Long groupId, Long operatorId, List<Long> userIds) {
        ChatGroupMember operator = memberRepository.findByGroupIdAndUserId(groupId, operatorId)
                .orElseThrow(() -> new RuntimeException("您不是群成员"));
        if (operator.getRole() == GroupMemberRole.MEMBER) {
            throw new RuntimeException("只有群主或管理员可以邀请成员");
        }
        List<Long> addedUserIds = new ArrayList<>();
        for (Long uid : userIds) {
            if (!memberRepository.existsByGroupIdAndUserId(groupId, uid)) {
                addMemberInternal(groupId, uid, GroupMemberRole.MEMBER);
                chatService.ensureGroupSession(uid, groupId);
                addedUserIds.add(uid);
            }
        }
        return addedUserIds;
    }

    @Transactional
    public Long removeMember(Long groupId, Long operatorId, Long targetUserId) {
        ChatGroupMember operator = memberRepository.findByGroupIdAndUserId(groupId, operatorId)
                .orElseThrow(() -> new RuntimeException("您不是群成员"));
        ChatGroupMember target = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new RuntimeException("目标用户不是群成员"));

        if (targetUserId.equals(operatorId)) throw new RuntimeException("不能移除自己");
        if (target.getRole() == GroupMemberRole.OWNER) throw new RuntimeException("不能移除群主");
        if (operator.getRole() == GroupMemberRole.MEMBER) throw new RuntimeException("权限不足");
        if (operator.getRole() == GroupMemberRole.ADMIN && target.getRole() == GroupMemberRole.ADMIN) {
            throw new RuntimeException("管理员不能移除管理员");
        }

        memberRepository.deleteByGroupIdAndUserId(groupId, targetUserId);
        chatService.removeGroupSession(targetUserId, groupId);
        return targetUserId;
    }

    @Transactional
    public Long leaveGroup(Long groupId, Long userId) {
        ChatGroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("您不是群成员"));
        if (member.getRole() == GroupMemberRole.OWNER) {
            throw new RuntimeException("群主不能退群，请先转让群主或解散群");
        }
        memberRepository.deleteByGroupIdAndUserId(groupId, userId);
        chatService.removeGroupSession(userId, groupId);
        return userId;
    }

    @Transactional
    public void updateRole(Long groupId, Long operatorId, Long targetUserId, GroupMemberRole newRole) {
        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));
        if (!group.getOwnerId().equals(operatorId)) {
            throw new RuntimeException("只有群主可以设置角色");
        }
        ChatGroupMember target = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new RuntimeException("目标用户不是群成员"));
        target.setRole(newRole);
        memberRepository.save(target);
    }

    @Transactional
    public void muteMember(Long groupId, Long operatorId, Long targetUserId, int minutes) {
        ChatGroupMember operator = memberRepository.findByGroupIdAndUserId(groupId, operatorId)
                .orElseThrow(() -> new RuntimeException("您不是群成员"));
        ChatGroupMember target = memberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new RuntimeException("目标用户不是群成员"));

        if (operator.getRole() == GroupMemberRole.MEMBER) throw new RuntimeException("权限不足");
        if (target.getRole() == GroupMemberRole.OWNER) throw new RuntimeException("不能禁言群主");
        if (operator.getRole() == GroupMemberRole.ADMIN && target.getRole() == GroupMemberRole.ADMIN) {
            throw new RuntimeException("管理员不能禁言管理员");
        }

        if (minutes <= 0) {
            target.setMutedUntil(null);
        } else {
            target.setMutedUntil(LocalDateTime.now().plusMinutes(minutes));
        }
        memberRepository.save(target);
    }

    public List<Map<String, Object>> getGroupMembers(Long groupId) {
        List<ChatGroupMember> members = memberRepository.findByGroupId(groupId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatGroupMember m : members) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("userId", m.getUserId());
            map.put("role", m.getRole().name());
            map.put("nickname", m.getNickname());
            map.put("mutedUntil", m.getMutedUntil() != null ? m.getMutedUntil().toString() : null);
            map.put("joinedAt", m.getJoinedAt().toString());

            Map<String, Object> userInfo = userService.getUserBasicInfo(m.getUserId());
            map.put("avatarUrl", userInfo.get("avatarUrl"));
            map.put("onlineStatus", userInfo.get("onlineStatus"));
            result.add(map);
        }
        return result;
    }

    public List<Long> getGroupUserIds(Long groupId) {
        return memberRepository.findByGroupId(groupId)
                .stream()
                .map(ChatGroupMember::getUserId)
                .toList();
    }

    public List<Map<String, Object>> getMyGroups(Long userId) {
        List<Long> groupIds = memberRepository.findGroupIdsByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long gid : groupIds) {
            groupRepository.findById(gid).ifPresent(group -> {
                result.add(groupToMap(group));
            });
        }
        return result;
    }

    public Map<String, Object> getGroupInfo(Long groupId) {
        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));
        return groupToMap(group);
    }

    @Transactional
    public void updateGroupName(Long groupId, Long userId, String name) {
        ChatGroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("您不是群成员"));
        if (member.getRole() == GroupMemberRole.MEMBER) {
            throw new RuntimeException("只有群主或管理员可以修改群名");
        }
        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));
        group.setName(name);
        groupRepository.save(group);
    }

    public boolean isMember(Long groupId, Long userId) {
        return memberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    public boolean isMuted(Long groupId, Long userId) {
        ChatGroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId).orElse(null);
        if (member == null) return false;
        return member.getMutedUntil() != null && member.getMutedUntil().isAfter(LocalDateTime.now());
    }

    private void addMemberInternal(Long groupId, Long userId, GroupMemberRole role) {
        User user = userService.getUserById(userId);
        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setNickname(user.getNickname());
        memberRepository.save(member);
    }

    private Map<String, Object> groupToMap(ChatGroup group) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", group.getId());
        map.put("name", group.getName());
        map.put("avatarUrl", group.getAvatarUrl() != null ? group.getAvatarUrl() : "");
        map.put("ownerId", group.getOwnerId());
        map.put("memberCount", memberRepository.countByGroupId(group.getId()));
        map.put("createdAt", group.getCreatedAt().toString());
        return map;
    }
}
