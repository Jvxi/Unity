package com.jvxi.unity.controller;

import com.jvxi.unity.service.UserService;
import com.jvxi.unity.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private UserService userService;

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, Object>> sendCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String purpose = request.get("purpose");

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "邮箱不能为空"));
            }
            if (purpose == null || purpose.isBlank()) {
                purpose = "register";
            }

            if ("register".equals(purpose)) {
                verificationService.sendRegisterCode(email);
            } else if ("reset_password".equals(purpose)) {
                verificationService.sendResetPasswordCode(email);
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "无效的验证码类型"));
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "验证码已发送"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Object>> verifyCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            String purpose = request.get("purpose");

            if (email == null || code == null || purpose == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "参数不完整"));
            }

            boolean valid = verificationService.verifyCode(email, code, purpose);

            if (valid) {
                return ResponseEntity.ok(Map.of("success", true, "message", "验证成功"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "验证码错误或已过期"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNickname(@RequestParam String nickname) {
        try {
            boolean available = userService.isNicknameAvailable(nickname);
            return ResponseEntity.ok(Map.of("success", true, "available", available));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String nickname = request.get("nickname");
            String email = request.get("email");
            String password = request.get("password");
            String code = request.get("code");

            if (nickname == null || email == null || password == null || code == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "参数不完整"));
            }

            Map<String, Object> result = userService.register(nickname, email, password, code);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String login = request.get("login");
            String password = request.get("password");

            if (login == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "参数不完整"));
            }

            Map<String, Object> result = userService.login(login, password);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
