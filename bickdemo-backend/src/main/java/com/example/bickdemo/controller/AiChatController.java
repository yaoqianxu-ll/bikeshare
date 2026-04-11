package com.example.bickdemo.controller;

import com.example.bickdemo.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * AI 智能客服控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    /**
     * 流式对话接口
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, String>> historyList = (java.util.List<Map<String, String>>) request.get("history");

        // 转换历史消息为 JSON 格式
        String historyJson = null;
        if (historyList != null && !historyList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map<String, String> m : historyList) {
                String role = m.get("role");
                String content = m.get("content");
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append("{\"role\":\"").append(role == null ? "user" : role)
                  .append("\",\"content\":\"").append(content == null ? "" : content.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"}");
            }
            historyJson = sb.toString();
        }

        log.info("AI 对话请求: {}", message);
        return aiChatService.chatStream(message, historyJson);
    }
}
