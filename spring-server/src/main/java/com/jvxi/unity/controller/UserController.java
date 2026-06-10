package com.jvxi.unity.controller;

import com.jvxi.unity.model.User;
import com.jvxi.unity.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final long AVATAR_MAX_SIZE = 10L * 1024 * 1024;
    private static final String AVATAR_MAX_SIZE_LABEL = "10MB";

    @Autowired
    private UserService userService;

    @Value("${cat-tool.upload-dir:uploads}")
    private String uploadRoot;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        try {
            Long userId = getUserId(auth);
            Map<String, Object> profile = userService.getUserProfile(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", profile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(Authentication auth, @RequestBody Map<String, String> request) {
        try {
            Long userId = getUserId(auth);
            userService.updateProfile(userId, request);
            return ResponseEntity.ok(Map.of("success", true, "message", "个人资料已更新"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(Authentication auth, @RequestBody Map<String, String> request) {
        try {
            Long userId = getUserId(auth);
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "参数不完整"));
            }
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "新密码长度不能少于6位"));
            }
            userService.changePassword(userId, oldPassword, newPassword);
            return ResponseEntity.ok(Map.of("success", true, "message", "密码已修改"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(Authentication auth, @RequestParam("file") MultipartFile file) {
        try {
            Long userId = getUserId(auth);
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "请选择文件"));
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "仅支持上传图片头像"));
            }
            if (file.getSize() > AVATAR_MAX_SIZE) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "头像不能超过 " + AVATAR_MAX_SIZE_LABEL));
            }

            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".") ?
                originalName.substring(originalName.lastIndexOf(".")) : ".png";
            String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + ext;

            Path uploadDir = Paths.get(uploadRoot, "avatars").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            if (!Files.isWritable(uploadDir)) {
                log.warn("Avatar upload directory is not writable: {}", uploadDir);
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "头像上传目录不可写，请检查服务器目录权限"));
            }
            Path filePath = uploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            String avatarUrl = "/uploads/avatars/" + fileName;
            userService.updateAvatar(userId, avatarUrl);

            return ResponseEntity.ok(Map.of("success", true, "avatarUrl", avatarUrl));
        } catch (IOException e) {
            log.warn("Avatar upload failed, uploadRoot={}", uploadRoot, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "头像保存失败，请检查服务器上传目录权限"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String keyword) {
        try {
            List<User> users = userService.searchUsers(keyword);
            List<Map<String, Object>> result = new ArrayList<>();
            for (User u : users) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", u.getId());
                map.put("nickname", u.getNickname());
                map.put("avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : "");
                map.put("onlineStatus", u.getOnlineStatus().name());
                result.add(map);
            }
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable Long id) {
        try {
            Map<String, Object> info = userService.getUserBasicInfo(id);
            return ResponseEntity.ok(Map.of("success", true, "data", info));
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
