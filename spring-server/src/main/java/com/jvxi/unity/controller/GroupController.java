package com.jvxi.unity.controller;

import com.jvxi.unity.model.GroupMemberRole;
import com.jvxi.unity.model.User;
import com.jvxi.unity.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createGroup(Authentication auth, @RequestBody Map<String, Object> request) {
        try {
            Long userId = getUserId(auth);
            String name = (String) request.get("name");
            @SuppressWarnings("unchecked")
            List<Long> memberIds = ((List<Number>) request.getOrDefault("memberIds", List.of()))
                    .stream().map(Number::longValue).toList();
            Map<String, Object> group = groupService.createGroup(name, userId, memberIds);
            @SuppressWarnings("unchecked")
            List<Long> participantIds = (List<Long>) group.getOrDefault("memberIds", List.of(userId));
            notifyGroupEvent(participantIds, "GROUP_CREATED", (Long) group.get("id"), userId);
            return ResponseEntity.ok(Map.of("success", true, "data", group));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> dissolveGroup(Authentication auth, @PathVariable Long groupId) {
        try {
            Long userId = getUserId(auth);
            List<Long> memberIds = groupService.dissolveGroup(groupId, userId);
            notifyGroupEvent(memberIds, "GROUP_DISSOLVED", groupId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "群已解散"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> getGroupInfo(@PathVariable Long groupId) {
        try {
            Map<String, Object> info = groupService.getGroupInfo(groupId);
            return ResponseEntity.ok(Map.of("success", true, "data", info));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyGroups(Authentication auth) {
        try {
            Long userId = getUserId(auth);
            List<Map<String, Object>> groups = groupService.getMyGroups(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", groups));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<Map<String, Object>> getMembers(@PathVariable Long groupId) {
        try {
            List<Map<String, Object>> members = groupService.getGroupMembers(groupId);
            return ResponseEntity.ok(Map.of("success", true, "data", members));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<Map<String, Object>> addMembers(Authentication auth, @PathVariable Long groupId, @RequestBody Map<String, Object> request) {
        try {
            Long userId = getUserId(auth);
            @SuppressWarnings("unchecked")
            List<Long> userIds = ((List<Number>) request.getOrDefault("userIds", List.of()))
                    .stream().map(Number::longValue).toList();
            List<Long> addedUserIds = groupService.addMembers(groupId, userId, userIds);
            Set<Long> notifyUserIds = new LinkedHashSet<>(groupService.getGroupUserIds(groupId));
            notifyUserIds.addAll(addedUserIds);
            notifyUserIds.add(userId);
            notifyGroupEvent(new ArrayList<>(notifyUserIds), "MEMBERS_ADDED", groupId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "成员已添加"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<Map<String, Object>> removeMember(Authentication auth, @PathVariable Long groupId, @PathVariable Long targetUserId) {
        try {
            Long userId = getUserId(auth);
            Long removedUserId = groupService.removeMember(groupId, userId, targetUserId);
            Set<Long> notifyUserIds = new LinkedHashSet<>(groupService.getGroupUserIds(groupId));
            notifyUserIds.add(userId);
            notifyUserIds.add(removedUserId);
            notifyGroupEvent(new ArrayList<>(notifyUserIds), "MEMBER_REMOVED", groupId, userId, removedUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", "成员已移除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Map<String, Object>> leaveGroup(Authentication auth, @PathVariable Long groupId) {
        try {
            Long userId = getUserId(auth);
            Long leftUserId = groupService.leaveGroup(groupId, userId);
            Set<Long> notifyUserIds = new LinkedHashSet<>(groupService.getGroupUserIds(groupId));
            notifyUserIds.add(leftUserId);
            notifyGroupEvent(new ArrayList<>(notifyUserIds), "MEMBER_LEFT", groupId, userId, leftUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", "已退出群聊"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/{groupId}/members/{targetUserId}/role")
    public ResponseEntity<Map<String, Object>> updateRole(Authentication auth, @PathVariable Long groupId, @PathVariable Long targetUserId, @RequestBody Map<String, String> request) {
        try {
            Long userId = getUserId(auth);
            GroupMemberRole role = GroupMemberRole.valueOf(request.get("role"));
            groupService.updateRole(groupId, userId, targetUserId, role);
            notifyGroupEvent(groupService.getGroupUserIds(groupId), "MEMBER_ROLE_UPDATED", groupId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "角色已更新"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/{groupId}/members/{targetUserId}/mute")
    public ResponseEntity<Map<String, Object>> muteMember(Authentication auth, @PathVariable Long groupId, @PathVariable Long targetUserId, @RequestBody Map<String, Object> request) {
        try {
            Long userId = getUserId(auth);
            int minutes = ((Number) request.getOrDefault("minutes", 0)).intValue();
            groupService.muteMember(groupId, userId, targetUserId, minutes);
            notifyGroupEvent(groupService.getGroupUserIds(groupId), "MEMBER_MUTE_UPDATED", groupId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", minutes > 0 ? "已禁言" : "已解除禁言"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/{groupId}/name")
    public ResponseEntity<Map<String, Object>> updateGroupName(Authentication auth, @PathVariable Long groupId, @RequestBody Map<String, String> request) {
        try {
            Long userId = getUserId(auth);
            groupService.updateGroupName(groupId, userId, request.get("name"));
            notifyGroupEvent(groupService.getGroupUserIds(groupId), "GROUP_UPDATED", groupId, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "群名已更新"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new RuntimeException("未登录");
    }
    private void notifyGroupEvent(List<Long> userIds, String event, Long groupId, Long operatorId) {
        notifyGroupEvent(userIds, event, groupId, operatorId, null);
    }

    private void notifyGroupEvent(List<Long> userIds, String event, Long groupId, Long operatorId, Long affectedUserId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "GROUP_EVENT");
        payload.put("event", event);
        payload.put("groupId", groupId);
        payload.put("operatorId", operatorId);
        if (affectedUserId != null) {
            payload.put("affectedUserId", affectedUserId);
        }
        userIds.stream()
                .filter(id -> id != null)
                .distinct()
                .forEach(id -> messagingTemplate.convertAndSendToUser(id.toString(), "/queue/private", payload));
    }
}
