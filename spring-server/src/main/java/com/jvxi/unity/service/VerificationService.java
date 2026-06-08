package com.jvxi.unity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class VerificationService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EmailService emailService;

    @Value("${cat-tool.verification.code-length:6}")
    private int codeLength;

    @Value("${cat-tool.verification.code-chars:3456789ABCDEFGHJKMNPQRSTUVWXY}")
    private String codeChars;

    @Value("${cat-tool.mail.code-ttl-minutes:3}")
    private int codeTtlMinutes;

    @Value("${cat-tool.mail.cooldown-seconds:60}")
    private int cooldownSeconds;

    private static final String CODE_KEY_PREFIX = "cat-tool:verify:";
    private static final String COOLDOWN_KEY_PREFIX = "cat-tool:cooldown:";

    private final SecureRandom random = new SecureRandom();

    /**
     * 发送注册验证码
     */
    public void sendRegisterCode(String email) {
        sendCode(email, "register");
    }

    /**
     * 发送密码重置验证码
     */
    public void sendResetPasswordCode(String email) {
        sendCode(email, "reset_password");
    }

    /**
     * 发送验证码通用方法
     */
    private void sendCode(String email, String purpose) {
        // 检查冷却时间
        String cooldownKey = COOLDOWN_KEY_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new RuntimeException("请" + cooldownSeconds + "秒后再试");
        }

        // 生成验证码
        String code = generateCode();

        // 存储到Redis
        String codeKey = CODE_KEY_PREFIX + purpose + ":" + email;
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(codeTtlMinutes));

        // 设置冷却时间
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(cooldownSeconds));

        // 发送邮件
        if ("register".equals(purpose)) {
            emailService.sendRegisterCode(email, code);
        } else {
            emailService.sendResetPasswordCode(email, code);
        }
    }

    /**
     * 验证验证码
     */
    public boolean verifyCode(String email, String code, String purpose) {
        String codeKey = CODE_KEY_PREFIX + purpose + ":" + email;
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode != null && storedCode.equalsIgnoreCase(code)) {
            redisTemplate.delete(codeKey); // 验证成功后删除，防止重复使用
            return true;
        }
        return false;
    }

    /**
     * 生成随机验证码
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(codeChars.charAt(random.nextInt(codeChars.length())));
        }
        return sb.toString();
    }
}
