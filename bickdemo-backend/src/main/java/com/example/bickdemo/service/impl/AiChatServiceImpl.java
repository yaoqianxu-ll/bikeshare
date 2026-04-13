package com.example.bickdemo.service.impl;

import com.example.bickdemo.service.AiChatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话服务实现
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private ChatClient aiChatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Flux<String> chatStream(String userMessage, String historyJson) {
        List<Message> messages = new ArrayList<>();

        // 解析历史消息
        if (historyJson != null && !historyJson.isEmpty()) {
            try {
                List<HistoryItem> history = objectMapper.readValue(
                        "[" + historyJson + "]",
                        new TypeReference<List<HistoryItem>>() {}
                );
                for (HistoryItem item : history) {
                    if ("user".equals(item.role)) {
                        messages.add(new UserMessage(item.content));
                    } else if ("assistant".equals(item.role)) {
                        messages.add(new AssistantMessage(item.content));
                    } else if ("system".equals(item.role)) {
                        messages.add(new SystemMessage(item.content));
                    }
                }
            } catch (Exception e) {
                // 解析失败，忽略历史
            }
        }

        // 添加当前用户消息
        messages.add(new UserMessage(userMessage));

        return aiChatClient.prompt()
                .messages(messages)
                .stream()
                .content();
    }

    private static class HistoryItem {
        public String role;
        public String content;
    }
}
