package com.example.bickdemo.controller.user;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.ChatMessageRequest;
import com.example.bickdemo.dto.ChatMessageResponse;
import com.example.bickdemo.dto.ConversationMessagesResponse;
import com.example.bickdemo.dto.FriendRequestCreateRequest;
import com.example.bickdemo.dto.FriendRequestResponse;
import com.example.bickdemo.dto.MessageReadReceiptResponse;
import com.example.bickdemo.dto.SocialContactResponse;
import com.example.bickdemo.dto.SocialEventType;
import com.example.bickdemo.dto.SocialWsEvent;
import com.example.bickdemo.dto.UserProfileResponse;
import com.example.bickdemo.dto.UserSearchResponse;
import com.example.bickdemo.service.JwtService;
import com.example.bickdemo.service.SocialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
/**
 * 社交接口控制器。
 * 所有接口都要求登录后访问，统一承接好友关系和私聊消息相关请求。
 */
public class SocialController {

    private final SocialService socialService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtService jwtService;

    /**
     * 按用户名关键字搜索用户。
     */
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> searchUsers(
            @RequestParam("keyword") String keyword,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (!StringUtils.hasText(keyword)) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        List<UserSearchResponse> response = socialService.searchUsers(keyword, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取指定用户的详细信息（用于聊天场景查看陌生人资料）。
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            UserProfileResponse response = socialService.getUserProfile(userDetails.getUsername(), userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 发起好友申请。
     */
    @PostMapping("/friend-requests")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> createFriendRequest(
            @Valid @RequestBody FriendRequestCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            FriendRequestResponse response = socialService.createFriendRequest(userDetails.getUsername(), request);
            return ResponseEntity.ok(ApiResponse.success("Friend request sent", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 获取收到的待处理好友申请。
     */
    @GetMapping("/friend-requests/received")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> listPendingReceivedRequests(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<FriendRequestResponse> response = socialService.listPendingReceivedRequests(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取自己发出的待处理好友申请。
     */
    @GetMapping("/friend-requests/sent")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> listPendingSentRequests(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<FriendRequestResponse> response = socialService.listPendingSentRequests(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 接受好友申请。
     */
    @PostMapping("/friend-requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> acceptFriendRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            FriendRequestResponse response = socialService.acceptFriendRequest(userDetails.getUsername(), requestId);
            return ResponseEntity.ok(ApiResponse.success("Friend request accepted", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 拒绝好友申请。
     */
    @PostMapping("/friend-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> rejectFriendRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            FriendRequestResponse response = socialService.rejectFriendRequest(userDetails.getUsername(), requestId);
            return ResponseEntity.ok(ApiResponse.success("Friend request rejected", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 获取联系人列表及最近消息摘要。
     */
    @GetMapping("/contacts")
    public ResponseEntity<ApiResponse<List<SocialContactResponse>>> listContacts(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<SocialContactResponse> response = socialService.listContacts(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 分页获取与指定用户的会话消息。
     */
    @GetMapping("/messages/{targetUserId}")
    public ResponseEntity<ApiResponse<ConversationMessagesResponse>> getConversationMessages(
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "24") Integer size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ConversationMessagesResponse response = socialService.getConversationMessages(
                    userDetails.getUsername(),
                    targetUserId,
                    page,
                    size
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 把与指定用户的会话标记为已读。
     */
    @PostMapping("/messages/{targetUserId}/read")
    public ResponseEntity<ApiResponse<MessageReadReceiptResponse>> markConversationRead(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            MessageReadReceiptResponse response = socialService.markConversationRead(
                    userDetails.getUsername(),
                    targetUserId
            );
            return ResponseEntity.ok(ApiResponse.success("Conversation marked as read", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 发送私聊消息。
     */
    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ChatMessageResponse response = socialService.sendMessage(userDetails.getUsername(), request);
            return ResponseEntity.ok(ApiResponse.success("Message sent", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 测试 WebSocket 直接发送（调试用）。
     */
    @PostMapping("/test-ws/{targetUsername}")
    public ResponseEntity<ApiResponse<String>> testWebSocket(
            @PathVariable String targetUsername,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            SocialWsEvent testEvent = new SocialWsEvent();
            testEvent.setEventType(SocialEventType.CHAT_MESSAGE);
            testEvent.setRecipientUsername(targetUsername);
            testEvent.setNotice("Test message from " + userDetails.getUsername());

            messagingTemplate.convertAndSendToUser(
                    targetUsername,
                    "/queue/social",
                    testEvent
            );

            return ResponseEntity.ok(ApiResponse.success("Test message sent to " + targetUsername, null));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 撤回消息接口
     */
    @PostMapping("/messages/{id}/recall")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recallMessage(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        String username = extractUsernameFromToken(token);
        Map<String, Object> result = socialService.recallMessage(username, id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 重新编辑发送消息接口
     */
    @PutMapping("/messages/{id}/resend")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> resendMessage(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @RequestBody ChatMessageRequest request) {
        String username = extractUsernameFromToken(token);
        ChatMessageResponse result = socialService.resendMessage(username, id, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 从 Authorization 请求头中提取用户名。
     */
    private String extractUsernameFromToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            return jwtService.extractUsername(token);
        }
        throw new RuntimeException("Invalid Authorization header format");
    }
}
