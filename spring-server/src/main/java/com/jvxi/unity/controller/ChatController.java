package com.jvxi.unity.controller;

import com.jvxi.unity.model.ChatMessageType;
import com.jvxi.unity.model.User;
import com.jvxi.unity.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Value("${cat-tool.upload-dir:uploads}")
    private String uploadRoot;

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessions(Authentication auth) {
        try {
            Long userId = getUserId(auth);
            List<Map<String, Object>> sessions = chatService.getSessions(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", sessions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/history/private")
    public ResponseEntity<Map<String, Object>> getPrivateHistory(
            Authentication auth,
            @RequestParam Long targetUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Long userId = getUserId(auth);
            List<Map<String, Object>> messages = chatService.getPrivateHistoryDesc(userId, targetUserId, page, size);
            return ResponseEntity.ok(Map.of("success", true, "data", messages));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/history/group")
    public ResponseEntity<Map<String, Object>> getGroupHistory(
            @RequestParam Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            List<Map<String, Object>> messages = chatService.getGroupHistory(groupId, page, size);
            return ResponseEntity.ok(Map.of("success", true, "data", messages));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMessages(
            Authentication auth,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) Long groupId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = getUserId(auth);
            List<Map<String, Object>> messages = chatService.searchMessages(userId, targetUserId, groupId, keyword, page, size);
            return ResponseEntity.ok(Map.of("success", true, "data", messages));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            Authentication auth,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "请选择文件"));
            }

            String originalName = StringUtils.cleanPath(
                Optional.ofNullable(file.getOriginalFilename()).orElse("file.bin")
            );
            if (originalName.contains("..")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "文件名不合法"));
            }
            String fileName = System.currentTimeMillis() + "_" + originalName;

            Path uploadDir = Paths.get(uploadRoot, "chat-files");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            String fileUrl = "/uploads/chat-files/" + fileName;
            String contentType = file.getContentType();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "fileUrl", fileUrl,
                "fileName", originalName != null ? originalName : fileName,
                "fileSize", file.getSize(),
                "contentType", contentType != null ? contentType : "application/octet-stream"
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "上传失败"));
        }
    }

    @PostMapping("/send-file")
    public ResponseEntity<Map<String, Object>> sendFileMessage(
            Authentication auth,
            @RequestParam Long receiverId,
            @RequestParam(required = false) Long groupId,
            @RequestParam String fileUrl,
            @RequestParam String fileName,
            @RequestParam Long fileSize,
            @RequestParam(defaultValue = "FILE") String messageType) {
        try {
            Long userId = getUserId(auth);
            ChatMessageType type = ChatMessageType.valueOf(messageType);

            if (groupId != null) {
                chatService.sendGroupFileMessage(userId, groupId, fileUrl, fileName, fileSize, type);
            } else {
                chatService.sendPrivateFileMessage(userId, receiverId, fileUrl, fileName, fileSize, type);
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            Authentication auth,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) Long groupId) {
        try {
            Long userId = getUserId(auth);
            chatService.markAsRead(userId, targetUserId, groupId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/recall/{messageId}")
    public ResponseEntity<Map<String, Object>> recallMessage(
            Authentication auth,
            @PathVariable Long messageId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long receiverId) {
        try {
            Long userId = getUserId(auth);
            boolean success = chatService.recallMessage(messageId, userId);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "消息已撤回"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "无法撤回消息（已超过2分钟或非本人消息）"));
            }
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
