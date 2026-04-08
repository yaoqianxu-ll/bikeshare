// 包声明，指定该类所在的包路径
package com.example.bickdemo.service;

// 导入聊天消息请求DTO
import com.example.bickdemo.dto.ChatMessageRequest;
// 导入聊天消息响应DTO
import com.example.bickdemo.dto.ChatMessageResponse;
// 导入会话消息响应DTO
import com.example.bickdemo.dto.ConversationMessagesResponse;
// 导入好友请求创建请求DTO
import com.example.bickdemo.dto.FriendRequestCreateRequest;
// 导入好友请求响应DTO
import com.example.bickdemo.dto.FriendRequestResponse;
// 导入消息已读回执响应DTO
import com.example.bickdemo.dto.MessageReadReceiptResponse;
// 导入社交联系人响应DTO
import com.example.bickdemo.dto.SocialContactResponse;
// 导入社交事件类型枚举
import com.example.bickdemo.dto.SocialEventType;
// 导入社交WebSocket事件DTO
import com.example.bickdemo.dto.SocialWsEvent;
// 导入用户资料响应DTO
import com.example.bickdemo.dto.UserProfileResponse;
// 导入用户搜索响应DTO
import com.example.bickdemo.dto.UserSearchResponse;
// 导入聊天消息实体
import com.example.bickdemo.entity.ChatMessage;
// 导入聊天消息类型枚举
import com.example.bickdemo.entity.ChatMessageType;
// 导入好友请求实体
import com.example.bickdemo.entity.FriendRequest;
// 导入好友请求状态枚举
import com.example.bickdemo.entity.FriendRequestStatus;
// 导入好友关系实体
import com.example.bickdemo.entity.Friendship;
// 导入用户实体
import com.example.bickdemo.entity.User;
// 导入聊天消息Mapper
import com.example.bickdemo.mapper.ChatMessageMapper;
// 导入好友请求Mapper
import com.example.bickdemo.mapper.FriendRequestMapper;
// 导入好友关系Mapper
import com.example.bickdemo.mapper.FriendshipMapper;
// 导入用户Mapper
import com.example.bickdemo.mapper.UserMapper;
// 导入Lombok的@RequiredArgsConstructor，用于生成构造函数
import lombok.RequiredArgsConstructor;
// 导入Lombok的@Slf4j，用于生成日志对象
import lombok.extern.slf4j.Slf4j;
// 导入Spring的WebSocket消息发送模板
import org.springframework.messaging.simp.SimpMessagingTemplate;
// 导入Spring的@Service注解，标识该类为服务层组件
import org.springframework.stereotype.Service;
// 导入Spring的事务管理注解
import org.springframework.transaction.annotation.Transactional;
// 导入Spring的工具类，用于字符串处理
import org.springframework.util.StringUtils;

// 导入Java时间类
import java.time.LocalDateTime;
// 导入Java数组列表类
import java.util.ArrayList;
// 导入Java比较器类
import java.util.Comparator;
// 导入Java链表哈希映射类
import java.util.LinkedHashMap;
// 导入Java列表接口
import java.util.List;
// 导入Java映射接口
import java.util.Map;
// 导入Java对象比较工具类
import java.util.Objects;

// Lombok生成的日志对象注解
@Slf4j
// Spring服务层注解
@Service
// Lombok生成的构造函数注解，用于注入final字段
@RequiredArgsConstructor
/**
 * 社交服务。
 * 负责用户搜索、好友申请、联系人列表、私聊消息分页、已读回执和实时事件发布等能力。
 * 该服务既维护数据库中的好友/消息状态，也负责向 WebSocket 推送实时通知。
 */
// 社交服务类定义
public class SocialService {

    // 搜索结果数量限制常量，设置为20
    private static final int SEARCH_LIMIT = 20;
    // 默认消息分页大小常量，设置为24
    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 24;
    // 最大消息分页大小常量，设置为60
    private static final int MAX_MESSAGE_PAGE_SIZE = 60;
    // 关系状态常量：无关系
    private static final String RELATION_NONE = "NONE";
    // 关系状态常量：好友关系
    private static final String RELATION_FRIEND = "FRIEND";
    // 关系状态常量：已发送好友请求
    private static final String RELATION_REQUEST_SENT = "REQUEST_SENT";
    // 关系状态常量：已收到好友请求
    private static final String RELATION_REQUEST_RECEIVED = "REQUEST_RECEIVED";
    // 消息撤回时间窗口，单位：分钟
    private static final int RECALL_WINDOW_MINUTES = 2;

    // 用户Mapper，用于操作用户数据
    private final UserMapper userMapper;
    // 好友请求Mapper，用于操作好友请求数据
    private final FriendRequestMapper friendRequestMapper;
    // 好友关系Mapper，用于操作好友关系数据
    private final FriendshipMapper friendshipMapper;
    // 聊天消息Mapper，用于操作聊天消息数据
    private final ChatMessageMapper chatMessageMapper;
    // 社交事件发布器，用于发布社交相关事件到消息队列
    private final SocialEventPublisher socialEventPublisher;
    // WebSocket消息发送模板，用于直接发送WebSocket消息
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 搜索用户。
     * 搜索结果里会附带与当前用户之间的关系状态，方便前端直接决定展示"加好友/已发送"等按钮。
     */
    // 搜索用户方法，根据关键词和当前用户名搜索用户
    public List<UserSearchResponse> searchUsers(String keyword, String currentUsername) {
        // 对关键词进行空值检查和去除首尾空格处理
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        // 如果处理后的关键词为空，则返回空列表
        if (!StringUtils.hasText(trimmedKeyword)) {
            return List.of();
        }

        // 获取当前用户信息
        User currentUser = requireUser(currentUsername);
        // 根据用户名模糊搜索用户，排除当前用户，限制返回数量
        List<User> users = userMapper.searchByUsernameLike(trimmedKeyword, currentUser.getId(), SEARCH_LIMIT);

        // 将搜索结果转换为用户搜索响应列表
        return users.stream()
                .map(user -> {
                    // 查询当前用户与搜索结果用户之间是否存在待处理的好友请求
                    FriendRequest pendingRequest = friendRequestMapper.findPendingBetweenUsers(currentUser.getId(), user.getId());
                    // 创建用户搜索响应对象，包含用户信息和关系状态
                    return new UserSearchResponse(
                            user.getId(), // 用户ID
                            user.getUsername(), // 用户名
                            user.getAvatar(), // 用户头像
                            user.getBio(), // 用户简介
                            resolveRelationStatus(currentUser.getId(), user.getId(), pendingRequest), // 关系状态
                            pendingRequest == null ? null : pendingRequest.getId() // 待处理请求ID
                    );
                })
                .toList(); // 转换为列表返回
    }

    /**
     * 获取指定用户的详细信息（用于聊天场景）。
     */
    // 获取用户资料方法
    public UserProfileResponse getUserProfile(String currentUsername, Long targetUserId) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 获取目标用户，并确保目标用户存在且已启用
        User targetUser = requireEnabledUser(targetUserId);

        // 查询当前用户与目标用户之间是否存在待处理的好友请求
        FriendRequest pendingRequest = friendRequestMapper.findPendingBetweenUsers(currentUser.getId(), targetUser.getId());
        // 解析关系状态
        String relationStatus = resolveRelationStatus(currentUser.getId(), targetUser.getId(), pendingRequest);

        // 创建并返回用户资料响应对象
        return new UserProfileResponse(
                targetUser.getId(), // 目标用户ID
                targetUser.getUsername(), // 目标用户名
                targetUser.getEmail(), // 目标用户邮箱
                targetUser.getAvatar(), // 目标用户头像
                targetUser.getBio(), // 目标用户简介
                targetUser.getRole() != null ? targetUser.getRole().name() : "USER", // 目标用户角色
                targetUser.isEnabled(), // 目标用户是否启用
                targetUser.getCreatedAt(), // 目标用户创建时间
                targetUser.getUpdatedAt(), // 目标用户更新时间
                relationStatus, // 关系状态
                pendingRequest == null ? null : pendingRequest.getId(), // 待处理请求ID
                pendingRequest == null ? null : (Objects.equals(pendingRequest.getSenderId(), currentUser.getId()) ? "OUTGOING" : "INCOMING"), // 请求方向
                true // 是否有权限查看
        );
    }

    /**
     * 发起好友申请。
     * 会拦截自己加自己、已是好友、已有待处理申请等重复场景。
     */
    // 事务注解，确保该方法在事务中执行
    @Transactional
    // 创建好友请求方法
    public FriendRequestResponse createFriendRequest(String currentUsername, FriendRequestCreateRequest request) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 获取接收者用户，并确保该用户存在且已启用
        User receiver = requireEnabledUser(request.getReceiverId());

        // 检查不能向自己发送好友请求
        if (Objects.equals(currentUser.getId(), receiver.getId())) {
            throw new RuntimeException("Cannot send a friend request to yourself");
        }

        // 检查是否已经是好友关系
        if (friendshipMapper.existsFriendship(currentUser.getId(), receiver.getId())) {
            throw new RuntimeException("You are already friends");
        }

        // 检查是否存在待处理的好友请求
        FriendRequest pendingRequest = friendRequestMapper.findPendingBetweenUsers(currentUser.getId(), receiver.getId());
        if (pendingRequest != null) {
            // 如果待处理请求的发送者是当前用户，说明已经发送过请求
            if (Objects.equals(pendingRequest.getSenderId(), currentUser.getId())) {
                throw new RuntimeException("Friend request already sent");
            }
            // 否则说明对方已经向当前用户发送了请求
            throw new RuntimeException("The other user has already sent you a friend request");
        }

        // 创建新的好友请求对象
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSenderId(currentUser.getId()); // 设置发送者ID为当前用户ID
        friendRequest.setReceiverId(receiver.getId()); // 设置接收者ID
        friendRequest.setRemark(buildRequestRemark(currentUser.getUsername(), request.getRemark())); // 设置好友请求备注
        friendRequest.setStatus(FriendRequestStatus.PENDING); // 设置状态为待处理
        friendRequestMapper.insert(friendRequest); // 插入数据库

        // 将好友请求转换为响应对象
        FriendRequestResponse response = toFriendRequestResponse(friendRequest, currentUser, receiver);
        // 好友申请创建后，立即通过消息队列通知接收方刷新社交面板。
        // 创建WebSocket事件对象
        SocialWsEvent event = new SocialWsEvent() {{
            setEventType(SocialEventType.FRIEND_REQUEST_CREATED); // 设置事件类型为好友请求已创建
            setRecipientUsername(receiver.getUsername()); // 设置接收者用户名
            setFriendRequest(response); // 设置好友请求响应
            setContactUserId(currentUser.getId()); // 设置联系人用户ID
            setNotice("New friend request"); // 设置通知消息
        }};
        // 打印日志，记录好友请求事件发布
        log.info("[RabbitMQ] 发布好友申请事件: sender={}, receiver={}", currentUser.getUsername(), receiver.getUsername());
        // 通过社交事件发布器发布事件
        socialEventPublisher.publish(event);

        // 返回好友请求响应
        return response;
    }

    /**
     * 查看收到的待处理好友申请。
     */
    // 获取收到的待处理好友请求列表
    public List<FriendRequestResponse> listPendingReceivedRequests(String currentUsername) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 查询收到的好友请求并转换为响应列表
        return friendRequestMapper.findPendingReceived(currentUser.getId()).stream()
                .map(this::toFriendRequestResponse) // 使用方法引用转换每个请求
                .toList(); // 转换为列表返回
    }

    /**
     * 查看自己发出的待处理好友申请。
     */
    // 获取发出的待处理好友请求列表
    public List<FriendRequestResponse> listPendingSentRequests(String currentUsername) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 查询发出的好友请求并转换为响应列表
        return friendRequestMapper.findPendingSent(currentUser.getId()).stream()
                .map(this::toFriendRequestResponse) // 使用方法引用转换每个请求
                .toList(); // 转换为列表返回
    }

    /**
     * 接受好友申请。
     * 接受后会补齐双向 friendship 关系，确保双方联系人列表都能查到彼此。
     */
    // 事务注解，确保该方法在事务中执行
    @Transactional
    // 接受好友请求方法
    public FriendRequestResponse acceptFriendRequest(String currentUsername, Long requestId) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 获取好友请求，并确保该请求存在
        FriendRequest friendRequest = requireFriendRequest(requestId);

        // 检查当前用户是否是该请求的接收者
        if (!Objects.equals(friendRequest.getReceiverId(), currentUser.getId())) {
            throw new RuntimeException("You can only handle requests sent to yourself");
        }
        // 检查请求是否已经被处理
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Friend request already handled");
        }

        // 获取发送者和接收者用户，并确保用户存在且已启用
        User sender = requireEnabledUser(friendRequest.getSenderId());
        User receiver = requireEnabledUser(friendRequest.getReceiverId());

        // 将好友请求状态更新为已接受
        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestMapper.updateById(friendRequest);

        // 好友关系采用双向各存一条记录，方便按 user_id 单边查询联系人。
        // 创建双向好友关系
        ensureFriendship(sender.getId(), receiver.getId()); // 创建发送者到接收者的好友关系
        ensureFriendship(receiver.getId(), sender.getId()); // 创建接收者到发送者的好友关系

        // 将好友请求转换为响应对象
        FriendRequestResponse response = toFriendRequestResponse(friendRequest, sender, receiver);
        // 创建WebSocket事件对象
        SocialWsEvent event = new SocialWsEvent() {{
            setEventType(SocialEventType.FRIEND_REQUEST_ACCEPTED); // 设置事件类型为好友请求已被接受
            setRecipientUsername(sender.getUsername()); // 设置接收者用户名（这里是发送者，因为需要通知他）
            setFriendRequest(response); // 设置好友请求响应
            setContactUserId(receiver.getId()); // 设置联系人用户ID
            setNotice(receiver.getUsername() + " accepted your friend request"); // 设置通知消息
        }};
        // 打印日志，记录好友请求接受事件发布
        log.info("[RabbitMQ] 发布好友申请通过事件: sender={}, receiver={}", sender.getUsername(), receiver.getUsername());
        // 通过社交事件发布器发布事件
        socialEventPublisher.publish(event);

        // 返回好友请求响应
        return response;
    }

    /**
     * 拒绝好友申请。
     */
    // 事务注解，确保该方法在事务中执行
    @Transactional
    // 拒绝好友请求方法
    public FriendRequestResponse rejectFriendRequest(String currentUsername, Long requestId) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 获取好友请求，并确保该请求存在
        FriendRequest friendRequest = requireFriendRequest(requestId);

        // 检查当前用户是否是该请求的接收者
        if (!Objects.equals(friendRequest.getReceiverId(), currentUser.getId())) {
            throw new RuntimeException("You can only handle requests sent to yourself");
        }
        // 检查请求是否已经被处理
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Friend request already handled");
        }

        // 获取发送者和接收者用户，并确保用户存在且已启用
        User sender = requireEnabledUser(friendRequest.getSenderId());
        User receiver = requireEnabledUser(friendRequest.getReceiverId());

        // 将好友请求状态更新为已拒绝
        friendRequest.setStatus(FriendRequestStatus.REJECTED);
        friendRequestMapper.updateById(friendRequest);

        // 将好友请求转换为响应对象
        FriendRequestResponse response = toFriendRequestResponse(friendRequest, sender, receiver);
        // 创建WebSocket事件对象
        SocialWsEvent event = new SocialWsEvent() {{
            setEventType(SocialEventType.FRIEND_REQUEST_REJECTED); // 设置事件类型为好友请求已被拒绝
            setRecipientUsername(sender.getUsername()); // 设置接收者用户名（这里是发送者，因为需要通知他）
            setFriendRequest(response); // 设置好友请求响应
            setContactUserId(receiver.getId()); // 设置联系人用户ID
            setNotice(receiver.getUsername() + " rejected your friend request"); // 设置通知消息
        }};
        // 打印日志，记录好友请求拒绝事件发布
        log.info("[RabbitMQ] 发布好友申请拒绝事件: sender={}, receiver={}", sender.getUsername(), receiver.getUsername());
        // 通过社交事件发布器发布事件
        socialEventPublisher.publish(event);

        // 返回好友请求响应
        return response;
    }

    /**
     * 组装联系人列表。
     * 联系人列表不仅包含好友，也包含待处理的好友申请和最近消息预览。
     */
    // 获取联系人列表方法
    public List<SocialContactResponse> listContacts(String currentUsername) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 创建联系人Map，使用LinkedHashMap保持插入顺序
        Map<Long, SocialContactResponse> contacts = new LinkedHashMap<>();
        // 创建活动时间Map，用于记录每个联系人的最新活动时间
        Map<Long, LocalDateTime> activityTimes = new LinkedHashMap<>();

        // 第一层先把正式好友装入联系人列表。
        // 遍历当前用户的所有好友关系
        for (Friendship friendship : friendshipMapper.findByUserId(currentUser.getId())) {
            // 根据好友ID获取好友用户信息
            User friend = userMapper.selectById(friendship.getFriendId());
            // 如果好友不存在或已被禁用，则跳过
            if (friend == null || !friend.isEnabled()) {
                continue;
            }

            // 创建联系人响应对象
            SocialContactResponse contact = new SocialContactResponse(
                    friend.getId(), // 好友ID
                    friend.getUsername(), // 好友用户名
                    friend.getEmail(), // 好友邮箱
                    friend.getAvatar(), // 好友头像
                    friend.getBio(), // 好友简介
                    friend.getRole() != null ? friend.getRole().name() : "USER", // 好友角色
                    friend.isEnabled(), // 好友是否启用
                    RELATION_FRIEND, // 关系状态为好友
                    null, // 待处理请求ID（好友没有）
                    null, // 待处理请求方向（好友没有）
                    null, // 最后消息预览
                    null, // 最后消息时间
                    friendship.getCreatedAt(), // 好友关系创建时间
                    friend.getCreatedAt(), // 好友用户创建时间
                    friend.getUpdatedAt(), // 好友用户更新时间
                    0, // 未读消息数
                    true, // 是否有权限
                    friendship.getCreatedAt() // 活动时间
            );
            // 将联系人添加到Map中
            contacts.put(friend.getId(), contact);
            // 记录好友的活动时间
            activityTimes.put(friend.getId(), friendship.getCreatedAt());
        }

        // 第二层补充"收到的好友申请"，这样前端的联系人侧栏能直接提醒处理。
        // 遍历当前用户收到的待处理好友请求
        for (FriendRequest request : friendRequestMapper.findPendingReceived(currentUser.getId())) {
            // 获取请求发送者用户信息
            User peer = userMapper.selectById(request.getSenderId());
            // 如果用户不存在或已被禁用，则跳过
            if (peer == null || !peer.isEnabled()) {
                continue;
            }

            // 使用computeIfAbsent方法，如果该用户不在联系人列表中，则创建新的联系人
            contacts.computeIfAbsent(peer.getId(), key -> buildContactFromUser(peer, RELATION_REQUEST_RECEIVED, request.getId(), "INCOMING"));
            // 如果该用户的活动时间尚未设置，则设置为请求创建时间
            activityTimes.putIfAbsent(peer.getId(), request.getCreatedAt());
        }

        // 第三层补充"已发出的好友申请"，方便用户看到请求仍在等待中。
        // 遍历当前用户发出的待处理好友请求
        for (FriendRequest request : friendRequestMapper.findPendingSent(currentUser.getId())) {
            // 获取请求接收者用户信息
            User peer = userMapper.selectById(request.getReceiverId());
            // 如果用户不存在或已被禁用，则跳过
            if (peer == null || !peer.isEnabled()) {
                continue;
            }

            // 使用computeIfAbsent方法，如果该用户不在联系人列表中，则创建新的联系人
            contacts.computeIfAbsent(peer.getId(), key -> buildContactFromUser(peer, RELATION_REQUEST_SENT, request.getId(), "OUTGOING"));
            // 如果该用户的活动时间尚未设置，则设置为请求创建时间
            activityTimes.putIfAbsent(peer.getId(), request.getCreatedAt());
        }

        // 遍历所有联系人，补充最近消息预览和未读消息数
        for (SocialContactResponse contact : contacts.values()) {
            // 再补齐最近一条消息、未读数和活动时间，用于联系人排序与摘要展示。
            // 查询与该联系人的最新消息
            ChatMessage latestMessage = chatMessageMapper.findLatestBetweenUsers(currentUser.getId(), contact.getUserId());
            if (latestMessage != null) {
                // 设置最后消息预览
                contact.setLastMessagePreview(buildMessagePreview(latestMessage));
                // 设置最后消息时间
                contact.setLastMessageTime(latestMessage.getCreatedAt());
                // 设置活动时间
                contact.setActivityTime(latestMessage.getCreatedAt());
            } else {
                // 如果没有消息，则使用之前记录的活动时间
                contact.setActivityTime(activityTimes.get(contact.getUserId()));
            }
            // 设置未读消息数
            contact.setUnreadCount(chatMessageMapper.countUnreadFromUser(contact.getUserId(), currentUser.getId()));
        }

        // 将联系人Map转换为ArrayList
        List<SocialContactResponse> result = new ArrayList<>(contacts.values());
        // 根据活动时间进行降序排序，空值放在最后
        result.sort(Comparator.comparing(
                SocialContactResponse::getActivityTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        // 返回排序后的联系人列表
        return result;
    }

    /**
     * 分页获取会话消息。
     * 拉取会话时会顺便把对方发来的未读消息标记为已读。
     */
    // 事务注解，确保该方法在事务中执行
    @Transactional
    // 获取会话消息方法，支持分页
    public ConversationMessagesResponse getConversationMessages(String currentUsername, Long targetUserId, Integer page, Integer size) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 获取目标用户，并确保该用户存在且已启用
        User targetUser = requireEnabledUser(targetUserId);
        // 确保当前用户与目标用户之间允许聊天
        ensureChatAllowed(currentUser.getId(), targetUser.getId());

        // 解析页码，如果为空或小于1则默认为1
        int resolvedPage = page == null || page < 1 ? 1 : page;
        // 解析每页大小，如果为空则使用默认值，最小为1，最大为60
        int resolvedSize = size == null
                ? DEFAULT_MESSAGE_PAGE_SIZE
                : Math.max(1, Math.min(size, MAX_MESSAGE_PAGE_SIZE));

        // 用户打开会话页就视为已读，因此这里先执行已读回执。
        // 调用内部方法标记会话为已读
        markConversationReadInternal(currentUser, targetUser);

        // 统计会话消息总数
        long total = chatMessageMapper.countConversationMessages(currentUser.getId(), targetUser.getId());
        // 计算分页偏移量
        long offset = (long) (resolvedPage - 1) * resolvedSize;
        // 分页查询会话消息
        List<ChatMessage> messages = chatMessageMapper.findConversationMessagesPage(
                currentUser.getId(), // 当前用户ID
                targetUser.getId(), // 目标用户ID
                offset, // 偏移量
                resolvedSize // 每页大小
        );
        // 对消息按创建时间和ID进行排序
        messages.sort(Comparator
                .comparing(ChatMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ChatMessage::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        // 将消息实体转换为消息响应DTO
        List<ChatMessageResponse> records = messages.stream()
                .map(message -> toChatMessageResponse(message, currentUser.getId(), currentUser, targetUser))
                .toList();
        // 计算是否还有更多消息
        boolean hasMore = offset + messages.size() < total;

        // 创建并返回会话消息响应对象
        return new ConversationMessagesResponse(records, total, resolvedPage, resolvedSize, hasMore);
    }

    /**
     * 主动标记某个会话为已读。
     */
    // 事务注解，确保该方法在事务中执行
    @Transactional
    // 标记会话已读方法
    public MessageReadReceiptResponse markConversationRead(String currentUsername, Long targetUserId) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 获取目标用户，并确保该用户存在且已启用
        User targetUser = requireEnabledUser(targetUserId);
        // 确保当前用户与目标用户之间允许聊天
        ensureChatAllowed(currentUser.getId(), targetUser.getId());

        // 调用内部方法标记会话为已读
        MessageReadReceiptResponse receipt = markConversationReadInternal(currentUser, targetUser);
        // 如果有未读消息被标记为已读，则返回回执
        if (receipt != null) {
            return receipt;
        }
        // 如果没有未读消息，则返回一个空的回执
        return new MessageReadReceiptResponse(
                currentUser.getId(), // 当前用户ID
                currentUser.getUsername(), // 当前用户名
                currentUser.getId(), // 操作者ID（这里是自己）
                List.of(), // 未读消息ID列表（为空）
                null // 已读时间
        );
    }

    /**
     * 发送私聊消息。
     * 支持文本、表情、图片、贴纸等消息类型，发送成功后会推送给接收方。
     */
    // 事务注解，确保该方法在事务中执行
    @Transactional
    // 发送消息方法
    public ChatMessageResponse sendMessage(String currentUsername, ChatMessageRequest request) {
        // 获取当前用户
        User currentUser = requireUser(currentUsername);
        // 获取接收者用户，并确保该用户存在且已启用
        User receiver = requireEnabledUser(request.getReceiverId());

        // 检查不能给自己发送私信
        if (Objects.equals(currentUser.getId(), receiver.getId())) {
            throw new RuntimeException("Cannot send a private message to yourself");
        }

        // 只有好友或仍有待处理好友申请的双方才允许私聊，避免开放式骚扰。
        // 确保双方之间允许聊天
        ensureChatAllowed(currentUser.getId(), receiver.getId());
        // 标记当前用户已阅读与接收者的会话
        markConversationReadInternal(currentUser, receiver);

        // 解析消息类型
        ChatMessageType messageType = resolveMessageType(request.getType());
        // 规范化消息内容
        String content = normalizeNullable(request.getContent());
        // 规范化媒体URL
        String mediaUrl = normalizeNullable(request.getMediaUrl());
        // 不同消息类型对 content / mediaUrl 的要求不同，这里统一做兜底校验。
        // 校验消息内容
        validateMessagePayload(messageType, content, mediaUrl);

        // 如果是贴纸类型且没有内容，则使用默认文本
        if (messageType == ChatMessageType.STICKER && !StringUtils.hasText(content)) {
            content = "Sticker";
        }

        // 创建新的聊天消息对象
        ChatMessage message = new ChatMessage();
        message.setSenderId(currentUser.getId()); // 设置发送者ID
        message.setReceiverId(receiver.getId()); // 设置接收者ID
        message.setType(messageType); // 设置消息类型
        message.setContent(content); // 设置消息内容
        message.setMediaUrl(mediaUrl); // 设置媒体URL
        message.setRead(false); // 设置消息为未读
        chatMessageMapper.insert(message); // 插入数据库

        // 创建发送者的消息响应
        ChatMessageResponse response = toChatMessageResponse(message, currentUser.getId(), currentUser, receiver);
        // 创建接收者的消息响应（视角转换）
        ChatMessageResponse receiverResponse = toChatMessageResponse(message, receiver.getId(), receiver, currentUser);

        // 发送方拿到自己的响应，接收方则通过实时事件收到另一份"面向接收者视角"的响应。
        // 创建WebSocket事件对象
        SocialWsEvent event = new SocialWsEvent() {{
            setEventType(SocialEventType.CHAT_MESSAGE); // 设置事件类型为聊天消息
            setRecipientUsername(receiver.getUsername()); // 设置接收者用户名
            setMessage(receiverResponse); // 设置消息响应
            setContactUserId(currentUser.getId()); // 设置联系人用户ID
            setNotice("New private message"); // 设置通知消息
        }};

        // 通过 RabbitMQ 发送实时消息通知
        socialEventPublisher.publish(event);

        // 同时直接发送 WebSocket（确保实时性）
        try {
            // 使用WebSocket模板发送消息给指定用户
            messagingTemplate.convertAndSendToUser(
                    receiver.getUsername(), // 接收者用户名
                    "/queue/social", // 目标队列
                    event // 事件对象
            );
        } catch (Exception ex) {
            // 如果直接发送WebSocket失败，记录警告日志
            log.warn("直接发送 WebSocket 失败: {}", ex.getMessage());
        }

        // 返回发送者的消息响应
        return response;
    }

    /**
     * 撤回消息。
     *
     * 业务规则：
     * 1. 仅消息发送者可以撤回自己的消息
     * 2. 消息发出后仅 2 分钟内可撤回
     * 3. 消息只能被撤回一次（已撤回消息不可再撤回）
     *
     * 撤回后：
     * - 消息在数据库中标记 recalled=1, recalled_at=当前时间
     * - 发送者在聊天界面看到"重新编辑"按钮（前端根据 recalled=true 判断）
     * - 接收者通过 WebSocket 收到 MESSAGE_RECALLED 事件，看到"消息已撤回"占位
     *
     * @param currentUsername 当前用户名
     * @param messageId       要撤回的消息ID
     * @return 包含 messageId 和 recalledAt 的 Map
     * @throws RuntimeException 当消息不存在、无权撤回、已撤回或超过2分钟窗口时抛出
     */
    @Transactional
    public Map<String, Object> recallMessage(String currentUsername, Long messageId) {
        // 1. 获取当前登录用户
        User currentUser = requireUser(currentUsername);

        // 2. 根据消息ID查询消息（绕过逻辑删除）
        ChatMessage message = chatMessageMapper.selectByIdForRecall(messageId);

        // 3. 校验：消息是否存在
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }

        // 4. 校验：必须是消息发送者本人才能撤回
        if (!Objects.equals(message.getSenderId(), currentUser.getId())) {
            throw new RuntimeException("只能撤回自己发送的消息");
        }

        // 5. 校验：消息是否已被撤回（不可重复撤回）
        if (Boolean.TRUE.equals(message.getRecalled())) {
            throw new RuntimeException("消息已撤回，不能重复撤回");
        }

        // 6. 校验：是否在2分钟撤回窗口内
        LocalDateTime createdAt = message.getCreatedAt();
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (createdAt.plusMinutes(RECALL_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("撤回时间已超过2分钟，无法撤回");
        }

        // 7. 执行撤回：更新数据库标记，同时保存原始内容用于支持"重新编辑"
        LocalDateTime recalledAt = LocalDateTime.now();
        chatMessageMapper.markRecalled(messageId, recalledAt, message.getContent());

        // 8. 通过 WebSocket 通知接收方（接收方将看到"消息已撤回"）
        User receiver = requireEnabledUser(message.getReceiverId());
        // 构建仅包含ID和撤回标记的消息响应对象
        ChatMessageResponse recalledMsg = new ChatMessageResponse();
        recalledMsg.setId(message.getId());
        recalledMsg.setRecalled(true);
        recalledMsg.setRecalledAt(recalledAt);
        SocialWsEvent event = new SocialWsEvent() {{
            setEventType(SocialEventType.MESSAGE_RECALLED);
            setRecipientUsername(receiver.getUsername());
            // 推送时带上消息ID，接收方前端据此更新对应消息的UI状态
            setMessage(recalledMsg);
            setContactUserId(currentUser.getId());
            setNotice("对方撤回了一条消息");
        }};
        socialEventPublisher.publish(event);
        // 同时直接发送 WebSocket（确保实时性）
        try {
            messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/social", event);
        } catch (Exception ex) {
            log.warn("撤回通知 WebSocket 发送失败: {}", ex.getMessage());
        }

        // 9. 返回结果
        return Map.of("messageId", messageId, "recalledAt", recalledAt);
    }

    /**
     * 重新编辑并发送消息（仅限已撤回消息）。
     *
     * 业务规则：
     * 1. 仅原消息发送者可以重新发送
     * 2. 消息必须处于已撤回状态（recalled=true）才能重新发送
     * 3. 重新发送后，消息内容、媒体URL、发送时间均会更新
     * 4. 消息类型（TEXT/IMAGE/EMOJI/STICKER）不可更改
     *
     * 重新发送后：
     * - 消息重置为正常状态（recalled=0, recalled_at=NULL），内容替换为新内容
     * - 消息在对话中位置保持不变（created_at 更新，但消息ID不变）
     * - 接收方通过 WebSocket 收到 MESSAGE_RESENT 事件，看到更新后的内容
     *
     * @param currentUsername 当前用户名
     * @param messageId       要重新发送的消息ID（必须是已撤回消息）
     * @param request         新的消息内容请求（content、mediaUrl、type）
     * @return 更新后的消息响应对象
     * @throws RuntimeException 当消息不存在、无权操作、非撤回状态或内容校验失败时抛出
     */
    @Transactional
    public ChatMessageResponse resendMessage(String currentUsername, Long messageId, ChatMessageRequest request) {
        // 1. 获取当前登录用户
        User currentUser = requireUser(currentUsername);

        // 2. 根据消息ID查询原消息（必须是已撤回的消息）
        ChatMessage original = chatMessageMapper.selectByIdForRecall(messageId);

        // 3. 校验：消息是否存在
        if (original == null) {
            throw new RuntimeException("消息不存在");
        }

        // 4. 校验：必须是消息发送者本人
        if (!Objects.equals(original.getSenderId(), currentUser.getId())) {
            throw new RuntimeException("只能重新发送自己撤回的消息");
        }

        // 5. 校验：消息必须处于已撤回状态
        if (!Boolean.TRUE.equals(original.getRecalled())) {
            throw new RuntimeException("只能重新发送已撤回的消息");
        }

        // 6. 解析并校验新的消息内容
        ChatMessageType messageType = resolveMessageType(request.getType());
        String content = normalizeNullable(request.getContent());
        String mediaUrl = normalizeNullable(request.getMediaUrl());
        validateMessagePayload(messageType, content, mediaUrl);

        // 7. 贴纸类型兜底：如果类型是贴纸但没有内容，使用默认文本
        if (messageType == ChatMessageType.STICKER && !StringUtils.hasText(content)) {
            content = "Sticker";
        }

        // 8. 执行更新：重置撤回状态，更新内容和时间
        chatMessageMapper.updateContentAndTime(messageId, content, mediaUrl);

        // 9. 重新查询更新后的消息（获取新的 created_at 等信息）
        ChatMessage updated = chatMessageMapper.selectByIdForRecall(messageId);
        User receiver = requireEnabledUser(original.getReceiverId());

        // 10. 构建响应对象（保留原消息ID，更新内容）
        ChatMessageResponse response = toChatMessageResponse(updated, currentUser.getId(), currentUser, receiver);

        // 11. 通过 WebSocket 通知接收方（接收方看到更新后的消息内容）
        SocialWsEvent event = new SocialWsEvent() {{
            setEventType(SocialEventType.MESSAGE_RESENT);
            setRecipientUsername(receiver.getUsername());
            setMessage(response);
            setContactUserId(currentUser.getId());
            setNotice("对方重新发送了一条消息");
        }};
        socialEventPublisher.publish(event);
        // 同时直接发送 WebSocket（确保实时性）
        try {
            messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/social", event);
        } catch (Exception ex) {
            log.warn("重新发送通知 WebSocket 失败: {}", ex.getMessage());
        }

        return response;
    }

    // 获取用户信息，如果不存在则抛出异常
    private User requireUser(String username) {
        // 根据用户名查询用户
        User user = userMapper.findByUsername(username);
        // 如果用户不存在，抛出异常
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        // 返回用户对象
        return user;
    }

    // 获取用户信息并确保用户已启用，如果不存在或未启用则抛出异常
    private User requireEnabledUser(Long userId) {
        // 根据用户ID查询用户
        User user = userMapper.selectById(userId);
        // 如果用户不存在或未启用，抛出异常
        if (user == null || !user.isEnabled()) {
            throw new RuntimeException("Target user not found or disabled");
        }
        // 返回用户对象
        return user;
    }

    // 获取好友请求信息，如果不存在则抛出异常
    private FriendRequest requireFriendRequest(Long requestId) {
        // 根据好友请求ID查询好友请求
        FriendRequest friendRequest = friendRequestMapper.selectById(requestId);
        // 如果好友请求不存在，抛出异常
        if (friendRequest == null) {
            throw new RuntimeException("Friend request not found");
        }
        // 返回好友请求对象
        return friendRequest;
    }

    // 确保两个用户之间存在好友关系，如果不存在则创建
    private void ensureFriendship(Long userId, Long friendId) {
        // 检查好友关系是否已存在
        if (friendshipMapper.existsFriendship(userId, friendId)) {
            return; // 如果已存在，直接返回
        }

        // 创建新的好友关系对象
        Friendship friendship = new Friendship();
        friendship.setUserId(userId); // 设置用户ID
        friendship.setFriendId(friendId); // 设置好友ID
        friendshipMapper.insert(friendship); // 插入数据库
    }

    // 确保两个用户之间允许聊天（好友关系或存在待处理的好友请求）
    private void ensureChatAllowed(Long userId, Long targetUserId) {
        // 如果两个用户是好友关系，允许聊天
        if (friendshipMapper.existsFriendship(userId, targetUserId)) {
            return;
        }
        // 如果存在待处理的好友请求，允许先建立沟通，便于双方确认身份。
        if (friendRequestMapper.findPendingBetweenUsers(userId, targetUserId) != null) {
            return;
        }
        // 允许任何人之间发起聊天，方便先沟通再决定是否加好友
        // 实际业务中可根据需要调整为仅允许特定条件下的陌生人聊天
    }

    // 解析两个用户之间的关系状态
    private String resolveRelationStatus(Long currentUserId, Long targetUserId, FriendRequest pendingRequest) {
        // 如果两个用户是好友关系，返回好友状态
        if (friendshipMapper.existsFriendship(currentUserId, targetUserId)) {
            return RELATION_FRIEND;
        }
        // 如果没有待处理的好友请求，返回无关系状态
        if (pendingRequest == null) {
            return RELATION_NONE;
        }
        // 如果有待处理的好友请求，根据发送者判断是已发送还是已接收
        return Objects.equals(pendingRequest.getSenderId(), currentUserId)
                ? RELATION_REQUEST_SENT
                : RELATION_REQUEST_RECEIVED;
    }

    // 将好友请求实体转换为好友请求响应DTO（重载方法，通过请求ID查询用户）
    private FriendRequestResponse toFriendRequestResponse(FriendRequest request) {
        // 获取发送者用户
        User sender = requireEnabledUser(request.getSenderId());
        // 获取接收者用户
        User receiver = requireEnabledUser(request.getReceiverId());
        // 调用重载方法进行转换
        return toFriendRequestResponse(request, sender, receiver);
    }

    // 将好友请求实体转换为好友请求响应DTO
    private FriendRequestResponse toFriendRequestResponse(FriendRequest request, User sender, User receiver) {
        // 创建并返回好友请求响应对象
        return new FriendRequestResponse(
                request.getId(), // 好友请求ID
                sender.getId(), // 发送者ID
                sender.getUsername(), // 发送者用户名
                sender.getAvatar(), // 发送者头像
                receiver.getId(), // 接收者ID
                receiver.getUsername(), // 接收者用户名
                receiver.getAvatar(), // 接收者头像
                request.getRemark(), // 好友请求备注
                request.getStatus(), // 好友请求状态
                true, // 是否有权限（默认为true）
                request.getCreatedAt() // 创建时间
        );
    }

    // 将聊天消息实体转换为聊天消息响应DTO
    private ChatMessageResponse toChatMessageResponse(ChatMessage message, Long currentUserId, User currentUser, User targetUser) {
        // 判断消息是否是当前用户发送的
        boolean mine = Objects.equals(message.getSenderId(), currentUserId);
        // 根据是否是自己发送的来确定发送者和接收者
        User sender = mine ? currentUser : targetUser;
        User receiver = mine ? targetUser : currentUser;
        // 判断消息是否已读
        boolean read = isMessageRead(message);

        // 同一条消息会按"当前查看者视角"转换，mine 字段决定前端左右气泡布局。
        // 创建并返回聊天消息响应对象
        ChatMessageResponse response = new ChatMessageResponse(
                message.getId(), // 消息ID
                sender.getId(), // 发送者ID
                sender.getUsername(), // 发送者用户名
                sender.getAvatar(), // 发送者头像
                receiver.getId(), // 接收者ID
                receiver.getUsername(), // 接收者用户名
                receiver.getAvatar(), // 接收者头像
                message.getType(), // 消息类型
                message.getContent(), // 消息内容
                message.getMediaUrl(), // 媒体URL
                read, // 是否已读
                message.getReadAt(), // 已读时间
                message.getRecalled(), // 消息是否已撤回
                message.getOriginalContent(), // 撤回前的原始内容
                message.getRecalledAt(), // 撤回时间
                mine, // 是否是自己发送的
                message.getCreatedAt() // 创建时间
        );
        return response;
    }

    // 判断消息是否已读
    private boolean isMessageRead(ChatMessage message) {
        // 如果消息为空，返回false；否则检查read字段或readAt字段
        return message != null
                && (Boolean.TRUE.equals(message.getRead()) || message.getReadAt() != null);
    }

    // 构建好友请求备注
    private String buildRequestRemark(String username, String remark) {
        // 规范化备注内容
        String normalizedRemark = normalizeNullable(remark);
        // 如果备注有内容，直接返回
        if (StringUtils.hasText(normalizedRemark)) {
            return normalizedRemark;
        }
        // 没有填写备注时给一个友好的默认文案，减少空白申请体验。
        // 返回默认的好友请求备注
        return "Hi, I am " + username + ". I'd like to add you as a friend.";
    }

    // 解析消息类型
    private ChatMessageType resolveMessageType(String type) {
        // 如果类型为空或空白，返回默认的文本类型
        if (!StringUtils.hasText(type)) {
            return ChatMessageType.TEXT;
        }
        try {
            // 尝试将类型字符串转换为枚举值
            return ChatMessageType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // 如果类型不支持，抛出异常
            throw new RuntimeException("Unsupported message type");
        }
    }

    // 校验消息内容，根据消息类型不同，content和mediaUrl的要求也不同
    private void validateMessagePayload(ChatMessageType type, String content, String mediaUrl) {
        // 根据消息类型进行校验
        switch (type) {
            case TEXT, EMOJI -> {
                // 文本和表情消息必须有内容
                if (!StringUtils.hasText(content)) {
                    throw new RuntimeException("Text or emoji content cannot be empty");
                }
            }
            case IMAGE, STICKER -> {
                // 图片和贴纸消息必须有mediaUrl
                if (!StringUtils.hasText(mediaUrl)) {
                    throw new RuntimeException("Image or sticker message is missing mediaUrl");
                }
            }
            default -> throw new RuntimeException("Unsupported message type");
        }
    }

    // 构建消息预览文本，用于在联系人列表中显示
    private String buildMessagePreview(ChatMessage message) {
        // 如果消息为空，返回null
        if (message == null) {
            return null;
        }
        // 联系人列表只展示摘要，因此不同消息类型需要做可读性转换。
        // 根据消息类型返回不同的预览文本
        return switch (message.getType()) {
            case IMAGE -> StringUtils.hasText(message.getContent()) ? "[Image] " + message.getContent() : "[Image]"; // 图片消息预览
            case STICKER -> StringUtils.hasText(message.getContent()) ? "[Sticker] " + message.getContent() : "[Sticker]"; // 贴纸消息预览
            case EMOJI -> message.getContent(); // 表情消息直接显示内容
            case TEXT -> message.getContent(); // 文本消息直接显示内容
        };
    }

    // 规范化可能为空的字符串，如果为空或空白则返回null
    private String normalizeNullable(String value) {
        // 如果值为空，直接返回null
        if (value == null) {
            return null;
        }
        // 去除首尾空格
        String normalized = value.trim();
        // 如果处理后为空，返回null；否则返回处理后的值
        return normalized.isEmpty() ? null : normalized;
    }

    // 根据用户信息构建联系人响应对象
    private SocialContactResponse buildContactFromUser(User user, String relationStatus, Long pendingRequestId, String pendingDirection) {
        // 创建并返回联系人响应对象
        return new SocialContactResponse(
                user.getId(), // 用户ID
                user.getUsername(), // 用户名
                user.getEmail(), // 邮箱
                user.getAvatar(), // 头像
                user.getBio(), // 简介
                user.getRole() != null ? user.getRole().name() : "USER", // 角色
                user.isEnabled(), // 是否启用
                relationStatus, // 关系状态
                pendingRequestId, // 待处理请求ID
                pendingDirection, // 待处理请求方向
                null, // 最后消息预览
                null, // 最后消息时间
                null, // 未读消息数
                user.getCreatedAt(), // 用户创建时间
                user.getUpdatedAt(), // 用户更新时间
                0, // 未读消息数
                true, // 是否有权限
                null // 活动时间
        );
    }

    // 内部方法，标记会话为已读
    private MessageReadReceiptResponse markConversationReadInternal(User currentUser, User targetUser) {
        // 查询目标用户发送给当前用户的所有未读消息ID
        List<Long> unreadMessageIds = chatMessageMapper.findUnreadMessageIds(targetUser.getId(), currentUser.getId());
        // 如果没有未读消息，返回null
        if (unreadMessageIds.isEmpty()) {
            return null;
        }

        // 获取当前时间作为已读时间
        LocalDateTime readAt = LocalDateTime.now();
        // 将目标用户发送给当前用户的消息标记为已读
        chatMessageMapper.markConversationRead(targetUser.getId(), currentUser.getId(), readAt);

        // 已读回执会通知消息发送方，便于前端把消息状态更新为"已读"。
        // 创建消息已读回执响应对象
        MessageReadReceiptResponse receipt = new MessageReadReceiptResponse(
                currentUser.getId(), // 当前用户ID（查看者）
                currentUser.getUsername(), // 当前用户名
                currentUser.getId(), // 操作者ID（这里是自己）
                unreadMessageIds, // 被标记为已读的消息ID列表
                readAt // 已读时间
        );
        // 创建WebSocket事件对象
        SocialWsEvent readEvent = new SocialWsEvent() {{
            setEventType(SocialEventType.MESSAGE_READ); // 设置事件类型为消息已读
            setRecipientUsername(targetUser.getUsername()); // 设置接收者用户名（消息发送者）
            setContactUserId(currentUser.getId()); // 设置联系人用户ID
            setNotice(currentUser.getUsername() + " read your messages"); // 设置通知消息
            setReadReceipt(receipt); // 设置已读回执
        }};
        // 通过社交事件发布器发布事件
        socialEventPublisher.publish(readEvent);

        // 同时直接发送 WebSocket
        try {
            // 使用WebSocket模板发送已读回执给指定用户
            messagingTemplate.convertAndSendToUser(
                    targetUser.getUsername(), // 接收者用户名（消息发送者）
                    "/queue/social", // 目标队列
                    readEvent // 事件对象
            );
        } catch (Exception ex) {
            // 如果直接发送WebSocket失败，记录警告日志
            log.warn("直接发送已读回执失败: {}", ex.getMessage());
        }

        // 返回已读回执响应
        return receipt;
    }
}
