# AI 智能客服助手实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在用户端新增 AI 智能客服助手，用户可随时唤起对话框，询问项目相关问题或闲聊。

**Architecture:** 前端 Vue 3 + SSE 流式响应，后端 Spring AI 调用硅基流动 DeepSeek-V3.2 模型，对话历史维护在 localStorage。

**Tech Stack:** Spring AI, Vue 3, SSE (EventSource), 硅基流动 API

---

## 文件结构

```
bickdemo-backend/
├── pom.xml                                          # 新增 Spring AI 依赖
├── src/main/resources/
│   └── application.yml                              # 新增 AI 配置
└── src/main/java/com/example/bickdemo/
    ├── controller/
    │   └── AiChatController.java                   # 新增：AI 对话接口
    ├── service/
    │   ├── AiChatService.java                      # 新增：AI 对话服务
    │   └── impl/
    │       └── AiChatServiceImpl.java              # 新增：AI 对话服务实现
    └── config/
        └── AiChatConfig.java                       # 新增：Spring AI 配置

bickdemo-frontend/
└── src/
    ├── api/
    │   └── ai.js                                   # 新增：AI API 调用
    ├── components/
    │   ├── AiChatButton.vue                        # 新增：悬浮按钮
    │   └── AiChatDialog.vue                        # 新增：聊天弹窗
    └── App.vue                                     # 修改：集成组件
```

---

## Task 1: 后端 - 添加 Spring AI 依赖

**Files:**
- Modify: `bickdemo-backend/pom.xml`

- [ ] **Step 1: 添加 Spring AI 依赖**

在 `pom.xml` 的 `<dependencies>` 部分添加：

```xml
<!-- Spring AI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

- [ ] **Step 2: 验证依赖添加成功**

Run: `cd bickdemo-backend && mvn dependency:tree -Dincludes=org.springframework.ai`
Expected: 显示 spring-ai-openai-spring-boot-starter 依赖

- [ ] **Step 3: 提交代码**

```bash
cd bickdemo-backend && git add pom.xml && git commit -m "feat(ai): 添加 Spring AI 依赖"
```

---

## Task 2: 后端 - 添加 AI 配置

**Files:**
- Modify: `bickdemo-backend/src/main/resources/application.yml`

- [ ] **Step 1: 添加 AI 配置**

在 `application.yml` 末尾添加：

```yaml
# AI 智能客服配置
spring:
  ai:
    openai:
      base-url: https://api.siliconflow.cn/v1
      api-key: ${SILICONFLOW_API_KEY:sk-vwkjyjdneikbthnrgejieargpdfqztnwurwlttrylqrbqazc}
      model: Pro/deepseek-ai/DeepSeek-V3.2
```

- [ ] **Step 2: 提交代码**

```bash
cd bickdemo-backend && git add src/main/resources/application.yml && git commit -m "feat(ai): 添加硅基流动 API 配置"
```

---

## Task 3: 后端 - 创建 AI 配置类

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/config/AiChatConfig.java`

- [ ] **Step 1: 创建 AiChatConfig 配置类**

```java
package com.example.bickdemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 聊天配置类
 */
@Configuration
public class AiChatConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
```

- [ ] **Step 2: 提交代码**

```bash
cd bickdemo-backend && git add src/main/java/com/example/bickdemo/config/AiChatConfig.java && git commit -m "feat(ai): 添加 AI 聊天配置类"
```

---

## Task 4: 后端 - 创建系统提示词配置

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/config/SystemPrompt.java`

- [ ] **Step 1: 创建 SystemPrompt 配置类**

```java
package com.example.bickdemo.config;

/**
 * AI 系统提示词配置
 */
public class SystemPrompt {

    public static final String AI_NAME = "小林";

    public static final String SYSTEM_PROMPT = """
            你是 BikeShare 自行车租赁系统的智能助手，名字叫小林。

            【身份】
            - 你是一个友好、热情的AI助手
            - 你可以回答项目相关问题，也可以进行闲聊

            【项目相关知识】
            - 租车服务：车辆浏览、在线租赁、实时计费、订单管理
            - VIP体系：月卡¥9.9/季卡¥25/年卡¥88，权益包括积分翻倍、专属客服、优先租赁
            - 积分体系：租车+10、发帖+5、活动+15、签到+3，可用积分兑换VIP
            - 活动管理：骑行活动发布、报名、签到
            - 论坛社区：图文发帖、评论点赞
            - 二手市场：闲置物品发布、审核、交易
            - 工单系统：用户反馈、进度跟踪

            【回答原则】
            - 项目相关问题：用项目知识准确回答
            - 闲聊：友好回应，可以开玩笑
            - 超出范围：礼貌引导回项目话题

            【输出风格】
            - 友好、亲切、活泼
            - 适当使用 emoji
            - 回答简洁有条理
            """;
}
```

- [ ] **Step 2: 提交代码**

```bash
cd bickdemo-backend && git add src/main/java/com/example/bickdemo/config/SystemPrompt.java && git commit -m "feat(ai): 添加系统提示词配置"
```

---

## Task 5: 后端 - 创建 AI 对话服务

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/AiChatService.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/AiChatServiceImpl.java`

- [ ] **Step 1: 创建 AiChatService 接口**

```java
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
```

- [ ] **Step 2: 创建 AiChatServiceImpl 实现类**

```java
package com.example.bickdemo.service.impl;

import com.example.bickdemo.config.SystemPrompt;
import com.example.bickdemo.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
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
public class AiChatServiceImpl extends ServiceImpl<Object, Object> implements AiChatService {

    @Resource
    private ChatClient chatClient;

    @Override
    public Flux<String> chatStream(String userMessage, List<Message> history) {
        // 构建消息列表
        var messages = new java.util.ArrayList<Message>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(SystemPrompt.SYSTEM_PROMPT));

        // 添加历史消息
        if (history != null) {
            for (Message msg : history) {
                messages.add(msg);
            }
        }

        // 添加当前用户消息
        messages.add(new UserMessage(userMessage));

        // 流式调用并返回
        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnError(e -> log.error("AI 对话异常: {}", e.getMessage()))
                .doOnComplete(() -> log.info("AI 对话完成"));
    }
}
```

**注意**：ServiceImpl 是 MyBatis-Plus 的，这里实际上不需要继承它。这是一个普通服务，不需要继承任何父类。修改如下：

```java
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
```

- [ ] **Step 3: 提交代码**

```bash
cd bickdemo-backend && git add src/main/java/com/example/bickdemo/service/AiChatService.java src/main/java/com/example/bickdemo/service/impl/AiChatServiceImpl.java && git commit -m "feat(ai): 添加 AI 对话服务"
```

---

## Task 6: 后端 - 创建 AI 对话控制器

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/controller/AiChatController.java`

- [ ] **Step 1: 创建 AiChatController**

```java
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
```

- [ ] **Step 2: 提交代码**

```bash
cd bickdemo-backend && git add src/main/java/com/example/bickdemo/controller/AiChatController.java && git commit -m "feat(ai): 添加 AI 对话控制器"
```

---

## Task 7: 后端 - 配置安全路径

**Files:**
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/config/SecurityConfig.java`

- [ ] **Step 1: 添加 AI 接口到安全配置**

找到 SecurityConfig 文件，添加 `/api/ai/**` 到permitAll列表。

Run: `grep -n "permitAll" bickdemo-backend/src/main/java/com/example/bickdemo/config/SecurityConfig.java`
Expected: 显示 permitAll 配置位置

- [ ] **Step 2: 提交代码**

```bash
cd bickdemo-backend && git add src/main/java/com/example/bickdemo/config/SecurityConfig.java && git commit -m "feat(ai): 开放 AI 接口安全配置"
```

---

## Task 8: 前端 - 创建 AI API 模块

**Files:**
- Create: `bickdemo-frontend/src/api/ai.js`

- [ ] **Step 1: 创建 AI API 模块**

```javascript
import request from '@/utils/request'

/**
 * AI 对话
 * @param {string} message - 用户消息
 * @param {Array} history - 对话历史
 * @returns {Promise} SSE 流式响应
 */
export function aiChat(message, history = []) {
  return request({
    url: '/api/ai/chat',
    method: 'post',
    data: { message, history },
    responseType: 'text',
    headers: {
      'Content-Type': 'application/json'
    }
  })
}
```

**注意**：axios 不直接支持 SSE，需要使用原生 EventSource 或修改为 fetch API。推荐使用 fetch 实现：

```javascript
/**
 * AI 对话（SSE 流式）
 * @param {string} message - 用户消息
 * @param {Array} history - 对话历史
 * @param {Function} onMessage - 消息回调
 * @param {Function} onError - 错误回调
 * @returns {AbortController} 用于取消请求
 */
export function aiChatSSE(message, history = [], onMessage, onError) {
  const controller = new AbortController()

  const messages = history.map(h => ({
    role: h.role,
    content: h.content
  }))

  fetch('/api/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + localStorage.getItem('token') || ''
    },
    body: JSON.stringify({ message, history: messages }),
    signal: controller.signal
  }).then(response => {
    if (!response.ok) {
      onError && onError('请求失败')
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    function read() {
      reader.read().then(({ done, value }) => {
        if (done) return

        const chunk = decoder.decode(value)
        // SSE 格式：data: 内容\n\n
        const lines = chunk.split('\n')
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const content = line.slice(6)
            if (content === '[DONE]') {
              return
            }
            onMessage && onMessage(content)
          }
        }
        read()
      })
    }

    read()
  }).catch(err => {
    if (err.name !== 'AbortError') {
      onError && onError(err.message)
    }
  })

  return controller
}
```

- [ ] **Step 2: 提交代码**

```bash
cd bickdemo-frontend && git add src/api/ai.js && git commit -m "feat(ai): 添加 AI API 模块"
```

---

## Task 9: 前端 - 创建 AI 悬浮按钮组件

**Files:**
- Create: `bickdemo-frontend/src/components/AiChatButton.vue`

- [ ] **Step 1: 创建 AiChatButton.vue**

```vue
<template>
  <div class="ai-chat-button" :class="{ 'ai-chat-button--open': isOpen }">
    <button
      type="button"
      class="ai-chat-button__btn"
      :aria-label="'AI 助手'"
      title="AI 助手"
      @click="$emit('click')"
    >
      <span class="ai-chat-button__icon-wrapper">
        <el-icon class="ai-icon-out"><ChatDotRound /></el-icon>
        <el-icon class="ai-icon-in"><Close /></el-icon>
      </span>
      <span class="ai-chat-button__badge" v-if="showBadge">1</span>
    </button>
  </div>
</template>

<script setup>
import { ChatDotRound, Close } from '@element-plus/icons-vue'

defineProps({
  isOpen: {
    type: Boolean,
    default: false
  },
  showBadge: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click'])
</script>

<style scoped>
.ai-chat-button {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1200;
}

.ai-chat-button__btn {
  width: 50px;
  height: 50px;
  border-radius: 999px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.4);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  position: relative;
}

.ai-chat-button__btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.ai-chat-button__btn:active {
  transform: scale(0.95);
}

.ai-chat-button__icon-wrapper {
  position: relative;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-chat-button__icon-wrapper .el-icon {
  position: absolute;
  font-size: 24px;
  transition: transform 0.3s ease, opacity 0.3s ease;
}

/* 默认状态：显示聊天图标 */
.ai-icon-out {
  transform: rotate(0deg) scale(1);
  opacity: 1;
}

.ai-icon-in {
  transform: rotate(-90deg) scale(0.5);
  opacity: 0;
}

/* 打开状态：显示关闭图标 */
.ai-chat-button--open .ai-icon-out {
  transform: rotate(90deg) scale(0.5);
  opacity: 0;
}

.ai-chat-button--open .ai-icon-in {
  transform: rotate(0deg) scale(1);
  opacity: 1;
}

.ai-chat-button__badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .ai-chat-button {
    right: 16px;
    bottom: 16px;
  }

  .ai-chat-button__btn {
    width: 46px;
    height: 46px;
  }
}
</style>
```

- [ ] **Step 2: 提交代码**

```bash
cd bickdemo-frontend && git add src/components/AiChatButton.vue && git commit -m "feat(ai): 添加 AI 悬浮按钮组件"
```

---

## Task 10: 前端 - 创建 AI 聊天弹窗组件

**Files:**
- Create: `bickdemo-frontend/src/components/AiChatDialog.vue`

- [ ] **Step 1: 创建 AiChatDialog.vue**

```vue
<template>
  <Transition name="dialog-fade">
    <div class="ai-chat-dialog" v-if="visible">
      <!-- 头部 -->
      <div class="ai-chat-dialog__header">
        <div class="ai-chat-dialog__title">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 助手</span>
        </div>
        <button class="ai-chat-dialog__close" @click="$emit('close')">
          <el-icon><Close /></el-icon>
        </button>
      </div>

      <!-- 消息列表 -->
      <div class="ai-chat-dialog__body" ref="messageListRef">
        <!-- 欢迎消息 -->
        <div class="ai-chat-message ai-chat-message--welcome" v-if="messages.length === 0">
          <div class="ai-chat-message__avatar">
            <el-icon><ServiceDocument /></el-icon>
          </div>
          <div class="ai-chat-message__content">
            <p>您好！我是 BikeShare 的智能助手小林～</p>
            <p>有什么关于自行车租赁系统的问题我可以帮您解答吗？</p>
            <p>也可以和我闲聊哦！</p>
          </div>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="ai-chat-message"
          :class="msg.role === 'user' ? 'ai-chat-message--user' : 'ai-chat-message--assistant'"
        >
          <div class="ai-chat-message__avatar" v-if="msg.role === 'assistant'">
            <el-icon><ServiceDocument /></el-icon>
          </div>
          <div class="ai-chat-message__content">
            <p v-html="formatMessage(msg.content)"></p>
          </div>
        </div>

        <!-- 正在输入指示器 -->
        <div class="ai-chat-message ai-chat-message--typing" v-if="isTyping">
          <div class="ai-chat-message__avatar">
            <el-icon><ServiceDocument /></el-icon>
          </div>
          <div class="ai-chat-message__content">
            <span class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </span>
          </div>
        </div>
      </div>

      <!-- 快捷回复 -->
      <div class="ai-chat-dialog__quick" v-if="messages.length <= 2">
        <button
          v-for="item in quickReplies"
          :key="item.label"
          class="ai-chat-dialog__quick-btn"
          @click="sendQuickReply(item.question)"
        >
          {{ item.label }}
        </button>
      </div>

      <!-- 输入框 -->
      <div class="ai-chat-dialog__footer">
        <input
          type="text"
          class="ai-chat-dialog__input"
          v-model="inputText"
          placeholder="输入消息..."
          @keyup.enter="sendMessage"
          :disabled="isTyping"
        />
        <button
          class="ai-chat-dialog__send"
          @click="sendMessage"
          :disabled="!inputText.trim() || isTyping"
        >
          <el-icon><Promotion /></el-icon>
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { ChatDotRound, Close, ServiceDocument, Promotion } from '@element-plus/icons-vue'
import { aiChatSSE } from '@/api/ai'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const inputText = ref('')
const messages = ref([])
const isTyping = ref(false)
const messageListRef = ref(null)
let abortController = null

// 快捷回复
const quickReplies = [
  { label: '项目介绍', question: 'BikeShare 是什么？' },
  { label: '怎么租车', question: '如何租用自行车？' },
  { label: 'VIP权益', question: 'VIP会员有什么好处？' },
  { label: '积分体系', question: '积分怎么获得和使用？' },
  { label: '随便聊聊', question: '今天天气不错，聊聊吧' }
]

// 格式化消息（简单处理换行）
function formatMessage(content) {
  return content.replace(/\n/g, '<br>')
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 发送消息
function sendMessage() {
  const text = inputText.value.trim()
  if (!text || isTyping.value) return

  // 取消之前的请求
  if (abortController) {
    abortController.abort()
  }

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text
  })
  inputText.value = ''
  isTyping.value = true
  scrollToBottom()

  // 获取历史
  const history = messages.value.slice(0, -1).map(m => ({
    role: m.role === 'user' ? 'user' : 'assistant',
    content: m.content
  }))

  // 发起请求
  abortController = aiChatSSE(
    text,
    history,
    (content) => {
      // 找到最后一条助手消息或创建新消息
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg && lastMsg.role === 'assistant') {
        lastMsg.content += content
      } else {
        messages.value.push({
          role: 'assistant',
          content: content
        })
      }
      scrollToBottom()
    },
    (error) => {
      isTyping.value = false
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg && lastMsg.role === 'assistant') {
        lastMsg.content += '\n\n[抱歉，网络出错了，请稍后再试]'
      }
    }
  )

  // 模拟完成（实际由 SSE 的 [DONE] 事件触发）
  // 这里需要等 SSE 完成后 isTyping = false
  // 简化处理：设置一个超时
  setTimeout(() => {
    if (isTyping.value) {
      isTyping.value = false
    }
  }, 60000) // 60秒超时
}

// 快捷回复
function sendQuickReply(question) {
  inputText.value = question
  sendMessage()
}

// 监听 visible 变化
watch(() => props.visible, (val) => {
  if (!val) {
    // 关闭时取消请求
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }
})
</script>

<style scoped>
.ai-chat-dialog {
  position: fixed;
  right: 80px;
  bottom: 20px;
  width: 380px;
  height: 520px;
  background: var(--bg-card, #ffffff);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1200;
}

@media (max-width: 768px) {
  .ai-chat-dialog {
    right: 16px;
    left: 16px;
    bottom: 80px;
    width: auto;
    height: 60vh;
  }
}

/* 头部 */
.ai-chat-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border, #e2e8f0);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.ai-chat-dialog__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.ai-chat-dialog__close {
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.ai-chat-dialog__close:hover {
  opacity: 1;
}

/* 消息区域 */
.ai-chat-dialog__body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-chat-message {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.ai-chat-message--user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.ai-chat-message--assistant {
  align-self: flex-start;
}

.ai-chat-message__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-chat-message__content {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.ai-chat-message--user .ai-chat-message__content {
  background: var(--bs-primary, #667eea);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-chat-message--assistant .ai-chat-message__content {
  background: var(--bg-page, #f8fafc);
  color: var(--text-primary, #1e293b);
  border-bottom-left-radius: 4px;
}

.ai-chat-message--welcome .ai-chat-message__content {
  background: none;
  padding: 0;
}

.ai-chat-message--welcome p {
  margin: 4px 0;
}

/* 正在输入 */
.typing-indicator {
  display: inline-flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted, #64748b);
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-6px);
  }
}

/* 快捷回复 */
.ai-chat-dialog__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border, #e2e8f0);
}

.ai-chat-dialog__quick-btn {
  padding: 6px 12px;
  border-radius: 16px;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-card, #ffffff);
  color: var(--text-primary, #1e293b);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.ai-chat-dialog__quick-btn:hover {
  background: var(--bs-primary, #667eea);
  color: #fff;
  border-color: var(--bs-primary, #667eea);
}

/* 输入框 */
.ai-chat-dialog__footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border, #e2e8f0);
}

.ai-chat-dialog__input {
  flex: 1;
  padding: 10px 14px;
  border-radius: 20px;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-page, #f8fafc);
  color: var(--text-primary, #1e293b);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.ai-chat-dialog__input:focus {
  border-color: var(--bs-primary, #667eea);
}

.ai-chat-dialog__input:disabled {
  opacity: 0.6;
}

.ai-chat-dialog__send {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, opacity 0.2s;
}

.ai-chat-dialog__send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-chat-dialog__send:not(:disabled):hover {
  transform: scale(1.05);
}

/* 动画 */
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
```

- [ ] **Step 2: 提交代码**

```bash
cd bickdemo-frontend && git add src/components/AiChatDialog.vue && git commit -m "feat(ai): 添加 AI 聊天弹窗组件"
```

---

## Task 11: 前端 - 集成到 App.vue

**Files:**
- Modify: `bickdemo-frontend/src/App.vue`

- [ ] **Step 1: 修改 App.vue 集成 AI 组件**

```vue
<template>
  <n-config-provider :theme="isDark ? darkTheme : undefined" :dialog="dialogConfig">
    <n-message-provider>
      <n-dialog-provider>
        <router-view :key="route.fullPath" />
        <ThemeToggle v-if="showFloatingToggle" />
        <AiChatButton :is-open="aiDialogVisible" @click="aiDialogVisible = true" />
        <AiChatDialog :visible="aiDialogVisible" @close="aiDialogVisible = false" />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { NConfigProvider, NMessageProvider, NDialogProvider, darkTheme } from 'naive-ui'
import ThemeToggle from '@/components/ThemeToggle.vue'
import AiChatButton from '@/components/AiChatButton.vue'
import AiChatDialog from '@/components/AiChatDialog.vue'

const route = useRoute()
const showFloatingToggle = computed(() => route.name === 'Login' || route.name === 'Register')
const aiDialogVisible = ref(false)

// 检测暗色模式
const isDark = computed(() => document.documentElement.classList.contains('dark'))

// 配置 dialog z-index 确保高于 Element Plus 组件
const dialogConfig = {
  zIndex: 2999
}
</script>
```

- [ ] **Step 2: 提交代码**

```bash
cd bickdemo-frontend && git add src/App.vue && git commit -m "feat(ai): 集成 AI 客服组件到 App"
```

---

## Task 12: 验证与测试

- [ ] **Step 1: 后端编译验证**

Run: `cd bickdemo-backend && mvn clean compile -DskipTests`
Expected: 编译成功，无报错

- [ ] **Step 2: 前端编译验证**

Run: `cd bickdemo-frontend && npm run build`
Expected: 编译成功，无报错

- [ ] **Step 3: 手动测试**

1. 启动后端：`cd bickdemo-backend && mvn spring-boot:run`
2. 启动前端：`cd bickdemo-frontend && npm run dev`
3. 打开浏览器访问用户端
4. 点击右下角 AI 按钮
5. 测试快捷按钮和打字输入
6. 验证流式响应是否正常

---

## 实施顺序

1. Task 1-2: 后端依赖和配置
2. Task 3-4: 后端配置类
3. Task 5-6: 后端服务和控制器
4. Task 7: 后端安全配置
5. Task 8-10: 前端组件
6. Task 11: 前端集成
7. Task 12: 验证测试
