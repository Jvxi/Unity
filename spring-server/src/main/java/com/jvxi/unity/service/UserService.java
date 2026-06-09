package com.jvxi.unity.service;

import com.jvxi.unity.model.OnlineStatus;
import com.jvxi.unity.model.User;
import com.jvxi.unity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    public Map<String, Object> register(String nickname, String email, String password, String code) {
        if (!verificationService.verifyCode(email, code, "register")) {
            throw new RuntimeException("验证码错误或已过期");
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new RuntimeException("昵称已被使用");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已注册");
        }
        User user = new User();
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user = userRepository.save(user);
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

    public Map<String, Object> login(String login, String password) {
        User user = userRepository.findByNicknameOrEmail(login, login)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }
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

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public User getUserByToken(String token) {
        Long userId = jwtService.extractUserId(token);
        return getUserById(userId);
    }

    public Map<String, Object> getUserProfile(Long userId) {
        User user = getUserById(userId);
        return Map.of(
            "id", user.getId(),
            "nickname", user.getNickname(),
            "email", user.getEmail(),
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
            "bio", user.getBio() != null ? user.getBio() : "",
            "gender", user.getGender() != null ? user.getGender() : "",
            "birthday", user.getBirthday() != null ? user.getBirthday() : "",
            "onlineStatus", user.getOnlineStatus().name(),
            "createdAt", user.getCreatedAt().toString()
        );
    }

    public void updateProfile(Long userId, Map<String, String> updates) {
        User user = getUserById(userId);
        if (updates.containsKey("nickname")) {
            String nick = updates.get("nickname");
            if (nick != null && !nick.isBlank() && !nick.equals(user.getNickname())) {
                if (userRepository.existsByNickname(nick)) {
                    throw new RuntimeException("昵称已被使用");
                }
                user.setNickname(nick);
            }
        }
        if (updates.containsKey("bio")) user.setBio(updates.get("bio"));
        if (updates.containsKey("gender")) user.setGender(updates.get("gender"));
        if (updates.containsKey("birthday")) user.setBirthday(updates.get("birthday"));
        userRepository.save(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void updateAvatar(Long userId, String avatarUrl) {
        User user = getUserById(userId);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
    }

    public void setOnlineStatus(Long userId, OnlineStatus status) {
        User user = getUserById(userId);
        user.setOnlineStatus(status);
        if (status == OnlineStatus.ONLINE) {
            user.setLastActiveAt(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.searchByNicknameOrEmail(keyword);
    }

    public Map<String, Object> getUserBasicInfo(Long userId) {
        User user = getUserById(userId);
        return Map.of(
            "id", user.getId(),
            "nickname", user.getNickname(),
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
            "onlineStatus", user.getOnlineStatus().name()
        );
    }
}
