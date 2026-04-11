package com.example.bickdemo.service.impl;

import com.example.bickdemo.config.SystemPrompt;
import com.example.bickdemo.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Map;

/**
 * AI 对话服务实现
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Flux<String> chatStream(String userMessage, String historyJson) {
        return Flux.create(sink -> {
            try {
                // 构建请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("Accept", "text/event-stream");

                // 构建请求体
                String requestBody = buildRequestBody(userMessage, historyJson);

                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                // 发送请求
                String response = restTemplate.postForObject(
                        URI.create(baseUrl + "/chat/completions"),
                        entity,
                        String.class
                );

                if (response != null) {
                    // 解析 SSE 格式
                    String[] lines = response.split("\n");
                    StringBuilder fullContent = new StringBuilder();
                    for (String line : lines) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                break;
                            }
                            // 解析 JSON 中的 content
                            int contentStart = data.indexOf("\"content\":\"");
                            if (contentStart >= 0) {
                                int start = contentStart + 10;
                                int end = data.indexOf("\"", start);
                                if (end > start) {
                                    String content = data.substring(start, end);
                                    fullContent.append(content);
                                    sink.next(content);
                                }
                            }
                        }
                    }
                }

                sink.complete();
                log.info("AI 对话完成");

            } catch (Exception e) {
                log.error("AI 对话异常: {}", e.getMessage());
                sink.error(e);
            }
        });
    }

    private String buildRequestBody(String userMessage, String historyJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(model).append("\",\"stream\":true,\"messages\":[");
        sb.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(SystemPrompt.SYSTEM_PROMPT)).append("\"}");

        if (historyJson != null && !historyJson.isEmpty()) {
            sb.append(",").append(historyJson);
        }

        sb.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}");
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
