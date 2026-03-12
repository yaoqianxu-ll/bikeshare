package com.example.bickdemo.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmailMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.from-name:BikeShare}")
    private String fromName;

    public void sendVerificationCode(String email, String code, String type, int expireMinutes) {
        if (!StringUtils.hasText(fromEmail)) {
            throw new RuntimeException("发信账号未配置，请先设置 spring.mail.username");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(email);
            helper.setSubject(buildSubject(type));
            helper.setText(buildHtml(code, type, expireMinutes), true);
            mailSender.send(message);
        } catch (MailSendException e) {
            throw new RuntimeException("邮件发送失败，请检查 SMTP 配置");
        } catch (Exception e) {
            throw new RuntimeException("验证码发送失败：" + e.getMessage());
        }
    }

    private String buildSubject(String type) {
        if ("RESET_PASSWORD".equalsIgnoreCase(type)) {
            return "BikeShare 找回密码验证码";
        }
        return "BikeShare 邮箱注册验证码";
    }

    private String buildHtml(String code, String type, int expireMinutes) {
        String scene = "RESET_PASSWORD".equalsIgnoreCase(type) ? "重置密码" : "注册账号";
        return """
                <div style="font-family: Arial, sans-serif; padding: 24px; color: #0f172a;">
                  <h2 style="margin: 0 0 16px; color: #ff6b35;">BikeShare</h2>
                  <p style="margin: 0 0 12px;">您正在进行%s，请使用以下验证码：</p>
                  <div style="font-size: 28px; font-weight: 700; letter-spacing: 6px; color: #111827; margin: 16px 0;">%s</div>
                  <p style="margin: 0; color: #475569;">验证码 %d 分钟内有效，请勿泄露给他人。</p>
                </div>
                """.formatted(scene, code, expireMinutes);
    }
}
