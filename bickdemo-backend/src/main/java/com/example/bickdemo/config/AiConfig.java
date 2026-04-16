package com.example.bickdemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置类
 * 配置 Silicon Flow API（OpenAI 兼容接口）
 */
@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    /**
     * 配置 OpenAI Chat 模型（使用 Silicon Flow API）
     */
    @Bean
    public OpenAiChatModel openAiChatModel() {
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel(model);
        options.setTemperature(temperature);
        return new OpenAiChatModel(openAiApi, options);
    }

    /**
     * 配置 AI 客服专用的 ChatClient
     */
    @Bean
    public ChatClient aiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SystemPrompt.SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
