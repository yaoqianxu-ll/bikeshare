import re

with open('bickdemo-backend/src/main/java/com/example/bickdemo/service/SocialService.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_code = '''        ChatMessageResponse response = toChatMessageResponse(message, currentUser.getId(), currentUser, receiver);
        ChatMessageResponse receiverResponse = toChatMessageResponse(message, receiver.getId(), receiver, currentUser);
        // 发送方拿到自己的响应，接收方则通过实时事件收到另一份"面向接收者视角"的响应。
        socialEventPublisher.publish(new SocialWsEvent(
                SocialEventType.CHAT_MESSAGE,
                receiver.getUsername(),
                null,
                receiverResponse,
                currentUser.getId(),
                "New private message",
                null
        ));

        return response;'''

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

        return response;'''

if old_code in content:
    content = content.replace(old_code, new_code)
    with open('bickdemo-backend/src/main/java/com/example/bickdemo/service/SocialService.java', 'w', encoding='utf-8') as f:
        f.write(content)
    print('Successfully replaced')
else:
    print('Pattern not found')
    # Let's print what we're looking for
    print("Looking for:")
    print(repr(old_code[:100]))
