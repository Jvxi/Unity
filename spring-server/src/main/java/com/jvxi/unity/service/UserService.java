package com.jvxi.unity.service;

import com.jvxi.unity.model.User;
import com.jvxi.unity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private JwtService jwtService;

    /**
     * 检查昵称是否可用
     */
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    /**
     * 检查邮箱是否已注册
     */
    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 用户注册
     */
    public Map<String, Object> register(String nickname, String email, String password, String code) {
        // 验证验证码
        if (!verificationService.verifyCode(email, code, "register")) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 检查昵称是否可用
        if (userRepository.existsByNickname(nickname)) {
            throw new RuntimeException("昵称已被使用");
        }

        // 检查邮箱是否已注册
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已注册");
        }

        // 创建用户
        User user = new User();
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user = userRepository.save(user);

        // 生成Token
        String token = jwtService.generateToken(user.getId(), user.getNickname());

        return Map.of(
            "success", true,
            "message", "注册成功",
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "nickname", user.getNickname(),
                "email", user.getEmail()
            )
        );
    }

    /**
     * 用户登录（支持昵称或邮箱）
     */
    public Map<String, Object> login(String login, String password) {
        // 查找用户（昵称或邮箱）
        User user = userRepository.findByNicknameOrEmail(login, login)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }

        // 生成Token
        String token = jwtService.generateToken(user.getId(), user.getNickname());

        return Map.of(
            "success", true,
            "message", "登录成功",
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "nickname", user.getNickname(),
                "email", user.getEmail()
            )
        );
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 根据Token获取用户
     */
    public User getUserByToken(String token) {
        Long userId = jwtService.extractUserId(token);
        return getUserById(userId);
    }
}
