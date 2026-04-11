package com.example.bickdemo.service;

import reactor.core.publisher.Flux;

/**
 * AI 对话服务接口
 */
public interface AiChatService {

    /**
     * 流式对话
     * @param userMessage 用户消息
     * @param historyJson 对话历史 JSON
     * @return 流式响应
     */
    Flux<String> chatStream(String userMessage, String historyJson);
}
