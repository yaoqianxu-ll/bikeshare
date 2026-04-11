package com.example.bickdemo.controller;

import com.example.bickdemo.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * AI 智能客服控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 客服")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    /**
     * 流式对话接口
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话")
    public Flux<String> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> historyList = (List<Map<String, String>>) request.get("history");

        // 转换历史消息
        List<Message> history = null;
        if (historyList != null) {
            history = historyList.stream()
                    .map(m -> {
                        String role = m.get("role");
                        String content = m.get("content");
                        if ("user".equals(role)) {
                            return new org.springframework.ai.chat.messages.UserMessage(content);
                        } else {
                            return new org.springframework.ai.chat.messages.AssistantMessage(content);
                        }
                    })
                    .toList();
        }

        log.info("AI 对话请求: {}", message);
        return aiChatService.chatStream(message, history);
    }
}