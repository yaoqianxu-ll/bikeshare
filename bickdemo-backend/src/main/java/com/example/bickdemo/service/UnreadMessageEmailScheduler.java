package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.entity.ChatMessage;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.ChatMessageMapper;
import com.example.bickdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 私信未读邮件提醒定时任务。
 * 每 2 分钟扫描一次，查找最近 10 分钟内产生、且超过 2 分钟仍未被阅读的私信，
 * 通过邮件提醒接收者。同一发送者对同一接收者 1 小时内只提醒一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnreadMessageEmailScheduler {

    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final UserEmailNotificationService emailNotificationService;

    @Scheduled(fixedRate = 120000)
    public void checkUnreadMessages() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
            // 只看最近 10 分钟内产生的消息，避免反复扫描历史旧消息
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(10);

            // 查询最近 10 分钟内、超过 2 分钟仍未读的消息
            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> w.eq(ChatMessage::getRead, false).or().isNull(ChatMessage::getRead))
                    .isNull(ChatMessage::getReadAt)
                    .eq(ChatMessage::getDeleted, 0)
                    .le(ChatMessage::getCreatedAt, threshold)
                    .ge(ChatMessage::getCreatedAt, windowStart);

            List<ChatMessage> unreadMessages = chatMessageMapper.selectList(wrapper);

            if (unreadMessages.isEmpty()) {
                return;
            }

            log.info("发现 {} 条超时未读私信，开始处理邮件提醒", unreadMessages.size());

            // 按 (receiverId, senderId) 分组，每组只取最新的一条消息作为提醒内容
            Map<String, ChatMessage> groupedMessages = unreadMessages.stream()
                    .collect(Collectors.toMap(
                            msg -> msg.getReceiverId() + "_" + msg.getSenderId(),
                            msg -> msg,
                            (existing, replacement) -> existing.getCreatedAt().isAfter(replacement.getCreatedAt()) ? existing : replacement
                    ));

            for (ChatMessage msg : groupedMessages.values()) {
                try {
                    User receiver = userMapper.selectById(msg.getReceiverId());
                    User sender = userMapper.selectById(msg.getSenderId());

                    if (receiver == null || sender == null) {
                        continue;
                    }

                    // 根据消息类型生成预览内容
                    String preview;
                    if (msg.getType() != null) {
                        switch (msg.getType()) {
                            case IMAGE -> preview = "发送了一张图片";
                            case EMOJI -> preview = "发送了一个表情";
                            case STICKER -> preview = "发送了一个贴纸";
                            default -> {
                                preview = msg.getContent();
                                if (preview != null && preview.length() > 50) {
                                    preview = preview.substring(0, 50) + "...";
                                }
                            }
                        }
                    } else {
                        preview = msg.getContent();
                        if (preview != null && preview.length() > 50) {
                            preview = preview.substring(0, 50) + "...";
                        }
                    }

                    // 兜底：如果预览内容仍为空，给一个默认提示
                    if (preview == null || preview.isBlank()) {
                        preview = "收到了一条消息";
                    }

                    emailNotificationService.sendPrivateMessageEmail(
                            receiver,
                            sender.getUsername(),
                            preview,
                            sender.getId()
                    );
                } catch (Exception e) {
                    log.error("处理私信提醒邮件失败，msgId={}, error={}", msg.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("私信未读邮件定时任务执行异常: {}", e.getMessage(), e);
        }
    }
}
