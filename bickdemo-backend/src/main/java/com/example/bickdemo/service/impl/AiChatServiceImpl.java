package com.example.bickdemo.service.impl;

import com.example.bickdemo.config.SystemPrompt;
import com.example.bickdemo.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * AI 对话服务实现
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private ChatClient chatClient;

    @Override
    public Flux<String> chatStream(String userMessage, List<Message> history) {
        // 构建消息列表
        var messages = new java.util.ArrayList<Message>();
        messages.add(new SystemMessage(SystemPrompt.SYSTEM_PROMPT));

        // 添加历史消息
        if (history != null) {
            for (Message msg : history) {
                messages.add(msg);
            }
        }

        // 添加当前用户消息
        messages.add(new UserMessage(userMessage));

        // 流式调用并返回
        return chatClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .doOnError(e -> log.error("AI 对话异常: {}", e.getMessage()));
    }
}