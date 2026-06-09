package com.example.bickdemo.service;

import com.example.bickdemo.config.RabbitMqConfig;
import com.example.bickdemo.entity.EmailNotificationLog;
import com.example.bickdemo.entity.EmailNotificationType;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.event.EmailEvent;
import com.example.bickdemo.mapper.EmailNotificationLogMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户邮件通知服务。
 * 负责组装 HTML 邮件内容并发送，内置频控逻辑防止重复骚扰。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEmailNotificationService {

    private final JavaMailSender mailSender;
    private final EmailNotificationLogMapper notificationLogMapper;
    private final UserNotificationSettingsService settingsService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.from-name:BikeShare}")
    private String fromName;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 发送私信未读提醒邮件。
     * 同一发送者对同一接收者，1 小时内最多发送 1 封。
     */
    @Async
    public void sendPrivateMessageEmail(User receiver, String senderName, String messagePreview, Long senderId) {
        if (!StringUtils.hasText(receiver.getEmail())) {
            return;
        }
        if (!settingsService.isEnabled(receiver.getId(), EmailNotificationType.MESSAGE.name())) {
            log.debug("用户 {} 已关闭私信邮件通知，跳过", receiver.getId());
            return;
        }
        // 频控检查：同一发送者 1 小时内只推送一次
        if (hasRecentNotification(receiver.getId(), EmailNotificationType.MESSAGE.name(), senderId, 60)) {
            log.debug("用户 {} 在冷却期内已收到来自发送者 {} 的私信提醒，跳过", receiver.getId(), senderId);
            return;
        }

        String subject = "BikeShare 您收到了一条私信";
        String html = buildPrivateMessageHtml(senderName, messagePreview);
        publishToQueue(receiver.getEmail(), subject, html, receiver.getId(), EmailNotificationType.MESSAGE.name(), senderId);
    }

    /**
     * 发送评论通知邮件。
     * 同一帖子对同一用户，1 小时内最多发送 1 封。
     */
    @Async
    public void sendCommentEmail(User postAuthor, String commenterName, String postTitle, String commentContent, Long postId) {
        if (!StringUtils.hasText(postAuthor.getEmail())) {
            return;
        }
        if (!settingsService.isEnabled(postAuthor.getId(), EmailNotificationType.COMMENT.name())) {
            log.debug("用户 {} 已关闭评论邮件通知，跳过", postAuthor.getId());
            return;
        }
        if (hasRecentNotification(postAuthor.getId(), EmailNotificationType.COMMENT.name(), postId, 60)) {
            log.debug("用户 {} 在冷却期内已收到帖子 {} 的评论提醒，跳过", postAuthor.getId(), postId);
            return;
        }

        String subject = "BikeShare 您的帖子收到了新评论";
        String html = buildCommentHtml(commenterName, postTitle, commentContent, postId);
        publishToQueue(postAuthor.getEmail(), subject, html, postAuthor.getId(), EmailNotificationType.COMMENT.name(), postId);
    }

    /**
     * 发送系统通知邮件（活动发布、公告发布、审核结果等）。
     * 系统通知不做频控。
     */
    @Async
    public void sendSystemEmail(User user, String subject, String title, String content, String actionUrl) {
        if (!StringUtils.hasText(user.getEmail())) {
            return;
        }
        if (!settingsService.isEnabled(user.getId(), EmailNotificationType.SYSTEM.name())) {
            log.debug("用户 {} 已关闭系统邮件通知，跳过", user.getId());
            return;
        }

        // 如果 actionUrl 是相对路径，补全为完整的前端域名
        if (StringUtils.hasText(actionUrl) && actionUrl.startsWith("/")) {
            actionUrl = frontendUrl + actionUrl;
        }

        String fullSubject = "BikeShare " + subject;
        String html = buildSystemHtml(title, content, actionUrl);
        publishToQueue(user.getEmail(), fullSubject, html, user.getId(), EmailNotificationType.SYSTEM.name(), null);
    }

    /**
     * 发送审核结果邮件（帖子/评论审核通过或驳回）。
     */
    @Async
    public void sendReviewResultEmail(User user, String targetType, String targetTitle, boolean approved, Long targetId) {
        if (!StringUtils.hasText(user.getEmail())) {
            return;
        }
        if (!settingsService.isEnabled(user.getId(), EmailNotificationType.SYSTEM.name())) {
            return;
        }

        String statusText = approved ? "已通过" : "已驳回";
        String subject = "BikeShare 您的" + targetType + "审核" + statusText;
        String html = buildReviewResultHtml(targetType, targetTitle, approved, targetId);
        publishToQueue(user.getEmail(), subject, html, user.getId(), EmailNotificationType.SYSTEM.name(), targetId);
    }

    /**
     * 检查是否在冷却期内已有同类通知。
     */
    private boolean hasRecentNotification(Long userId, String type, Long refId, int cooldownMinutes) {
        if (refId == null) {
            return false;
        }
        LocalDateTime since = LocalDateTime.now().minusMinutes(cooldownMinutes);
        int count = notificationLogMapper.countRecentByUserAndTypeAndRef(userId, type, refId, since);
        return count > 0;
    }

    /**
     * 将邮件事件发布到 RabbitMQ 队列，由消费者逐条处理，避免并发压垮 SMTP 服务器
     */
    private void publishToQueue(String toEmail, String subject, String html, Long userId, String type, Long refId) {
        EmailEvent event = new EmailEvent(toEmail, subject, html, userId, type, refId);
        rabbitTemplate.convertAndSend(RabbitMqConfig.EMAIL_EXCHANGE, RabbitMqConfig.EMAIL_ROUTING_KEY, event);
        log.debug("邮件事件已入队，type={}, userId={}, to={}", type, userId, toEmail);
    }

    /**
     * 处理邮件队列事件，由 EmailQueueListener 调用，执行实际的邮件发送
     */
    public void processEmailEvent(EmailEvent event) {
        doSend(event.getToEmail(), event.getSubject(), event.getHtml(),
                event.getUserId(), event.getType(), event.getRefId());
    }

    /**
     * 实际发送邮件并记录日志。
     */
    private void doSend(String toEmail, String subject, String html, Long userId, String type, Long refId) {
        if (!StringUtils.hasText(fromEmail)) {
            log.warn("邮件发送账号未配置 (spring.mail.username)，跳过发送");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);

            // 记录发送日志
            EmailNotificationLog logEntry = new EmailNotificationLog();
            logEntry.setUserId(userId);
            logEntry.setType(type);
            logEntry.setRefId(refId);
            notificationLogMapper.insert(logEntry);

            log.info("邮件通知已发送，type={}, userId={}, to={}", type, userId, toEmail);
        } catch (MailSendException e) {
            log.error("邮件发送失败，userId={}, to={}: {}", userId, toEmail, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("邮件通知异常，userId={}, to={}: {}", userId, toEmail, e.getMessage());
            throw new RuntimeException("邮件发送异常: " + e.getMessage(), e);
        }
    }

    // ========== HTML 模板构建 ==========

    private String buildPrivateMessageHtml(String senderName, String messagePreview) {
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 32px 24px; background: #f8fafc; border-radius: 16px;">
                  <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="margin: 0; font-size: 22px; font-weight: 800; color: #ff6b35;">BikeShare</h1>
                  </div>
                  <div style="background: #ffffff; border-radius: 14px; padding: 28px; box-shadow: 0 4px 16px rgba(15,23,42,0.08);">
                    <h2 style="margin: 0 0 16px; font-size: 18px; color: #0f172a;">您收到了一条私信</h2>
                    <div style="background: #f1f5f9; border-radius: 10px; padding: 16px; margin-bottom: 20px;">
                      <p style="margin: 0 0 8px; font-size: 14px; color: #64748b;">来自：<strong style="color: #0f172a;">%s</strong></p>
                      <p style="margin: 0; font-size: 15px; color: #334155; line-height: 1.6;">%s</p>
                    </div>
                    <p style="margin: 0 0 20px; font-size: 13px; color: #94a3b8;">%s</p>
                    <a href="%s/friends" style="display: inline-block; padding: 12px 28px; background: #4A90E2; color: #ffffff; text-decoration: none; border-radius: 10px; font-weight: 700; font-size: 14px;">查看私信</a>
                  </div>
                  <p style="text-align: center; margin-top: 24px; font-size: 12px; color: #94a3b8;">此邮件由 BikeShare 系统自动发送，请勿回复。</p>
                </div>
                """.formatted(
                escapeHtml(senderName),
                escapeHtml(messagePreview),
                LocalDateTime.now().format(DATE_FORMATTER),
                frontendUrl
        );
    }

    private String buildCommentHtml(String commenterName, String postTitle, String commentContent, Long postId) {
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 32px 24px; background: #f8fafc; border-radius: 16px;">
                  <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="margin: 0; font-size: 22px; font-weight: 800; color: #ff6b35;">BikeShare</h1>
                  </div>
                  <div style="background: #ffffff; border-radius: 14px; padding: 28px; box-shadow: 0 4px 16px rgba(15,23,42,0.08);">
                    <h2 style="margin: 0 0 16px; font-size: 18px; color: #0f172a;">您的帖子收到了新评论</h2>
                    <p style="margin: 0 0 8px; font-size: 14px; color: #64748b;">帖子：<strong style="color: #0f172a;">%s</strong></p>
                    <div style="background: #f1f5f9; border-radius: 10px; padding: 16px; margin-bottom: 20px;">
                      <p style="margin: 0 0 8px; font-size: 14px; color: #64748b;">%s 评论道：</p>
                      <p style="margin: 0; font-size: 15px; color: #334155; line-height: 1.6;">%s</p>
                    </div>
                    <p style="margin: 0 0 20px; font-size: 13px; color: #94a3b8;">%s</p>
                    <a href="%s/forum?postId=%d" style="display: inline-block; padding: 12px 28px; background: #4A90E2; color: #ffffff; text-decoration: none; border-radius: 10px; font-weight: 700; font-size: 14px;">查看评论</a>
                  </div>
                  <p style="text-align: center; margin-top: 24px; font-size: 12px; color: #94a3b8;">此邮件由 BikeShare 系统自动发送，请勿回复。</p>
                </div>
                """.formatted(
                escapeHtml(postTitle),
                escapeHtml(commenterName),
                escapeHtml(commentContent),
                LocalDateTime.now().format(DATE_FORMATTER),
                frontendUrl,
                postId
        );
    }

    private String buildSystemHtml(String title, String content, String actionUrl) {
        String buttonHtml = "";
        if (StringUtils.hasText(actionUrl)) {
            buttonHtml = """
                    <a href="%s" style="display: inline-block; padding: 12px 28px; background: #4A90E2; color: #ffffff; text-decoration: none; border-radius: 10px; font-weight: 700; font-size: 14px; margin-top: 8px;">查看详情</a>
                    """.formatted(escapeHtml(actionUrl));
        }
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 32px 24px; background: #f8fafc; border-radius: 16px;">
                  <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="margin: 0; font-size: 22px; font-weight: 800; color: #ff6b35;">BikeShare</h1>
                  </div>
                  <div style="background: #ffffff; border-radius: 14px; padding: 28px; box-shadow: 0 4px 16px rgba(15,23,42,0.08);">
                    <h2 style="margin: 0 0 16px; font-size: 18px; color: #0f172a;">%s</h2>
                    <div style="background: #f1f5f9; border-radius: 10px; padding: 16px; margin-bottom: 20px;">
                      <p style="margin: 0; font-size: 15px; color: #334155; line-height: 1.6;">%s</p>
                    </div>
                    <p style="margin: 0 0 20px; font-size: 13px; color: #94a3b8;">%s</p>
                    %s
                  </div>
                  <p style="text-align: center; margin-top: 24px; font-size: 12px; color: #94a3b8;">此邮件由 BikeShare 系统自动发送，请勿回复。</p>
                </div>
                """.formatted(
                escapeHtml(title),
                escapeHtml(content),
                LocalDateTime.now().format(DATE_FORMATTER),
                buttonHtml
        );
    }

    private String buildReviewResultHtml(String targetType, String targetTitle, boolean approved, Long targetId) {
        String statusText = approved ? "审核通过" : "审核驳回";
        String statusColor = approved ? "#16a34a" : "#dc2626";
        String statusBg = approved ? "#f0fdf4" : "#fef2f2";
        String statusBorder = approved ? "#bbf7d0" : "#fecaca";
        String actionUrl = "forum".equals(targetType) ? frontendUrl + "/forum?postId=" + targetId : frontendUrl + "/forum";

        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 32px 24px; background: #f8fafc; border-radius: 16px;">
                  <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="margin: 0; font-size: 22px; font-weight: 800; color: #ff6b35;">BikeShare</h1>
                  </div>
                  <div style="background: #ffffff; border-radius: 14px; padding: 28px; box-shadow: 0 4px 16px rgba(15,23,42,0.08);">
                    <h2 style="margin: 0 0 16px; font-size: 18px; color: #0f172a;">%s审核结果</h2>
                    <div style="background: %s; border: 1px solid %s; border-radius: 10px; padding: 16px; margin-bottom: 20px;">
                      <p style="margin: 0 0 8px; font-size: 14px; color: #64748b;">%s：<strong style="color: #0f172a;">%s</strong></p>
                      <p style="margin: 0; font-size: 16px; font-weight: 700; color: %s;">%s</p>
                    </div>
                    <p style="margin: 0 0 20px; font-size: 13px; color: #94a3b8;">%s</p>
                    <a href="%s" style="display: inline-block; padding: 12px 28px; background: #4A90E2; color: #ffffff; text-decoration: none; border-radius: 10px; font-weight: 700; font-size: 14px;">查看详情</a>
                  </div>
                  <p style="text-align: center; margin-top: 24px; font-size: 12px; color: #94a3b8;">此邮件由 BikeShare 系统自动发送，请勿回复。</p>
                </div>
                """.formatted(
                escapeHtml(targetType),
                statusBg,
                statusBorder,
                escapeHtml(targetType),
                escapeHtml(targetTitle),
                statusColor,
                statusText,
                LocalDateTime.now().format(DATE_FORMATTER),
                actionUrl
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
