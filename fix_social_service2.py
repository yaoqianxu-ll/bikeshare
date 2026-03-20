with open('bickdemo-backend/src/main/java/com/example/bickdemo/service/SocialService.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# 替换第444-457行 (索引443-456)
new_lines = lines[:444]  # 保留前444行 (0-443)

# 添加新的代码
new_code = '''        ChatMessageResponse response = toChatMessageResponse(message, currentUser.getId(), currentUser, receiver);
        ChatMessageResponse receiverResponse = toChatMessageResponse(message, receiver.getId(), receiver, currentUser);
        
        // 发送方拿到自己的响应，接收方则通过实时事件收到另一份"面向接收者视角"的响应。
        SocialWsEvent event = new SocialWsEvent(
                SocialEventType.CHAT_MESSAGE,
                receiver.getUsername(),
                null,
                receiverResponse,
                currentUser.getId(),
                "New private message",
                null
        );
        log.info("[RabbitMQ] 准备发布聊天消息事件: sender={}, receiver={}, messageId={}", 
                currentUser.getUsername(), receiver.getUsername(), message.getId());
        socialEventPublisher.publish(event);
        log.info("[RabbitMQ] 聊天消息事件已提交: sender={}, receiver={}", 
                currentUser.getUsername(), receiver.getUsername());

        return response;
'''

new_lines.append(new_code)
new_lines.extend(lines[458:])  # 添加第458行及以后的内容

with open('bickdemo-backend/src/main/java/com/example/bickdemo/service/SocialService.java', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print('File updated successfully')
