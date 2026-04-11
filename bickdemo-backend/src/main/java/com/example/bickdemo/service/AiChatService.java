package com.example.bickdemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话服务接口
 */
public interface AiChatService {

    /**
     * 流式对话
     * @param userMessage 用户消息
     * @param history 对话历史
     * @return 流式响应
     */
    Flux<String> chatStream(String userMessage, List<Message> history);
}