package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.ForumAuthorProfileResponse;
import com.example.bickdemo.dto.ForumPostCommentCreateRequest;
import com.example.bickdemo.dto.ForumPostCommentResponse;
import com.example.bickdemo.dto.ForumPostCreateRequest;
import com.example.bickdemo.dto.ForumPostDetailResponse;
import com.example.bickdemo.dto.ForumPostListResponse;
import com.example.bickdemo.dto.ForumPostReactionResponse;
import com.example.bickdemo.dto.ForumPostResponse;
import com.example.bickdemo.service.ForumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<ForumPostListResponse>> getPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ForumPostListResponse response = forumService.getPosts(
                userDetails == null ? null : userDetails.getUsername(),
                page,
                size,
                keyword
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<ForumPostDetailResponse>> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostDetailResponse response = forumService.getPostDetail(
                    postId,
                    userDetails == null ? null : userDetails.getUsername()
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    @PostMapping("/posts")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForumPostResponse>> createPost(
            @Valid @RequestBody ForumPostCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostResponse response = forumService.createPost(userDetails.getUsername(), request);
            return ResponseEntity.ok(ApiResponse.success("发布成功", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForumPostCommentResponse>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody ForumPostCommentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostCommentResponse response = forumService.createComment(userDetails.getUsername(), postId, request);
            return ResponseEntity.ok(ApiResponse.success("评论成功", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    @PostMapping("/posts/{postId}/like")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForumPostReactionResponse>> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostReactionResponse response = forumService.toggleLike(userDetails.getUsername(), postId);
            return ResponseEntity.ok(ApiResponse.success("操作成功", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    @PostMapping("/posts/{postId}/favorite")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForumPostReactionResponse>> toggleFavorite(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostReactionResponse response = forumService.toggleFavorite(userDetails.getUsername(), postId);
            return ResponseEntity.ok(ApiResponse.success("操作成功", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<ForumAuthorProfileResponse>> getUserProfile(@PathVariable Long userId) {
        try {
            ForumAuthorProfileResponse response = forumService.getUserProfile(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }
}
