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

    public List<FriendRequestResponse> listPendingReceivedRequests(String currentUsername) {
        User currentUser = requireUser(currentUsername);
        return friendRequestMapper.findPendingReceived(currentUser.getId()).stream()
                .map(this::toFriendRequestResponse)
                .toList();
    }

    public List<FriendRequestResponse> listPendingSentRequests(String currentUsername) {
        User currentUser = requireUser(currentUsername);
        return friendRequestMapper.findPendingSent(currentUser.getId()).stream()
                .map(this::toFriendRequestResponse)
                .toList();
    }

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

    public List<SocialContactResponse> listContacts(String currentUsername) {
        User currentUser = requireUser(currentUsername);
        Map<Long, SocialContactResponse> contacts = new LinkedHashMap<>();
        Map<Long, LocalDateTime> activityTimes = new LinkedHashMap<>();

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

    @Transactional
    public ConversationMessagesResponse getConversationMessages(String currentUsername, Long targetUserId, Integer page, Integer size) {
        User currentUser = requireUser(currentUsername);
        User targetUser = requireEnabledUser(targetUserId);
        ensureChatAllowed(currentUser.getId(), targetUser.getId());

        int resolvedPage = page == null || page < 1 ? 1 : page;
        int resolvedSize = size == null
                ? DEFAULT_MESSAGE_PAGE_SIZE
                : Math.max(1, Math.min(size, MAX_MESSAGE_PAGE_SIZE));

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

    @Transactional
    public ChatMessageResponse sendMessage(String currentUsername, ChatMessageRequest request) {
        User currentUser = requireUser(currentUsername);
        User receiver = requireEnabledUser(request.getReceiverId());

        if (Objects.equals(currentUser.getId(), receiver.getId())) {
            throw new RuntimeException("Cannot send a private message to yourself");
        }

        ensureChatAllowed(currentUser.getId(), receiver.getId());

        ChatMessageType messageType = resolveMessageType(request.getType());
        String content = normalizeNullable(request.getContent());
        String mediaUrl = normalizeNullable(request.getMediaUrl());
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
        socialEventPublisher.publish(new SocialWsEvent(
                SocialEventType.CHAT_MESSAGE,
                receiver.getUsername(),
                null,
                response,
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
                Boolean.TRUE.equals(message.getRead()),
                message.getReadAt(),
                mine,
                message.getCreatedAt()
        );
    }

    private String buildRequestRemark(String username, String remark) {
        String normalizedRemark = normalizeNullable(remark);
        if (StringUtils.hasText(normalizedRemark)) {
            return normalizedRemark;
        }
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
