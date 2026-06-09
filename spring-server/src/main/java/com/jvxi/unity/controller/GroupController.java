package com.jvxi.unity.controller;

import com.jvxi.unity.model.GroupMemberRole;
import com.jvxi.unity.model.User;
import com.jvxi.unity.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createGroup(Authentication auth, @RequestBody Map<String, Object> request) {
        try {
            Long userId = getUserId(auth);
            String name = (String) request.get("name");
            @SuppressWarnings("unchecked")
            List<Long> memberIds = ((List<Number>) request.getOrDefault("memberIds", List.of()))
                    .stream().map(Number::longValue).toList();
            Map<String, Object> group = groupService.createGroup(name, userId, memberIds);
            return ResponseEntity.ok(Map.of("success", true, "data", group));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> dissolveGroup(Authentication auth, @PathVariable Long groupId) {
        try {
            Long userId = getUserId(auth);
            groupService.dissolveGroup(groupId, userId);
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
            groupService.addMembers(groupId, userId, userIds);
            return ResponseEntity.ok(Map.of("success", true, "message", "成员已添加"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<Map<String, Object>> removeMember(Authentication auth, @PathVariable Long groupId, @PathVariable Long targetUserId) {
        try {
            Long userId = getUserId(auth);
            groupService.removeMember(groupId, userId, targetUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", "成员已移除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Map<String, Object>> leaveGroup(Authentication auth, @PathVariable Long groupId) {
        try {
            Long userId = getUserId(auth);
            groupService.leaveGroup(groupId, userId);
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
}
