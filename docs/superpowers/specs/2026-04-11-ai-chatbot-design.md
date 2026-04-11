# AI 智能客服助手设计方案

**日期**：2026-04-11
**状态**：已确认

---

## 1. 功能概述

在用户端新增 AI 智能客服助手，用户可随时唤起对话框，询问项目相关问题或闲聊。

---

## 2. 技术架构

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│  用户端前端  │ ←→  │  后端 AI 模块 │ ←→  │  硅基流动 API   │
│  Vue 组件   │ SSE │  Spring AI   │     │  DeepSeek-V3.2  │
└─────────────┘     └──────────────┘     └─────────────────┘
```

---

## 3. 后端设计

### 3.1 新增文件

| 文件 | 说明 |
|------|------|
| `AiChatController.java` | 接收消息，返回 SSE 流式响应 |
| `AiChatService.java` | 调用 Spring AI，封装硅基流动 API |
| `SystemPrompt.java` | 系统提示词配置 |

### 3.2 依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

### 3.3 配置（application.yml）

```yaml
spring:
  ai:
    openai:
      base-url: https://api.siliconflow.cn/v1
      api-key: ${SILICONFLOW_API_KEY}
      model: Pro/deepseek-ai/DeepSeek-V3.2
```

### 3.4 环境变量

```
SILICONFLOW_API_KEY=sk-vwkjyjdneikbthnrgejieargpdfqztnwurwlttrylqrbqazc
```

### 3.5 接口设计

```
POST /api/ai/chat
Content-Type: text/event-stream

请求：
{
  "message": "用户输入的消息",
  "history": [
    {"role": "user", "content": "历史消息"},
    {"role": "assistant", "content": "历史回复"}
  ]
}

响应：SSE 流式输出
```

---

## 4. 前端设计

### 4.1 新增组件

| 组件 | 说明 |
|------|------|
| `AiChatButton.vue` | 右下角悬浮按钮（铃铛旁边） |
| `AiChatDialog.vue` | 聊天弹窗主组件 |
| `AiChatMessage.vue` | 单条消息展示 |
| `AiQuickReplies.vue` | 快捷回复按钮组 |

### 4.2 交互流程

1. 用户点击悬浮按钮 → 打开聊天弹窗
2. 显示欢迎语 + 快捷按钮
3. 用户点击快捷项或打字 → 发送消息
4. AI 流式响应，打字机效果展示
5. 对话历史存储在 localStorage

### 4.3 快捷按钮

| 按钮文案 | 对应问题 |
|---------|---------|
| 项目介绍 | BikeShare 是什么？ |
| 怎么租车 | 如何租用自行车？ |
| VIP权益 | VIP会员有什么好处？ |
| 积分体系 | 积分怎么获得和使用？ |
| 随便聊聊 | 开启闲聊模式 |

### 4.4 欢迎语

```
您好！我是 BikeShare 的智能助手小林～
有什么关于自行车租赁系统的问题我可以帮您解答吗？
也可以和我闲聊哦！
```

---

## 5. Prompt 设计

```
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
```

---

## 6. 实施步骤

1. **后端** - 引入 Spring AI 依赖，配置硅基流动 API
2. **后端** - 实现 AiChatController，支持 SSE 流式输出
3. **后端** - 配置 SystemPrompt，封装业务知识
4. **前端** - 创建 AiChatButton 悬浮按钮组件
5. **前端** - 创建 AiChatDialog 聊天弹窗组件
6. **前端** - 实现 SSE 接收，打字机效果
7. **前端** - 集成到用户端页面（建议放在导航栏附近）

---

## 7. 技术细节

### 7.1 流式响应（SSE）

后端使用 `StreamingResponseBody` 返回 SSE，前端用 `EventSource` 接收。

### 7.2 对话历史

前端维护最近 10 轮对话历史，通过 `/api/ai/chat` 接口传递。

### 7.3 异常处理

- API 调用失败：返回友好错误提示
- 网络断开：提示用户检查网络
