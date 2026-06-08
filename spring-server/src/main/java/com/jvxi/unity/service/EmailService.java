package com.jvxi.unity.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${cat-tool.mail.from}")
    private String fromEmail;

    @Value("${cat-tool.mail.from-name}")
    private String fromName;

    /**
     * 发送注册验证码邮件
     */
    public void sendRegisterCode(String toEmail, String code) {
        String subject = "【猫爪工具】注册验证码";
        String htmlContent = loadTemplate("templates/email/verification-code.html", code);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * 发送密码重置验证码邮件
     */
    public void sendResetPasswordCode(String toEmail, String code) {
        String subject = "【猫爪工具】密码重置验证码";
        String htmlContent = loadTemplate("templates/email/reset-password.html", code);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * 发送HTML邮件（带嵌入图片）
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // 嵌入小猫logo图片 (CID方式)
            ClassPathResource logoResource = new ClassPathResource("static/images/cat-logo.png");
            if (logoResource.exists()) {
                helper.addInline("catLogo", logoResource);
            }

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加载邮件模板并替换验证码
     */
    private String loadTemplate(String templatePath, String code) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return template.replace("${CODE}", code);
        } catch (IOException e) {
            throw new RuntimeException("邮件模板加载失败: " + e.getMessage(), e);
        }
    }
}
