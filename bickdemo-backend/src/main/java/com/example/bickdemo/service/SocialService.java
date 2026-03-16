package com.example.bickdemo.service;

import com.example.bickdemo.dto.ChatMessageRequest;
import com.example.bickdemo.dto.ChatMessageResponse;
import com.example.bickdemo.dto.ConversationMessagesResponse;
import com.example.bickdemo.dto.FriendRequestCreateRequest;
import com.example.bickdemo.dto.FriendRequestResponse;
import com.example.bickdemo.dto.MessageReadReceiptResponse;
import com.example.bickdemo.dto.SocialContactResponse;
import com.example.bickdemo.dto.SocialEventType;
import com.example.bickdemo.dto.SocialWsEvent;
import com.example.bickdemo.dto.UserSearchResponse;
import com.example.bickdemo.entity.ChatMessage;
import com.example.bickdemo.entity.ChatMessageType;
import com.example.bickdemo.entity.FriendRequest;
import com.example.bickdemo.entity.FriendRequestStatus;
import com.example.bickdemo.entity.Friendship;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.ChatMessageMapper;
import com.example.bickdemo.mapper.FriendRequestMapper;
import com.example.bickdemo.mapper.FriendshipMapper;
import com.example.bickdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
/**
 * 社交服务。
 * 负责用户搜索、好友申请、联系人列表、私聊消息分页、已读回执和实时事件发布等能力。
 * 该服务既维护数据库中的好友/消息状态，也负责向 WebSocket 推送实时通知。
 */
public class SocialService {

    private static final int SEARCH_LIMIT = 20;
    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 24;
    private static final int MAX_MESSAGE_PAGE_SIZE = 60;
    private static final String RELATION_NONE = "NONE";
    private static final String RELATION_FRIEND = "FRIEND";
    private static final String RELATION_REQUEST_SENT = "REQUEST_SENT";
    private static final String RELATION_REQUEST_RECEIVED = "REQUEST_RECEIVED";

    private final UserMapper userMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final FriendshipMapper friendshipMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SocialEventPublisher socialEventPublisher;

    /**
     * 搜索用户。
     * 搜索结果里会附带与当前用户之间的关系状态，方便前端直接决定展示“加好友/已发送”等按钮。
     */
    public List<UserSearchResponse> searchUsers(String keyword, String currentUsername) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (!StringUtils.hasText(trimmedKeyword)) {
            return List.of();
        }

        User currentUser = requireUser(currentUsername);
        List<User> users = userMapper.searchByUsernameLike(trimmedKeyword, currentUser.getId(), SEARCH_LIMIT);

        return users.stream()
                .map(user -> {
                    FriendRequest pendingRequest = friendRequestMapper.findPendingBetweenUsers(currentUser.getId(), user.getId());
                    return new UserSearchResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getAvatar(),
                            user.getBio(),
                            resolveRelationStatus(currentUser.getId(), user.getId(), pendingRequest),
                            pendingRequest == null ? null : pendingRequest.getId()
                    );
                })
                .toList();
    }

    /**
     * 发起好友申请。
     * 会拦截自己加自己、已是好友、已有待处理申请等重复场景。
     */
    @Transactional
    public FriendRequestResponse createFriendRequest(String currentUsername, FriendRequestCreateRequest request) {
        User currentUser = requireUser(currentUsername);
        User receiver = requireEnabledUser(request.getReceiverId());

        if (Objects.equals(currentUser.getId(), receiver.getId())) {
            throw new RuntimeException("Cannot send a friend request to yourself");
        }

        if (friendshipMapper.existsFriendship(currentUser.getId(), receiver.getId())) {
            throw new RuntimeException("You are already friends");
        }

        FriendRequest pendingRequest = friendRequestMapper.findPendingBetweenUsers(currentUser.getId(), receiver.getId());
        if (pendingRequest != null) {
            if (Objects.equals(pendingRequest.getSenderId(), currentUser.getId())) {
                throw new RuntimeException("Friend request already sent");
            }
            throw new RuntimeException("The other user has already sent you a friend request");
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSenderId(currentUser.getId());
        friendRequest.setReceiverId(receiver.getId());
        friendRequest.setRemark(buildRequestRemark(currentUser.getUsername(), request.getRemark()));
        friendRequest.setStatus(FriendRequestStatus.PENDING);
        friendRequestMapper.insert(friendRequest);

        FriendRequestResponse response = toFriendRequestResponse(friendRequest, currentUser, receiver);
        // 好友申请创建后，立即通过消息队列通知接收方刷新社交面板。
        socialEventPublisher.publish(new SocialWsEvent(
                SocialEventType.FRIEND_REQUEST_CREATED,
                receiver.getUsername(),
                response,
                null,
                currentUser.getId(),
                "New friend request",
                null
        ));

        return response;
    }

    /**
     * 查看收到的待处理好友申请。
     */
    public List<FriendRequestResponse> listPendingReceivedRequests(String currentUsername) {
        User currentUser = requireUser(currentUsername);
        return friendRequestMapper.findPendingReceived(currentUser.getId()).stream()
                .map(this::toFriendRequestResponse)
                .toList();
    }

    /**
     * 查看自己发出的待处理好友申请。
     */
    public List<FriendRequestResponse> listPendingSentRequests(String currentUsername) {
        User currentUser = requireUser(currentUsername);
        return friendRequestMapper.findPendingSent(currentUser.getId()).stream()
                .map(this::toFriendRequestResponse)
                .toList();
    }

    /**
     * 接受好友申请。
     * 接受后会补齐双向 friendship 关系，确保双方联系人列表都能查到彼此。
     */
    @Transactional
    public FriendRequestResponse acceptFriendRequest(String currentUsername, Long requestId) {
        User currentUser = requireUser(currentUsername);
        FriendRequest friendRequest = requireFriendRequest(requestId);

        if (!Objects.equals(friendRequest.getReceiverId(), currentUser.getId())) {
            throw new RuntimeException("You can only handle requests sent to yourself");
        }
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Friend request already handled");
        }

        User sender = requireEnabledUser(friendRequest.getSenderId());
        User receiver = requireEnabledUser(friendRequest.getReceiverId());

        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestMapper.updateById(friendRequest);

        // 好友关系采用双向各存一条记录，方便按 user_id 单边查询联系人。
        ensureFriendship(sender.getId(), receiver.getId());
        ensureFriendship(receiver.getId(), sender.getId());

        FriendRequestResponse response = toFriendRequestResponse(friendRequest, sender, receiver);
        socialEventPublisher.publish(new SocialWsEvent(
                SocialEventType.FRIEND_REQUEST_ACCEPTED,
                sender.getUsername(),
                response,
                null,
                receiver.getId(),
                receiver.getUsername() + " accepted your friend request",
                null
        ));

        return response;
    }

    /**
     * 拒绝好友申请。
     */
    @Transactional
    public FriendRequestResponse rejectFriendRequest(String currentUsername, Long requestId) {
        User currentUser = requireUser(currentUsername);
        FriendRequest friendRequest = requireFriendRequest(requestId);

        if (!Objects.equals(friendRequest.getReceiverId(), currentUser.getId())) {
            throw new RuntimeException("You can only handle requests sent to yourself");
        }
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Friend request already handled");
        }

        User sender = requireEnabledUser(friendRequest.getSenderId());
        User receiver = requireEnabledUser(friendRequest.getReceiverId());

        friendRequest.setStatus(FriendRequestStatus.REJECTED);
        friendRequestMapper.updateById(friendRequest);

        FriendRequestResponse response = toFriendRequestResponse(friendRequest, sender, receiver);
        socialEventPublisher.publish(new SocialWsEvent(
                SocialEventType.FRIEND_REQUEST_REJECTED,
                sender.getUsername(),
                response,
                null,
                receiver.getId(),
                receiver.getUsername() + " rejected your friend request",
                null
        ));

        return response;
    }

    /**
     * 组装联系人列表。
     * 联系人列表不仅包含好友，也包含待处理的好友申请和最近消息预览。
     */
    public List<SocialContactResponse> listContacts(String currentUsername) {
        User currentUser = requireUser(currentUsername);
        Map<Long, SocialContactResponse> contacts = new LinkedHashMap<>();
        Map<Long, LocalDateTime> activityTimes = new LinkedHashMap<>();

        // 第一层先把正式好友装入联系人列表。
        for (Friendship friendship : friendshipMapper.findByUserId(currentUser.getId())) {
            User friend = userMapper.selectById(friendship.getFriendId());
            if (friend == null || !friend.isEnabled()) {
                continue;
            }

            SocialContactResponse contact = new SocialContactResponse(
                    friend.getId(),
                    friend.getUsername(),
                    friend.getAvatar(),
                    friend.getBio(),
                    RELATION_FRIEND,
                    null,
                    null,
                    null,
                    null,
                    friendship.getCreatedAt(),
                    0,
                    true
            );
            contacts.put(friend.getId(), contact);
            activityTimes.put(friend.getId(), friendship.getCreatedAt());
        }

        // 第二层补充“收到的好友申请”，这样前端的联系人侧栏能直接提醒处理。
        for (FriendRequest request : friendRequestMapper.findPendingReceived(currentUser.getId())) {
            User peer = userMapper.selectById(request.getSenderId());
            if (peer == null || !peer.isEnabled()) {
                continue;
            }

            contacts.computeIfAbsent(peer.getId(), key -> new SocialContactResponse(
                    peer.getId(),
                    peer.getUsername(),
                    peer.getAvatar(),
                    peer.getBio(),
                    RELATION_REQUEST_RECEIVED,
                    request.getId(),
                    "INCOMING",
                    null,
                    null,
                    request.getCreatedAt(),
                    0,
                    true
            ));
            activityTimes.putIfAbsent(peer.getId(), request.getCreatedAt());
        }

        // 第三层补充“已发出的好友申请”，方便用户看到请求仍在等待中。
        for (FriendRequest request : friendRequestMapper.findPendingSent(currentUser.getId())) {
            User peer = userMapper.selectById(request.getReceiverId());
            if (peer == null || !peer.isEnabled()) {
                continue;
            }

            contacts.computeIfAbsent(peer.getId(), key -> new SocialContactResponse(
                    peer.getId(),
                    peer.getUsername(),
                    peer.getAvatar(),
                    peer.getBio(),
                    RELATION_REQUEST_SENT,
                    request.getId(),
                    "OUTGOING",
                    null,
                    null,
                    request.getCreatedAt(),
                    0,
                    true
            ));
            activityTimes.putIfAbsent(peer.getId(), request.getCreatedAt());
        }

        for (SocialContactResponse contact : contacts.values()) {
            // 再补齐最近一条消息、未读数和活动时间，用于联系人排序与摘要展示。
            ChatMessage latestMessage = chatMessageMapper.findLatestBetweenUsers(currentUser.getId(), contact.getUserId());
            if (latestMessage != null) {
                contact.setLastMessagePreview(buildMessagePreview(latestMessage));
                contact.setLastMessageTime(latestMessage.getCreatedAt());
                contact.setActivityTime(latestMessage.getCreatedAt());
            } else {
                contact.setActivityTime(activityTimes.get(contact.getUserId()));
            }
            contact.setUnreadCount(chatMessageMapper.countUnreadFromUser(contact.getUserId(), currentUser.getId()));
        }

        List<SocialContactResponse> result = new ArrayList<>(contacts.values());
        result.sort(Comparator.comparing(
                SocialContactResponse::getActivityTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result;
    }

    /**
     * 分页获取会话消息。
     * 拉取会话时会顺便把对方发来的未读消息标记为已读。
     */
    @Transactional
    public ConversationMessagesResponse getConversationMessages(String currentUsername, Long targetUserId, Integer page, Integer size) {
        User currentUser = requireUser(currentUsername);
        User targetUser = requireEnabledUser(targetUserId);
        ensureChatAllowed(currentUser.getId(), targetUser.getId());

        int resolvedPage = page == null || page < 1 ? 1 : page;
        int resolvedSize = size == null
                ? DEFAULT_MESSAGE_PAGE_SIZE
                : Math.max(1, Math.min(size, MAX_MESSAGE_PAGE_SIZE));

        // 用户打开会话页就视为已读，因此这里先执行已读回执。
        markConversationReadInternal(currentUser, targetUser);

        long total = chatMessageMapper.countConversationMessages(currentUser.getId(), targetUser.getId());
        long offset = (long) (resolvedPage - 1) * resolvedSize;
        List<ChatMessage> messages = chatMessageMapper.findConversationMessagesPage(
                currentUser.getId(),
                targetUser.getId(),
                offset,
                resolvedSize
        );
        messages.sort(Comparator
                .comparing(ChatMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ChatMessage::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        List<ChatMessageResponse> records = messages.stream()
                .map(message -> toChatMessageResponse(message, currentUser.getId(), currentUser, targetUser))
                .toList();
        boolean hasMore = offset + messages.size() < total;

        return new ConversationMessagesResponse(records, total, resolvedPage, resolvedSize, hasMore);
    }

    /**
     * 主动标记某个会话为已读。
     */
    @Transactional
    public MessageReadReceiptResponse markConversationRead(String currentUsername, Long targetUserId) {
        User currentUser = requireUser(currentUsername);
        User targetUser = requireEnabledUser(targetUserId);
        ensureChatAllowed(currentUser.getId(), targetUser.getId());

        MessageReadReceiptResponse receipt = markConversationReadInternal(currentUser, targetUser);
        if (receipt != null) {
            return receipt;
        }
        return new MessageReadReceiptResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getId(),
                List.of(),
                null
        );
    }

    /**
     * 发送私聊消息。
     * 支持文本、表情、图片、贴纸等消息类型，发送成功后会推送给接收方。
     */
    @Transactional
    public ChatMessageResponse sendMessage(String currentUsername, ChatMessageRequest request) {
        User currentUser = requireUser(currentUsername);
        User receiver = requireEnabledUser(request.getReceiverId());

        if (Objects.equals(currentUser.getId(), receiver.getId())) {
            throw new RuntimeException("Cannot send a private message to yourself");
        }

        // 只有好友或仍有待处理好友申请的双方才允许私聊，避免开放式骚扰。
        ensureChatAllowed(currentUser.getId(), receiver.getId());
        markConversationReadInternal(currentUser, receiver);

        ChatMessageType messageType = resolveMessageType(request.getType());
        String content = normalizeNullable(request.getContent());
        String mediaUrl = normalizeNullable(request.getMediaUrl());
        // 不同消息类型对 content / mediaUrl 的要求不同，这里统一做兜底校验。
        validateMessagePayload(messageType, content, mediaUrl);

        if (messageType == ChatMessageType.STICKER && !StringUtils.hasText(content)) {
            content = "Sticker";
        }

        ChatMessage message = new ChatMessage();
        message.setSenderId(currentUser.getId());
        message.setReceiverId(receiver.getId());
        message.setType(messageType);
        message.setContent(content);
        message.setMediaUrl(mediaUrl);
        message.setRead(false);
        chatMessageMapper.insert(message);

        ChatMessageResponse response = toChatMessageResponse(message, currentUser.getId(), currentUser, receiver);
        ChatMessageResponse receiverResponse = toChatMessageResponse(message, receiver.getId(), receiver, currentUser);
        // 发送方拿到自己的响应，接收方则通过实时事件收到另一份“面向接收者视角”的响应。
        socialEventPublisher.publish(new SocialWsEvent(
                SocialEventType.CHAT_MESSAGE,
                receiver.getUsername(),
                null,
                receiverResponse,
                currentUser.getId(),
                "New private message",
                null
        ));

        return response;
    }

    private User requireUser(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

    private User requireEnabledUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !user.isEnabled()) {
            throw new RuntimeException("Target user not found or disabled");
        }
        return user;
    }

    private FriendRequest requireFriendRequest(Long requestId) {
        FriendRequest friendRequest = friendRequestMapper.selectById(requestId);
        if (friendRequest == null) {
            throw new RuntimeException("Friend request not found");
        }
        return friendRequest;
    }

    private void ensureFriendship(Long userId, Long friendId) {
        if (friendshipMapper.existsFriendship(userId, friendId)) {
            return;
        }

        Friendship friendship = new Friendship();
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendshipMapper.insert(friendship);
    }

    private void ensureChatAllowed(Long userId, Long targetUserId) {
        if (friendshipMapper.existsFriendship(userId, targetUserId)) {
            return;
        }
        if (friendRequestMapper.findPendingBetweenUsers(userId, targetUserId) != null) {
            // 待处理好友申请期间允许先建立沟通，便于双方确认身份。
            return;
        }
        throw new RuntimeException("Private chat is only available for friends or pending friend requests");
    }

    private String resolveRelationStatus(Long currentUserId, Long targetUserId, FriendRequest pendingRequest) {
        if (friendshipMapper.existsFriendship(currentUserId, targetUserId)) {
            return RELATION_FRIEND;
        }
        if (pendingRequest == null) {
            return RELATION_NONE;
        }
        return Objects.equals(pendingRequest.getSenderId(), currentUserId)
                ? RELATION_REQUEST_SENT
                : RELATION_REQUEST_RECEIVED;
    }

    private FriendRequestResponse toFriendRequestResponse(FriendRequest request) {
        User sender = requireEnabledUser(request.getSenderId());
        User receiver = requireEnabledUser(request.getReceiverId());
        return toFriendRequestResponse(request, sender, receiver);
    }

    private FriendRequestResponse toFriendRequestResponse(FriendRequest request, User sender, User receiver) {
        return new FriendRequestResponse(
                request.getId(),
                sender.getId(),
                sender.getUsername(),
                sender.getAvatar(),
                receiver.getId(),
                receiver.getUsername(),
                receiver.getAvatar(),
                request.getRemark(),
                request.getStatus(),
                true,
                request.getCreatedAt()
        );
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage message, Long currentUserId, User currentUser, User targetUser) {
        boolean mine = Objects.equals(message.getSenderId(), currentUserId);
        User sender = mine ? currentUser : targetUser;
        User receiver = mine ? targetUser : currentUser;
        boolean read = isMessageRead(message);

        // 同一条消息会按“当前查看者视角”转换，mine 字段决定前端左右气泡布局。
        return new ChatMessageResponse(
                message.getId(),
                sender.getId(),
                sender.getUsername(),
                sender.getAvatar(),
                receiver.getId(),
                receiver.getUsername(),
                receiver.getAvatar(),
                message.getType(),
                message.getContent(),
                message.getMediaUrl(),
                read,
                message.getReadAt(),
                mine,
                message.getCreatedAt()
        );
    }

    private boolean isMessageRead(ChatMessage message) {
        return message != null
                && (Boolean.TRUE.equals(message.getRead()) || message.getReadAt() != null);
    }

    private String buildRequestRemark(String username, String remark) {
        String normalizedRemark = normalizeNullable(remark);
        if (StringUtils.hasText(normalizedRemark)) {
            return normalizedRemark;
        }
        // 没有填写备注时给一个友好的默认文案，减少空白申请体验。
        return "Hi, I am " + username + ". I'd like to add you as a friend.";
    }

    private ChatMessageType resolveMessageType(String type) {
        if (!StringUtils.hasText(type)) {
            return ChatMessageType.TEXT;
        }
        try {
            return ChatMessageType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Unsupported message type");
        }
    }

    private void validateMessagePayload(ChatMessageType type, String content, String mediaUrl) {
        switch (type) {
            case TEXT, EMOJI -> {
                if (!StringUtils.hasText(content)) {
                    throw new RuntimeException("Text or emoji content cannot be empty");
                }
            }
            case IMAGE, STICKER -> {
                if (!StringUtils.hasText(mediaUrl)) {
                    throw new RuntimeException("Image or sticker message is missing mediaUrl");
                }
            }
            default -> throw new RuntimeException("Unsupported message type");
        }
    }

    private String buildMessagePreview(ChatMessage message) {
        if (message == null) {
            return null;
        }
        // 联系人列表只展示摘要，因此不同消息类型需要做可读性转换。
        return switch (message.getType()) {
            case IMAGE -> StringUtils.hasText(message.getContent()) ? "[Image] " + message.getContent() : "[Image]";
            case STICKER -> StringUtils.hasText(message.getContent()) ? "[Sticker] " + message.getContent() : "[Sticker]";
            case EMOJI -> message.getContent();
            case TEXT -> message.getContent();
        };
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private MessageReadReceiptResponse markConversationReadInternal(User currentUser, User targetUser) {
        List<Long> unreadMessageIds = chatMessageMapper.findUnreadMessageIds(targetUser.getId(), currentUser.getId());
        if (unreadMessageIds.isEmpty()) {
            return null;
        }

        LocalDateTime readAt = LocalDateTime.now();
        chatMessageMapper.markConversationRead(targetUser.getId(), currentUser.getId(), readAt);

        // 已读回执会通知消息发送方，便于前端把消息状态更新为“已读”。
        MessageReadReceiptResponse receipt = new MessageReadReceiptResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getId(),
                unreadMessageIds,
                readAt
        );
        socialEventPublisher.publish(new SocialWsEvent(
                SocialEventType.MESSAGE_READ,
                targetUser.getUsername(),
                null,
                null,
                currentUser.getId(),
                currentUser.getUsername() + " read your messages",
                receipt
        ));

        return receipt;
    }
}
