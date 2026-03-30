package com.example.bickdemo.service;

// 引入MyBatis-Plus核心查询条件构建器，用于动态SQL拼接
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 引入MyBatis-Plus查询条件包装器
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
// 引入MyBatis-Plus分页插件，支持分页查询
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// 引入论坛作者主页响应DTO，用于返回作者信息
import com.example.bickdemo.dto.ForumAuthorProfileResponse;
// 引入论坛评论创建请求DTO，包含评论内容等
import com.example.bickdemo.dto.ForumPostCommentCreateRequest;
// 引入论坛评论响应DTO，用于返回评论信息
import com.example.bickdemo.dto.ForumPostCommentResponse;
// 引入论坛帖子创建请求DTO，包含标题、内容等
import com.example.bickdemo.dto.ForumPostCreateRequest;
// 引入论坛帖子详情响应DTO，包含帖子完整信息
import com.example.bickdemo.dto.ForumPostDetailResponse;
// 引入论坛帖子列表响应DTO，包含分页信息
import com.example.bickdemo.dto.ForumPostListResponse;
// 引入论坛帖子互动响应DTO，如点赞、收藏状态
import com.example.bickdemo.dto.ForumPostReactionResponse;
// 引入论坛帖子响应DTO，用于返回帖子基本信息
import com.example.bickdemo.dto.ForumPostResponse;
// 引入论坛帖子实体类，对应数据库表结构
import com.example.bickdemo.entity.ForumPost;
// 引入论坛评论实体类
import com.example.bickdemo.entity.ForumPostComment;
// 引入论坛帖子图片实体类
import com.example.bickdemo.entity.ForumPostImage;
// 引入论坛帖子互动实体类（点赞、收藏）
import com.example.bickdemo.entity.ForumPostReaction;
// 引入论坛帖子状态枚举（待审核、已通过、已驳回）
import com.example.bickdemo.entity.ForumPostStatus;
// 引入论坛帖子浏览记录实体类
import com.example.bickdemo.entity.ForumPostViewRecord;
// 引入论坛互动类型枚举（点赞、收藏）
import com.example.bickdemo.entity.ForumReactionType;
// 引入用户实体类
import com.example.bickdemo.entity.User;
// 引入用户角色枚举
import com.example.bickdemo.entity.UserRole;
// 引入好友请求Mapper，用于查询好友申请状态
import com.example.bickdemo.mapper.FriendRequestMapper;
// 引入好友关系Mapper，用于查询好友关系
import com.example.bickdemo.mapper.FriendshipMapper;
// 引入论坛评论Mapper
import com.example.bickdemo.mapper.ForumPostCommentMapper;
// 引入论坛帖子图片Mapper
import com.example.bickdemo.mapper.ForumPostImageMapper;
// 引入论坛帖子Mapper
import com.example.bickdemo.mapper.ForumPostMapper;
// 引入论坛帖子互动Mapper
import com.example.bickdemo.mapper.ForumPostReactionMapper;
// 引入论坛帖子浏览记录Mapper
import com.example.bickdemo.mapper.ForumPostViewRecordMapper;
// 引入用户Mapper
import com.example.bickdemo.mapper.UserMapper;
// 引入IP地址工具类，用于获取客户端真实IP
import com.example.bickdemo.util.IpAddressUtils;
// 引入HTTP请求对象，用于获取请求头等信息
import jakarta.servlet.http.HttpServletRequest;
// 引入Lombok注解，用于生成构造函数
import lombok.RequiredArgsConstructor;
// 引入数据库主键冲突异常，用于处理重复浏览记录
import org.springframework.dao.DuplicateKeyException;
// 引入Spring服务注解，标识这是服务层组件
import org.springframework.stereotype.Service;
// 引入事务注解，用于保证数据一致性
import org.springframework.transaction.annotation.Transactional;
// 引入Spring摘要工具，用于生成访客指纹
import org.springframework.util.DigestUtils;
// 引入Spring字符串工具类
import org.springframework.util.StringUtils;

// 引入Java标准字符集编码
import java.nio.charset.StandardCharsets;
// 引入Java日期类，用于处理日期
import java.time.LocalDate;
// 引入Java集合接口，用于接收用户ID集合
import java.util.Collection;
// 引入Java集合工具类
import java.util.Collections;
// 引入Java LinkedHashSet，保持插入顺序的Set，用于去重但保留顺序
import java.util.LinkedHashSet;
// 引入Java列表接口
import java.util.List;
// 引入Java映射接口
import java.util.Map;
// 引入Java对象工具类，用于对象比较
import java.util.Objects;
// 引入Java Set集合接口
import java.util.Set;
// 引入Java数组列表
import java.util.ArrayList;
// 引入Java日期时间类
import java.time.LocalDateTime;
// 引入Java函数接口，用于Stream操作
import java.util.function.Function;
// 引入Java Stream收集器
import java.util.stream.Collectors;

// 使用@Service注解标识这是服务层组件
@Service
// 使用@RequiredArgsConstructor注解，由Lombok生成构造函数注入依赖
@RequiredArgsConstructor
/**
 * 论坛业务服务类。
 * 负责帖子列表、帖子详情、发帖审核、评论、点赞收藏、作者主页等论坛相关功能，
 * 同时也维护帖子访问权限、审核状态以及与社交好友关系有关的作者信息展示。
 */
public class ForumService {

    // 默认页码，当未指定页码时使用
    private static final int DEFAULT_PAGE = 1;
    // 默认每页大小
    private static final int DEFAULT_SIZE = 10;
    // 每页最大条目数，防止查询过多数据
    private static final int MAX_SIZE = 30;
    // 待审核列表默认显示条数
    private static final int DEFAULT_PENDING_LIMIT = 12;
    // 待审核列表最大显示条数
    private static final int MAX_PENDING_LIMIT = 30;
    // 帖子最多允许的图片数量
    private static final int MAX_POST_IMAGES = 9;
    // 已注销用户的显示名称
    private static final String REMOVED_USERNAME = "已注销用户";
    // 与目标用户无任何关系
    private static final String RELATION_NONE = "NONE";
    // 与目标用户是好友关系
    private static final String RELATION_FRIEND = "FRIEND";
    // 已向目标用户发送好友请求
    private static final String RELATION_REQUEST_SENT = "REQUEST_SENT";
    // 收到目标用户的好友请求
    private static final String RELATION_REQUEST_RECEIVED = "REQUEST_RECEIVED";
    // 当前用户查看自己的主页
    private static final String RELATION_SELF = "SELF";
    // 访客ID请求头名称，用于匿名用户识别
    private static final String VISITOR_ID_HEADER = "X-Visitor-Id";

    // 论坛帖子Mapper，用于数据库操作
    private final ForumPostMapper forumPostMapper;
    // 论坛评论Mapper
    private final ForumPostCommentMapper forumPostCommentMapper;
    // 论坛帖子图片Mapper
    private final ForumPostImageMapper forumPostImageMapper;
    // 论坛帖子互动Mapper（点赞、收藏）
    private final ForumPostReactionMapper forumPostReactionMapper;
    // 论坛帖子浏览记录Mapper
    private final ForumPostViewRecordMapper forumPostViewRecordMapper;
    // 好友关系Mapper
    private final FriendshipMapper friendshipMapper;
    // 好友请求Mapper
    private final FriendRequestMapper friendRequestMapper;
    // 用户Mapper
    private final UserMapper userMapper;

    /**
     * 获取帖子列表方法。
     * 游客只能看到已通过审核的帖子；普通用户还能看到自己待审核/被驳回的帖子；
     * 管理员则可以查看所有帖子。
     * @param currentUsername 当前登录用户名，未登录为null
     * @param page 页码
     * @param size 每页条数
     * @param keyword 搜索关键词
     * @param category 帖子分类
     * @param sortBy 排序方式
     * @return 帖子列表响应，包含分页信息和帖子数据
     */
    public ForumPostListResponse getPosts(String currentUsername, Integer page, Integer size, String keyword, String category, String sortBy) {
        // 根据用户名解析当前登录用户
        User currentUser = resolveCurrentUser(currentUsername);
        // 计算实际页码，如果为空或小于1则使用默认值
        int current = page == null || page < 1 ? DEFAULT_PAGE : page;
        // 计算实际每页条数，限制在1到MAX_SIZE之间
        int resolvedSize = size == null ? DEFAULT_SIZE : Math.max(1, Math.min(size, MAX_SIZE));

        // 创建查询条件包装器
        QueryWrapper<ForumPost> wrapper = new QueryWrapper<>();
        // 过滤已删除的帖子
        wrapper.eq("deleted", 0);
        // 如果有关键词，添加模糊搜索条件（搜索标题或内容）
        if (StringUtils.hasText(keyword)) {
            // 去除关键词首尾空格
            String trimmedKeyword = keyword.trim();
            // 构建模糊查询条件：标题包含关键词或内容包含关键词
            wrapper.and(item -> item.like("title", trimmedKeyword).or().like("content", trimmedKeyword));
        }
        // 如果有分类条件，添加分类过滤
        if (StringUtils.hasText(category)) {
            wrapper.eq("category", category);
        }
        // 根据用户角色设置可见性条件
        if (currentUser == null) {
            // 游客只能看已审核通过的帖子
            wrapper.eq("status", ForumPostStatus.APPROVED.name());
        } else if (!isAdmin(currentUser)) {
            // 普通用户除了公开帖子外，还能看到自己发的帖子，方便在"我的内容"里查看状态
            wrapper.and(item -> item.eq("status", ForumPostStatus.APPROVED.name()).or().eq("user_id", currentUser.getId()));
        }
        // 应用排序规则
        applySort(wrapper, sortBy);

        // 执行分页查询，获取帖子分页结果
        Page<ForumPost> postPage = forumPostMapper.selectPage(new Page<>(current, resolvedSize), wrapper);
        // 获取当前页的帖子列表
        List<ForumPost> posts = postPage.getRecords();
        // 批量加载帖子作者信息，构建用户ID到用户的映射
        Map<Long, User> userMap = loadUsers(posts.stream().map(ForumPost::getUserId).toList());
        // 批量加载帖子的图片列表
        Map<Long, List<String>> postImagesMap = loadPostImages(posts);
        // 解析当前用户点赞的帖子ID集合
        Set<Long> likedIds = resolveReactionPostIds(currentUser, posts, ForumReactionType.LIKE);
        // 解析当前用户收藏的帖子ID集合
        Set<Long> favoritedIds = resolveReactionPostIds(currentUser, posts, ForumReactionType.FAVORITE);

        // 将帖子列表转换为响应DTO列表
        List<ForumPostResponse> records = posts.stream()
                // 遍历每个帖子，转换为响应对象
                .map(post -> toPostResponse(
                        post,
                        userMap.get(post.getUserId()),
                        currentUser,
                        likedIds,
                        favoritedIds,
                        postImagesMap.get(post.getId())
                ))
                // 转换为列表
                .toList();

        // 计算是否还有更多数据（当前页是否已满）
        boolean hasMore = postPage.getCurrent() * postPage.getSize() < postPage.getTotal();
        // 构建并返回帖子列表响应对象
        return new ForumPostListResponse(records, postPage.getTotal(), postPage.getCurrent(), postPage.getSize(), hasMore);
    }

    /**
     * 应用排序规则到查询条件
     * @param wrapper 查询条件包装器
     * @param sortBy 排序方式字符串
     */
    private void applySort(QueryWrapper<ForumPost> wrapper, String sortBy) {
        // 如果按浏览量排序
        if ("mostViewed".equals(sortBy)) {
            // 按浏览量降序，相同时按创建时间降序
            wrapper.orderByDesc("view_count").orderByDesc("created_at");
        } else if ("mostLiked".equals(sortBy)) {
            // 如果按点赞数排序
            // 按点赞数降序，相同时按创建时间降序
            wrapper.orderByDesc("like_count").orderByDesc("created_at");
        } else if ("mostCommented".equals(sortBy)) {
            // 如果按评论数排序
            // 按评论数降序，相同时按创建时间降序
            wrapper.orderByDesc("comment_count").orderByDesc("created_at");
        } else {
            // 默认排序：按置顶优先，然后按创建时间降序，最后按ID降序
            wrapper.orderByDesc("pinned").orderByDesc("created_at").orderByDesc("id");
        }
    }

    /**
     * 获取帖子详情方法。
     * 查看已通过审核的帖子时会按"同一用户/访客每天一次"累计浏览量；
     * 待审核帖子只有作者本人和管理员可见。
     * @param postId 帖子ID
     * @param currentUsername 当前登录用户名
     * @param request HTTP请求对象，用于记录浏览量
     * @param commentPage 评论分页页码
     * @param commentSize 评论分页每页条数
     * @return 帖子详情响应，包含帖子信息和评论列表
     */
    @Transactional
    public ForumPostDetailResponse getPostDetail(Long postId, String currentUsername, HttpServletRequest request,
                                                Integer commentPage, Integer commentSize) {
        // 解析当前登录用户
        User currentUser = resolveCurrentUser(currentUsername);
        // 获取可访问的帖子（检查权限）
        ForumPost post = requireAccessiblePost(postId, currentUser);
        // 如果是已审核通过的帖子，则记录浏览量
        if (post.getStatus() == ForumPostStatus.APPROVED) {
            // 只有对外可见的帖子才记浏览量，且同一用户/访客当天只计一次
            recordDailyPostView(post, currentUser, request);
            // 重新获取帖子以获取更新后的浏览数
            post = requireAccessiblePost(postId, currentUser);
        }

        // 计算评论分页页码，默认第1页
        int page = commentPage != null ? commentPage : 1;
        // 计算评论分页每页条数，默认10条
        int size = commentSize != null ? commentSize : 10;
        // 创建评论分页对象
        Page<ForumPostComment> commentPageObj = new Page<>(page, size);
        // 构建评论查询条件
        LambdaQueryWrapper<ForumPostComment> commentWrapper = new LambdaQueryWrapper<ForumPostComment>()
                // 过滤属于该帖子的评论
                .eq(ForumPostComment::getPostId, postId)
                // 普通用户只看已通过审核的评论，管理员看所有评论
                .and(!isAdmin(currentUser), wrapper -> wrapper.eq(ForumPostComment::getReviewStatus, ForumPostStatus.APPROVED.name()))
                // 按创建时间升序排列
                .orderByAsc(ForumPostComment::getCreatedAt)
                // 相同时按ID升序，保持评论顺序稳定
                .orderByAsc(ForumPostComment::getId);
        // 执行评论分页查询
        Page<ForumPostComment> commentResult = forumPostCommentMapper.selectPage(commentPageObj, commentWrapper);
        // 获取评论列表
        List<ForumPostComment> comments = commentResult.getRecords();
        // 获取评论总数
        long commentTotal = commentResult.getTotal();

        // 收集所有需要查询的用户ID（作者和评论者）
        Set<Long> userIds = new LinkedHashSet<>();
        // 添加帖子作者ID
        userIds.add(post.getUserId());
        // 添加所有评论的用户ID
        comments.stream().map(ForumPostComment::getUserId).forEach(userIds::add);
        // 添加所有回复目标用户ID
        comments.stream().map(ForumPostComment::getReplyToUserId).forEach(userIds::add);
        // 批量加载用户信息
        Map<Long, User> userMap = loadUsers(userIds);

        // 获取当前用户是否点赞该帖子
        Set<Long> likedIds = resolveReactionPostIds(currentUser, List.of(post), ForumReactionType.LIKE);
        // 获取当前用户是否收藏该帖子
        Set<Long> favoritedIds = resolveReactionPostIds(currentUser, List.of(post), ForumReactionType.FAVORITE);
        // 获取帖子的图片URL列表
        List<String> imageUrls = resolveImageUrls(post, forumPostImageMapper.findByPostId(postId));

        // 将帖子转换为响应DTO
        ForumPostResponse postResponse = toPostResponse(
                post,
                userMap.get(post.getUserId()),
                currentUser,
                likedIds,
                favoritedIds,
                imageUrls
        );
        // 将评论列表转换为响应DTO列表
        List<ForumPostCommentResponse> commentResponses = comments.stream()
                .map(comment -> toCommentResponse(
                        comment,
                        userMap.get(comment.getUserId()),
                        userMap.get(comment.getReplyToUserId()),
                        currentUser
                ))
                .toList();

        // 构建并返回帖子详情响应对象
        return new ForumPostDetailResponse(postResponse, commentResponses, commentTotal, page, size);
    }

    /**
     * 创建帖子方法。
     * 管理员发帖直接通过审核，普通用户发帖进入待审核状态。
     * @param currentUsername 当前登录用户名
     * @param request 帖子创建请求，包含标题、内容等
     * @return 创建的帖子响应DTO
     */
    @Transactional
    public ForumPostResponse createPost(String currentUsername, ForumPostCreateRequest request) {
        // 获取当前登录用户
        User currentUser = requireUser(currentUsername);

        // 创建帖子实体对象
        ForumPost post = new ForumPost();
        // 规范化图片URL列表（去重、限制数量）
        List<String> imageUrls = normalizeImageUrls(request);
        // 设置帖子作者ID
        post.setUserId(currentUser.getId());
        // 设置帖子标题（去除首尾空格）
        post.setTitle(request.getTitle().trim());
        // 设置帖子内容（去除首尾空格）
        post.setContent(request.getContent().trim());
        // 设置帖子分类
        post.setCategory(request.getCategory());
        // 如果有图片，设置封面图URL（取第一张）
        post.setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
        // 初始化浏览量为0
        post.setViewCount(0L);
        // 初始化点赞数为0
        post.setLikeCount(0L);
        // 初始化收藏数为0
        post.setFavoriteCount(0L);
        // 初始化评论数为0
        post.setCommentCount(0L);
        // 论坛采用"普通用户先审后发、管理员直接发布"的审核策略
        // 如果是管理员则直接审核通过，否则待审核
        post.setStatus(isAdmin(currentUser) ? ForumPostStatus.APPROVED : ForumPostStatus.PENDING);
        // 设置审核备注
        post.setReviewRemark(isAdmin(currentUser) ? "管理员发布，已直接通过审核" : "等待管理员审核后展示");
        // 如果是管理员，审核信息也一并记录
        if (isAdmin(currentUser)) {
            post.setReviewerId(currentUser.getId());
            post.setReviewedAt(LocalDateTime.now());
        }
        // 将帖子插入数据库
        forumPostMapper.insert(post);
        // 保存帖子的多张图片
        savePostImages(post.getId(), imageUrls);

        // 构建并返回帖子响应DTO
        return toPostResponse(post, currentUser, currentUser, Collections.emptySet(), Collections.emptySet(), imageUrls);
    }

    /**
     * 获取待审核帖子列表，仅管理员使用。
     * @param currentUsername 当前登录管理员用户名
     * @param limit 返回条数限制
     * @return 待审核帖子列表
     */
    public List<ForumPostResponse> getPendingPosts(String currentUsername, Integer limit) {
        // 获取当前登录用户并验证
        User currentUser = requireUser(currentUsername);
        // 确保是管理员操作
        ensureAdmin(currentUser);

        // 计算实际限制条数，默认DEFAULT_PENDING_LIMIT，最大MAX_PENDING_LIMIT
        int resolvedLimit = limit == null ? DEFAULT_PENDING_LIMIT : Math.max(1, Math.min(limit, MAX_PENDING_LIMIT));
        // 构建查询条件
        QueryWrapper<ForumPost> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                // 只查询待审核状态的帖子
                .eq("status", ForumPostStatus.PENDING.name())
                // 按创建时间升序排列，先提交的先审核
                .orderByAsc("created_at")
                // 限制返回条数
                .last("LIMIT " + resolvedLimit);

        // 执行查询获取待审核帖子列表
        List<ForumPost> posts = forumPostMapper.selectList(wrapper);
        // 批量加载用户信息
        Map<Long, User> userMap = loadUsers(posts.stream().map(ForumPost::getUserId).toList());
        // 批量加载帖子图片
        Map<Long, List<String>> postImagesMap = loadPostImages(posts);

        // 转换为响应DTO列表
        return posts.stream()
                .map(post -> toPostResponse(
                        post,
                        userMap.get(post.getUserId()),
                        currentUser,
                        // 待审核列表不需要显示点赞收藏状态
                        Collections.emptySet(),
                        Collections.emptySet(),
                        postImagesMap.get(post.getId())
                ))
                .toList();
    }

    /**
     * 获取待审核评论列表，仅管理员使用。
     * @param currentUsername 当前登录管理员用户名
     * @param limit 返回条数限制
     * @return 待审核评论列表
     */
    public List<ForumPostCommentResponse> getPendingComments(String currentUsername, Integer limit) {
        // 获取当前登录用户并验证
        User currentUser = requireUser(currentUsername);
        // 确保是管理员操作
        ensureAdmin(currentUser);

        // 计算实际限制条数
        int resolvedLimit = limit == null ? DEFAULT_PENDING_LIMIT : Math.max(1, Math.min(limit, MAX_PENDING_LIMIT));
        // 构建评论查询条件
        LambdaQueryWrapper<ForumPostComment> wrapper = new LambdaQueryWrapper<ForumPostComment>()
                // 过滤未删除的评论
                .eq(ForumPostComment::getDeleted, 0)
                // 只查询待审核状态的评论
                .eq(ForumPostComment::getReviewStatus, ForumPostStatus.PENDING.name())
                // 按创建时间升序排列
                .orderByAsc(ForumPostComment::getCreatedAt)
                // 限制返回条数
                .last("LIMIT " + resolvedLimit);

        // 执行查询获取待审核评论列表
        List<ForumPostComment> comments = forumPostCommentMapper.selectList(wrapper);

        // 收集需要查询的用户ID
        Set<Long> userIds = new LinkedHashSet<>();
        // 遍历评论添加用户ID
        comments.forEach(c -> {
            // 添加评论作者ID
            userIds.add(c.getUserId());
            // 如果有回复目标用户，也添加其ID
            if (c.getReplyToUserId() != null) userIds.add(c.getReplyToUserId());
        });
        // 批量加载用户信息
        Map<Long, User> userMap = loadUsers(userIds);

        // 转换为响应DTO列表
        return comments.stream()
                .map(comment -> toCommentResponse(
                        comment,
                        userMap.get(comment.getUserId()),
                        userMap.get(comment.getReplyToUserId()),
                        currentUser
                ))
                .toList();
    }

    /**
     * 审核通过评论方法。
     * @param currentUsername 当前登录管理员用户名
     * @param commentId 要审核的评论ID
     * @return 审核后的评论响应DTO
     */
    public ForumPostCommentResponse approveComment(String currentUsername, Long commentId) {
        // 获取当前登录用户并验证
        User currentUser = requireUser(currentUsername);
        // 确保是管理员操作
        ensureAdmin(currentUser);

        // 根据ID查询评论
        ForumPostComment comment = forumPostCommentMapper.selectById(commentId);
        // 检查评论是否存在且未删除
        if (comment == null || comment.getDeleted() == 1) {
            throw new RuntimeException("评论不存在");
        }
        // 设置评论状态为已通过审核
        comment.setReviewStatus(ForumPostStatus.APPROVED.name());
        // 更新评论信息
        forumPostCommentMapper.updateById(comment);

        // 获取评论作者信息
        User author = userMapper.selectById(comment.getUserId());
        // 获取回复目标用户信息（如果有）
        User replyToUser = comment.getReplyToUserId() == null ? null : userMapper.selectById(comment.getReplyToUserId());
        // 构建并返回评论响应DTO
        return toCommentResponse(comment, author, replyToUser, currentUser);
    }

    /**
     * 驳回评论方法。
     * @param currentUsername 当前登录管理员用户名
     * @param commentId 要驳回的评论ID
     * @return 驳回后的评论响应DTO
     */
    public ForumPostCommentResponse rejectComment(String currentUsername, Long commentId) {
        // 获取当前登录用户并验证
        User currentUser = requireUser(currentUsername);
        // 确保是管理员操作
        ensureAdmin(currentUser);

        // 根据ID查询评论
        ForumPostComment comment = forumPostCommentMapper.selectById(commentId);
        // 检查评论是否存在且未删除
        if (comment == null || comment.getDeleted() == 1) {
            throw new RuntimeException("评论不存在");
        }
        // 设置评论状态为已驳回
        comment.setReviewStatus(ForumPostStatus.REJECTED.name());
        // 更新评论信息
        forumPostCommentMapper.updateById(comment);

        // 获取评论作者信息
        User author = userMapper.selectById(comment.getUserId());
        // 获取回复目标用户信息（如果有）
        User replyToUser = comment.getReplyToUserId() == null ? null : userMapper.selectById(comment.getReplyToUserId());
        // 构建并返回评论响应DTO
        return toCommentResponse(comment, author, replyToUser, currentUser);
    }

    /**
     * 获取热门帖子列表方法。
     * 根据浏览量、点赞数、评论数综合计算热度。
     * @param currentUsername 当前登录用户名
     * @param limit 返回条数限制
     * @return 热门帖子列表
     */
    public List<ForumPostResponse> getHotPosts(String currentUsername, Integer limit) {
        // 解析当前登录用户（游客也可以调用）
        User currentUser = resolveCurrentUser(currentUsername);
        // 计算实际限制条数，默认5条，最大20条
        int resolvedLimit = limit == null ? 5 : Math.max(1, Math.min(limit, 20));

        // 构建查询条件
        QueryWrapper<ForumPost> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                // 只查询已审核通过的帖子
                .eq("status", ForumPostStatus.APPROVED.name())
                // 按浏览量降序
                .orderByDesc("view_count")
                // 相同时按点赞数降序
                .orderByDesc("like_count")
                // 相同时按评论数降序
                .orderByDesc("comment_count")
                // 限制返回条数
                .last("LIMIT " + resolvedLimit);

        // 执行查询获取热门帖子列表
        List<ForumPost> posts = forumPostMapper.selectList(wrapper);
        // 批量加载用户信息
        Map<Long, User> userMap = loadUsers(posts.stream().map(ForumPost::getUserId).toList());
        // 批量加载帖子图片
        Map<Long, List<String>> postImagesMap = loadPostImages(posts);
        // 获取当前用户点赞的帖子ID集合
        Set<Long> likedIds = resolveReactionPostIds(currentUser, posts, ForumReactionType.LIKE);
        // 获取当前用户收藏的帖子ID集合
        Set<Long> favoritedIds = resolveReactionPostIds(currentUser, posts, ForumReactionType.FAVORITE);

        // 转换为响应DTO列表
        return posts.stream()
                .map(post -> toPostResponse(
                        post,
                        userMap.get(post.getUserId()),
                        currentUser,
                        likedIds,
                        favoritedIds,
                        postImagesMap.get(post.getId())
                ))
                .toList();
    }

    /**
     * 获取当前用户的帖子列表方法。
     * @param currentUsername 当前登录用户名
     * @param page 页码
     * @param size 每页条数
     * @return 当前用户的帖子列表响应
     */
    public ForumPostListResponse getMyPosts(String currentUsername, Integer page, Integer size) {
        // 获取当前登录用户
        User currentUser = requireUser(currentUsername);
        // 计算实际页码
        int current = page == null || page < 1 ? DEFAULT_PAGE : page;
        // 计算实际每页条数，默认5条
        int resolvedSize = size == null ? 5 : Math.max(1, Math.min(size, MAX_SIZE));

        // 构建查询条件
        QueryWrapper<ForumPost> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                // 只查询当前用户的帖子
                .eq("user_id", currentUser.getId())
                // 按创建时间降序
                .orderByDesc("created_at")
                // 相同时按ID降序
                .orderByDesc("id");

        // 执行分页查询
        Page<ForumPost> postPage = forumPostMapper.selectPage(new Page<>(current, resolvedSize), wrapper);
        // 获取帖子列表
        List<ForumPost> posts = postPage.getRecords();
        // 批量加载用户信息
        Map<Long, User> userMap = loadUsers(posts.stream().map(ForumPost::getUserId).toList());
        // 批量加载帖子图片
        Map<Long, List<String>> postImagesMap = loadPostImages(posts);
        // 获取点赞状态
        Set<Long> likedIds = resolveReactionPostIds(currentUser, posts, ForumReactionType.LIKE);
        // 获取收藏状态
        Set<Long> favoritedIds = resolveReactionPostIds(currentUser, posts, ForumReactionType.FAVORITE);

        // 转换为响应DTO列表
        List<ForumPostResponse> records = posts.stream()
                .map(post -> toPostResponse(
                        post,
                        userMap.get(post.getUserId()),
                        currentUser,
                        likedIds,
                        favoritedIds,
                        postImagesMap.get(post.getId())
                ))
                .toList();

        // 计算是否还有更多数据
        boolean hasMore = postPage.getCurrent() * postPage.getSize() < postPage.getTotal();
        // 构建并返回响应
        return new ForumPostListResponse(records, postPage.getTotal(), postPage.getCurrent(), postPage.getSize(), hasMore);
    }

    /**
     * 置顶或取消置顶帖子方法，仅管理员使用。
     * @param currentUsername 当前登录管理员用户名
     * @param postId 帖子ID
     * @param pinned 是否置顶
     * @return 更新后的帖子响应DTO
     */
    @Transactional
    public ForumPostResponse pinPost(String currentUsername, Long postId, Boolean pinned) {
        // 获取当前登录用户并验证
        User currentUser = requireUser(currentUsername);
        // 确保是管理员操作
        ensureAdmin(currentUser);

        // 获取帖子
        ForumPost post = requirePost(postId);
        // 设置置顶状态（如果pinned为null则视为false）
        post.setPinned(pinned != null && pinned);
        // 更新帖子信息
        forumPostMapper.updateById(post);

        // 重新获取帖子以获取最新状态
        post = requirePost(postId);
        // 获取帖子作者
        User author = userMapper.selectById(post.getUserId());
        // 获取帖子图片
        List<String> imageUrls = resolveImageUrls(post, forumPostImageMapper.findByPostId(postId));
        // 获取点赞状态
        Set<Long> likedIds = resolveReactionPostIds(currentUser, List.of(post), ForumReactionType.LIKE);
        // 获取收藏状态
        Set<Long> favoritedIds = resolveReactionPostIds(currentUser, List.of(post), ForumReactionType.FAVORITE);
        // 构建并返回响应
        return toPostResponse(post, author, currentUser, likedIds, favoritedIds, imageUrls);
    }

    /**
     * 通过帖子审核方法。
     * @param currentUsername 当前登录管理员用户名
     * @param postId 帖子ID
     * @return 审核后的帖子响应DTO
     */
    @Transactional
    public ForumPostResponse approvePost(String currentUsername, Long postId) {
        // 调用审核方法，状态设为已通过
        return reviewPost(currentUsername, postId, ForumPostStatus.APPROVED, "管理员已通过审核");
    }

    /**
     * 驳回帖子审核方法。
     * @param currentUsername 当前登录管理员用户名
     * @param postId 帖子ID
     * @return 驳回后的帖子响应DTO
     */
    @Transactional
    public ForumPostResponse rejectPost(String currentUsername, Long postId) {
        // 调用审核方法，状态设为已驳回
        return reviewPost(currentUsername, postId, ForumPostStatus.REJECTED, "管理员未通过审核");
    }

    /**
     * 发表评论或回复方法。
     * 评论只能挂在已通过审核的帖子下，且不允许回复自己的评论。
     * @param currentUsername 当前登录用户名
     * @param postId 帖子ID
     * @param request 评论创建请求
     * @return 创建的评论响应DTO
     */
    @Transactional
    public ForumPostCommentResponse createComment(String currentUsername, Long postId, ForumPostCommentCreateRequest request) {
        // 获取当前登录用户
        User currentUser = requireUser(currentUsername);
        // 确保帖子已审核通过
        requireApprovedPost(postId);
        // 解析父评论（如果是回复）
        ForumPostComment parentComment = resolveParentComment(postId, request.getParentCommentId());
        // 不能回复自己的评论
        if (parentComment != null && Objects.equals(parentComment.getUserId(), currentUser.getId())) {
            throw new RuntimeException("不能回复自己的评论");
        }

        // 创建评论实体
        ForumPostComment comment = new ForumPostComment();
        // 设置所属帖子ID
        comment.setPostId(postId);
        // 设置评论作者ID
        comment.setUserId(currentUser.getId());
        // 设置父评论ID（顶级评论为null）
        comment.setParentCommentId(parentComment == null ? null : parentComment.getId());
        // replyToUserId最终由父评论决定，避免前端随意伪造回复对象
        comment.setReplyToUserId(resolveReplyToUserId(parentComment, request.getReplyToUserId()));
        // 设置评论内容（去除首尾空格）
        comment.setContent(request.getContent().trim());
        // 管理员评论直接通过审核，普通用户评论需要审核
        comment.setReviewStatus(isAdmin(currentUser) ? ForumPostStatus.APPROVED.name() : ForumPostStatus.PENDING.name());
        // 插入评论到数据库
        forumPostCommentMapper.insert(comment);
        // 增加帖子的评论计数
        forumPostMapper.updateCommentCount(postId, 1L);

        // 获取回复目标用户信息（如果有）
        User replyToUser = comment.getReplyToUserId() == null ? null : userMapper.selectById(comment.getReplyToUserId());
        // 构建并返回评论响应DTO
        return toCommentResponse(comment, currentUser, replyToUser, currentUser);
    }

    /**
     * 切换点赞状态方法。
     * @param currentUsername 当前登录用户名
     * @param postId 帖子ID
     * @return 点赞操作响应DTO
     */
    @Transactional
    public ForumPostReactionResponse toggleLike(String currentUsername, Long postId) {
        // 确保帖子已审核通过才能点赞
        requireApprovedPost(postId);
        // 调用通用切换互动方法
        return toggleReaction(currentUsername, postId, ForumReactionType.LIKE);
    }

    /**
     * 切换收藏状态方法。
     * @param currentUsername 当前登录用户名
     * @param postId 帖子ID
     * @return 收藏操作响应DTO
     */
    @Transactional
    public ForumPostReactionResponse toggleFavorite(String currentUsername, Long postId) {
        // 确保帖子已审核通过才能收藏
        requireApprovedPost(postId);
        // 调用通用切换互动方法
        return toggleReaction(currentUsername, postId, ForumReactionType.FAVORITE);
    }

    /**
     * 删除帖子方法。
     * 作者本人可以删除自己的帖子；管理员只允许删除未通过审核或待审核帖子。
     * @param currentUsername 当前登录用户名
     * @param postId 帖子ID
     */
    @Transactional
    public void deletePost(String currentUsername, Long postId) {
        // 获取当前登录用户
        User currentUser = requireUser(currentUsername);
        // 获取帖子
        ForumPost post = requirePost(postId);
        // 检查是否有删除权限
        if (!canDeletePost(currentUser, post)) {
            throw new RuntimeException("你没有权限删除这条帖子");
        }

        // 删除帖子的图片记录
        forumPostImageMapper.deleteByPostId(postId);
        // 删除帖子（逻辑删除）
        forumPostMapper.deleteById(postId);
    }

    /**
     * 获取论坛作者主页信息方法。
     * 同时返回与当前登录用户之间的好友关系，方便前端决定是否显示"加好友"按钮。
     * @param userId 作者用户ID
     * @param currentUsername 当前登录用户名
     * @return 作者主页响应DTO
     */
    public ForumAuthorProfileResponse getUserProfile(Long userId, String currentUsername) {
        // 根据ID查询用户
        User user = userMapper.selectById(userId);
        // 检查用户是否存在
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 解析当前登录用户
        User currentUser = resolveCurrentUser(currentUsername);
        // 判断是否在查看自己的主页
        boolean self = currentUser != null && Objects.equals(currentUser.getId(), userId);
        // 解析好友关系状态
        String relationStatus = resolveRelationStatus(currentUser, user);
        // 解析待处理的好友请求ID
        Long pendingRequestId = resolvePendingRequestId(currentUser, user);

        // 统计该用户的帖子数量
        long postCount = forumPostMapper.countByUserId(userId);
        // 统计该用户的评论数量
        long commentCount = forumPostCommentMapper.countByUserId(userId);

        // 构建并返回作者主页响应
        return new ForumAuthorProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatar(),
                user.getBio(),
                // 如果角色为空默认为普通用户
                user.getRole() == null ? "USER" : user.getRole().name(),
                postCount,
                commentCount,
                relationStatus,
                pendingRequestId,
                self,
                // 是否可以发送好友请求（非自己、无关系、且没有待处理请求）
                currentUser != null && !self && RELATION_NONE.equals(relationStatus),
                user.getCreatedAt()
        );
    }

    /**
     * 切换互动状态（点赞/收藏）的通用方法。
     * @param currentUsername 当前登录用户名
     * @param postId 帖子ID
     * @param type 互动类型（点赞或收藏）
     * @return 互动操作响应DTO
     */
    private ForumPostReactionResponse toggleReaction(String currentUsername, Long postId, ForumReactionType type) {
        // 获取当前登录用户
        User currentUser = requireUser(currentUsername);
        // 获取帖子
        ForumPost post = requirePost(postId);

        // 查询当前用户是否已对该帖子进行过该互动
        ForumPostReaction existing = forumPostReactionMapper.findByPostIdAndUserIdAndType(postId, currentUser.getId(), type);
        boolean active;
        if (existing == null) {
            // 首次操作视为新增互动，并同步递增帖子计数字段
            // 创建新的互动记录
            ForumPostReaction reaction = new ForumPostReaction();
            reaction.setPostId(postId);
            reaction.setUserId(currentUser.getId());
            reaction.setType(type);
            // 插入互动记录
            forumPostReactionMapper.insert(reaction);
            // 增加帖子的互动计数
            incrementReactionCount(postId, type, 1L);
            active = true;
        } else {
            // 已有记录则视为取消互动
            // 删除互动记录
            forumPostReactionMapper.deleteByPostIdAndUserIdAndType(postId, currentUser.getId(), type);
            // 减少帖子的互动计数
            incrementReactionCount(postId, type, -1L);
            active = false;
        }

        // 重新获取帖子以获取最新计数
        post = requirePost(postId);
        // 构建并返回响应
        return new ForumPostReactionResponse(postId, type.name(), active, safeLong(post.getLikeCount()), safeLong(post.getFavoriteCount()));
    }

    /**
     * 记录每日浏览量方法。
     * 同一用户或访客对同一帖子每天只记录一次浏览量。
     * @param post 帖子对象
     * @param currentUser 当前用户（可能为null表示游客）
     * @param request HTTP请求对象
     */
    private void recordDailyPostView(ForumPost post, User currentUser, HttpServletRequest request) {
        // 参数校验，防止空指针
        if (post == null || post.getId() == null || request == null) {
            return;
        }

        // 创建浏览记录实体
        ForumPostViewRecord viewRecord = new ForumPostViewRecord();
        // 设置帖子ID
        viewRecord.setPostId(post.getId());
        // 设置用户ID（如果已登录）
        viewRecord.setUserId(currentUser != null ? currentUser.getId() : null);
        // 设置每日访客标识（用户ID、访客ID或IP+UA指纹）
        viewRecord.setViewerKey(buildDailyViewerKey(currentUser, request));
        // 设置浏览日期
        viewRecord.setViewedOn(LocalDate.now());

        try {
            // 插入浏览记录
            forumPostViewRecordMapper.insert(viewRecord);
            // 增加帖子的浏览计数
            forumPostMapper.updateViewCount(post.getId(), 1L);
        } catch (DuplicateKeyException ignored) {
            // 唯一索引命中说明今天已经记过这位访客的浏览量，不再重复累计
        }
    }

    /**
     * 增加或减少帖子互动计数方法。
     * @param postId 帖子ID
     * @param type 互动类型
     * @param delta 变化量（增加为正数，减少为负数）
     */
    private void incrementReactionCount(Long postId, ForumReactionType type, long delta) {
        // 根据互动类型分别处理
        if (type == ForumReactionType.LIKE) {
            // 更新点赞数
            forumPostMapper.updateLikeCount(postId, delta);
            return;
        }
        // 更新收藏数
        forumPostMapper.updateFavoriteCount(postId, delta);
    }

    /**
     * 获取帖子对象方法，如果不存在则抛出异常。
     * @param postId 帖子ID
     * @return 帖子对象
     */
    private ForumPost requirePost(Long postId) {
        // 根据ID查询帖子
        ForumPost post = forumPostMapper.selectById(postId);
        // 检查帖子是否存在
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        return post;
    }

    /**
     * 确保帖子已审核通过方法。
     * @param postId 帖子ID
     * @return 帖子对象
     */
    private ForumPost requireApprovedPost(Long postId) {
        // 先获取帖子
        ForumPost post = requirePost(postId);
        // 检查帖子状态是否为已审核
        if (post.getStatus() != ForumPostStatus.APPROVED) {
            // 论坛互动能力仅面向审核通过的内容开放，避免未公开帖子被提前互动
            throw new RuntimeException("帖子审核通过后才可以互动");
        }
        return post;
    }

    /**
     * 获取可访问的帖子方法。
     * 根据用户角色判断是否有权访问该帖子。
     * @param postId 帖子ID
     * @param currentUser 当前用户
     * @return 可访问的帖子对象
     */
    private ForumPost requireAccessiblePost(Long postId, User currentUser) {
        // 先获取帖子
        ForumPost post = requirePost(postId);
        // 检查当前用户是否有权访问
        if (canAccessPost(currentUser, post)) {
            return post;
        }
        throw new RuntimeException("帖子正在审核中，暂时不能查看");
    }

    /**
     * 获取用户对象方法，如果不存在则抛出异常。
     * @param username 用户名
     * @return 用户对象
     */
    private User requireUser(String username) {
        // 先解析用户
        User user = resolveCurrentUser(username);
        // 检查用户是否存在
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    /**
     * 解析当前用户方法。
     * @param username 用户名
     * @return 用户对象，如果用户名为空或不存在则返回null
     */
    private User resolveCurrentUser(String username) {
        // 如果用户名为空或空白，直接返回null
        if (!StringUtils.hasText(username)) {
            return null;
        }
        // 根据用户名查询用户
        return userMapper.findByUsername(username);
    }

    /**
     * 确保是管理员方法，如果不是则抛出异常。
     * @param user 用户对象
     */
    private void ensureAdmin(User user) {
        // 检查是否为管理员
        if (!isAdmin(user)) {
            throw new RuntimeException("只有管理员可以执行该操作");
        }
    }

    /**
     * 批量加载用户信息方法。
     * @param userIds 用户ID集合
     * @return 用户ID到用户对象的映射
     */
    private Map<Long, User> loadUsers(Collection<Long> userIds) {
        // 过滤空值并去重
        List<Long> resolvedIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        // 如果没有有效ID，返回空Map
        if (resolvedIds.isEmpty()) {
            return Map.of();
        }
        // 批量查询用户并转换为Map
        return userMapper.selectBatchIds(resolvedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /**
     * 解析当前用户对帖子列表的互动ID集合。
     * @param currentUser 当前用户
     * @param posts 帖子列表
     * @param type 互动类型
     * @return 已被当前用户互动的帖子ID集合
     */
    private Set<Long> resolveReactionPostIds(User currentUser, List<ForumPost> posts, ForumReactionType type) {
        // 如果用户为空或帖子列表为空，直接返回空集合
        if (currentUser == null || posts.isEmpty()) {
            return Collections.emptySet();
        }

        // 提取帖子ID并过滤去重
        List<Long> postIds = posts.stream()
                .map(ForumPost::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        // 如果没有有效ID，返回空集合
        if (postIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 批量查出当前用户已经互动过的帖子ID
        return forumPostReactionMapper.findByUserIdAndPostIdsAndType(currentUser.getId(), postIds, type).stream()
                .map(ForumPostReaction::getPostId)
                .collect(Collectors.toSet());
    }

    /**
     * 将帖子实体转换为响应DTO。
     * @param post 帖子实体
     * @param author 作者用户
     * @param currentUser 当前登录用户
     * @param likedIds 当前用户点赞的帖子ID集合
     * @param favoritedIds 当前用户收藏的帖子ID集合
     * @param imageUrls 帖子图片URL列表
     * @return 帖子响应DTO
     */
    private ForumPostResponse toPostResponse(
            ForumPost post,
            User author,
            User currentUser,
            Set<Long> likedIds,
            Set<Long> favoritedIds,
            List<String> imageUrls
    ) {
        // 如果没有提供图片，使用备用图片URL
        List<String> resolvedImageUrls = imageUrls == null ? fallbackImageUrls(post) : imageUrls;
        // 响应对象里同时聚合作者信息、互动计数、当前用户权限，供前端直接渲染
        return new ForumPostResponse(
                post.getId(),
                post.getUserId(),
                // 如果作者为空显示"已注销用户"
                author == null ? REMOVED_USERNAME : author.getUsername(),
                author == null ? null : author.getAvatar(),
                author == null ? null : author.getBio(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl(),
                resolvedImageUrls,
                safeLong(post.getViewCount()),
                safeLong(post.getLikeCount()),
                safeLong(post.getFavoriteCount()),
                safeLong(post.getCommentCount()),
                post.getCategory(),
                post.getPinned(),
                // 如果状态为空默认为已通过
                post.getStatus() == null ? ForumPostStatus.APPROVED.name() : post.getStatus().name(),
                post.getReviewRemark(),
                post.getReviewedAt(),
                // 当前用户是否点赞该帖子
                likedIds.contains(post.getId()),
                // 当前用户是否收藏该帖子
                favoritedIds.contains(post.getId()),
                // 当前用户是否是作者
                currentUser != null && Objects.equals(currentUser.getId(), post.getUserId()),
                // 当前用户是否可以删除该帖子
                canDeletePost(currentUser, post),
                // 当前用户是否可以审核该帖子
                canReviewPost(currentUser, post),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    /**
     * 将评论实体转换为响应DTO。
     * @param comment 评论实体
     * @param author 评论作者
     * @param replyToUser 回复目标用户
     * @param currentUser 当前登录用户
     * @return 评论响应DTO
     */
    private ForumPostCommentResponse toCommentResponse(ForumPostComment comment, User author, User replyToUser, User currentUser) {
        return new ForumPostCommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getUserId(),
                // 如果作者为空显示"已注销用户"
                author == null ? REMOVED_USERNAME : author.getUsername(),
                author == null ? null : author.getAvatar(),
                author == null ? null : author.getBio(),
                comment.getParentCommentId(),
                comment.getReplyToUserId(),
                // 回复目标用户名
                replyToUser == null ? null : replyToUser.getUsername(),
                comment.getContent(),
                // 当前用户是否是评论作者
                currentUser != null && Objects.equals(currentUser.getId(), comment.getUserId()),
                comment.getReviewStatus(),
                comment.getCreatedAt()
        );
    }

    /**
     * 安全获取Long值方法，防止空指针。
     * @param value 可能为null的Long值
     * @return 如果为null返回0L，否则返回原值
     */
    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 构建每日访客标识Key方法。
     * 用于区分不同用户/访客对同一帖子每天的访问。
     * @param currentUser 当前用户（可能为null）
     * @param request HTTP请求对象
     * @return 每日访客标识Key
     */
    private String buildDailyViewerKey(User currentUser, HttpServletRequest request) {
        // 如果已登录用户，使用用户ID作为标识
        if (currentUser != null && currentUser.getId() != null) {
            return "USER:" + currentUser.getId();
        }

        // 尝试获取访客ID请求头
        String visitorId = normalizeNullable(request.getHeader(VISITOR_ID_HEADER));
        if (visitorId != null) {
            // 使用访客ID，长度截断到64
            return "GUEST:" + trimToLength(visitorId, 64);
        }

        // 获取客户端IP
        String ip = normalizeNullable(IpAddressUtils.resolveClientIp(request));
        // 获取User-Agent
        String userAgent = normalizeNullable(request.getHeader("User-Agent"));
        // 组合指纹：IP + User-Agent
        String fingerprint = (ip == null ? "unknown-ip" : ip) + "|" + (userAgent == null ? "unknown-agent" : userAgent);
        // 对指纹进行MD5加密后返回
        return "GUEST:" + DigestUtils.md5DigestAsHex(fingerprint.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 规范化可能为空的字符串方法。
     * @param value 原始字符串
     * @return 规范化后的字符串，如果为空或null则返回null
     */
    private String normalizeNullable(String value) {
        // 如果为null直接返回
        if (value == null) {
            return null;
        }
        // 去除首尾空格
        String normalized = value.trim();
        // 如果为空字符串返回null
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * 截断字符串到指定长度方法。
     * @param value 原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String trimToLength(String value, int maxLength) {
        // 如果为空或长度小于等于最大长度，直接返回
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        // 截断到指定长度
        return value.substring(0, maxLength);
    }

    /**
     * 规范化图片URL列表方法。
     * 去除空值、去重、限制数量。
     * @param request 帖子创建请求
     * @return 规范化后的图片URL列表
     */
    private List<String> normalizeImageUrls(ForumPostCreateRequest request) {
        // 创建图片URL列表
        List<String> imageUrls = new ArrayList<>();
        // 如果请求中包含图片URL列表
        if (request.getImageUrls() != null) {
            // 遍历处理每张图片
            for (String imageUrl : request.getImageUrls()) {
                String normalized = normalizeNullable(imageUrl);
                if (normalized != null) {
                    imageUrls.add(normalized);
                }
            }
        }
        // 如果列表为空，兼容旧版只传单图imageUrl的请求结构
        if (imageUrls.isEmpty()) {
            String legacyImageUrl = normalizeNullable(request.getImageUrl());
            if (legacyImageUrl != null) {
                imageUrls.add(legacyImageUrl);
            }
        }
        // 检查图片数量是否超过限制
        if (imageUrls.size() > MAX_POST_IMAGES) {
            throw new RuntimeException("最多只能上传 9 张图片");
        }
        // 去重并限制数量后返回
        return imageUrls.stream().distinct().limit(MAX_POST_IMAGES).toList();
    }

    /**
     * 保存帖子图片方法。
     * @param postId 帖子ID
     * @param imageUrls 图片URL列表
     */
    private void savePostImages(Long postId, List<String> imageUrls) {
        // 参数校验
        if (postId == null || imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        // 多图使用独立从表存储，并通过sort_order保持前端上传顺序
        for (int index = 0; index < imageUrls.size(); index++) {
            // 创建帖子图片实体
            ForumPostImage postImage = new ForumPostImage();
            postImage.setPostId(postId);
            postImage.setImageUrl(imageUrls.get(index));
            // 设置图片排序顺序
            postImage.setSortOrder(index);
            // 插入数据库
            forumPostImageMapper.insert(postImage);
        }
    }

    /**
     * 批量加载帖子图片方法。
     * @param posts 帖子列表
     * @return 帖子ID到图片URL列表的映射
     */
    private Map<Long, List<String>> loadPostImages(List<ForumPost> posts) {
        // 提取帖子ID列表
        List<Long> postIds = posts.stream()
                .map(ForumPost::getId)
                .filter(Objects::nonNull)
                .toList();
        // 如果没有有效ID，返回空Map
        if (postIds.isEmpty()) {
            return Map.of();
        }
        // 批量查询图片并按帖子ID分组
        Map<Long, List<ForumPostImage>> grouped = forumPostImageMapper.findByPostIds(postIds).stream()
                .collect(Collectors.groupingBy(ForumPostImage::getPostId));

        // 构建帖子ID到图片URL列表的映射
        return posts.stream().collect(Collectors.toMap(
                ForumPost::getId,
                post -> resolveImageUrls(post, grouped.get(post.getId()))
        ));
    }

    /**
     * 解析帖子图片URL列表方法。
     * @param post 帖子实体
     * @param postImages 帖子图片实体列表
     * @return 图片URL列表
     */
    private List<String> resolveImageUrls(ForumPost post, List<ForumPostImage> postImages) {
        // 如果有独立存储的图片，使用独立图片
        if (postImages != null && !postImages.isEmpty()) {
            return postImages.stream()
                    .map(ForumPostImage::getImageUrl)
                    .filter(Objects::nonNull)
                    .toList();
        }
        // 否则使用帖子的封面图字段
        return fallbackImageUrls(post);
    }

    /**
     * 获取备用图片URL列表方法。
     * @param post 帖子实体
     * @return 图片URL列表
     */
    private List<String> fallbackImageUrls(ForumPost post) {
        // 如果帖子为空或没有封面图，返回空列表
        if (post == null || !StringUtils.hasText(post.getImageUrl())) {
            return List.of();
        }
        // 返回封面图URL
        return List.of(post.getImageUrl());
    }

    /**
     * 解析父评论方法。
     * 验证父评论是否属于同一帖子。
     * @param postId 帖子ID
     * @param parentCommentId 父评论ID
     * @return 父评论实体
     */
    private ForumPostComment resolveParentComment(Long postId, Long parentCommentId) {
        // 如果没有父评论ID，返回null
        if (parentCommentId == null) {
            return null;
        }
        // 查询父评论
        ForumPostComment parentComment = forumPostCommentMapper.selectById(parentCommentId);
        // 检查父评论是否存在且属于同一帖子
        if (parentComment == null || !Objects.equals(parentComment.getPostId(), postId)) {
            // 回复链路必须保证父评论属于同一篇帖子，避免跨帖串评论
            throw new RuntimeException("回复的评论不存在");
        }
        return parentComment;
    }

    /**
     * 解析回复目标用户ID方法。
     * 优先使用父评论的作者作为回复目标。
     * @param parentComment 父评论
     * @param requestedReplyToUserId 前端请求的回复目标用户ID
     * @return 最终的回复目标用户ID
     */
    private Long resolveReplyToUserId(ForumPostComment parentComment, Long requestedReplyToUserId) {
        // 如果没有父评论，返回null
        if (parentComment == null) {
            return null;
        }
        // 如果请求的回复目标用户ID与父评论作者一致，使用请求的ID
        if (requestedReplyToUserId != null && Objects.equals(requestedReplyToUserId, parentComment.getUserId())) {
            return requestedReplyToUserId;
        }
        // 否则使用父评论的作者ID
        return parentComment.getUserId();
    }

    /**
     * 审核帖子通用方法。
     * @param currentUsername 当前登录管理员用户名
     * @param postId 帖子ID
     * @param nextStatus 审核状态
     * @param remark 审核备注
     * @return 审核后的帖子响应DTO
     */
    private ForumPostResponse reviewPost(String currentUsername, Long postId, ForumPostStatus nextStatus, String remark) {
        // 获取当前登录用户并验证
        User currentUser = requireUser(currentUsername);
        // 确保是管理员操作
        ensureAdmin(currentUser);

        // 获取帖子
        ForumPost post = requirePost(postId);
        // 审核动作会同时记录审核人、审核时间和审核备注，方便后台追踪
        post.setStatus(nextStatus);
        post.setReviewerId(currentUser.getId());
        post.setReviewedAt(LocalDateTime.now());
        post.setReviewRemark(remark);
        // 更新帖子信息
        forumPostMapper.updateById(post);

        // 重新获取帖子以获取最新状态
        post = requirePost(postId);
        // 获取帖子作者
        User author = userMapper.selectById(post.getUserId());
        // 获取帖子图片
        List<String> imageUrls = resolveImageUrls(post, forumPostImageMapper.findByPostId(postId));
        // 构建并返回响应
        return toPostResponse(post, author, currentUser, Collections.emptySet(), Collections.emptySet(), imageUrls);
    }

    /**
     * 判断当前用户是否有权访问帖子。
     * @param currentUser 当前用户
     * @param post 帖子
     * @return 是否有权访问
     */
    private boolean canAccessPost(User currentUser, ForumPost post) {
        // 已审核通过的帖子对所有人可见
        if (post.getStatus() == ForumPostStatus.APPROVED) {
            return true;
        }
        // 未登录用户不能访问未审核通过的帖子
        if (currentUser == null) {
            return false;
        }
        // 管理员或帖子作者可以访问
        return isAdmin(currentUser) || Objects.equals(currentUser.getId(), post.getUserId());
    }

    /**
     * 判断当前用户是否有权删除帖子。
     * @param currentUser 当前用户
     * @param post 帖子
     * @return 是否有权删除
     */
    private boolean canDeletePost(User currentUser, ForumPost post) {
        // 如果用户或帖子为空，不能删除
        if (currentUser == null || post == null) {
            return false;
        }
        // 作者可以删除自己的帖子
        if (Objects.equals(currentUser.getId(), post.getUserId())) {
            return true;
        }
        // 管理员只处理审核中的内容，不主动删除已经公开展示的帖子
        return isAdmin(currentUser) && post.getStatus() != ForumPostStatus.APPROVED;
    }

    /**
     * 判断当前用户是否有权审核帖子。
     * @param currentUser 当前用户
     * @param post 帖子
     * @return 是否有权审核
     */
    private boolean canReviewPost(User currentUser, ForumPost post) {
        // 必须已登录、是管理员、且帖子处于待审核状态
        return currentUser != null
                && isAdmin(currentUser)
                && post.getStatus() == ForumPostStatus.PENDING;
    }

    /**
     * 判断用户是否为管理员方法。
     * @param user 用户对象
     * @return 是否为管理员
     */
    private boolean isAdmin(User user) {
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    /**
     * 解析与目标用户的关系状态方法。
     * @param currentUser 当前用户
     * @param targetUser 目标用户
     * @return 关系状态字符串
     */
    private String resolveRelationStatus(User currentUser, User targetUser) {
        // 如果任一用户为空，返回无关系
        if (currentUser == null || targetUser == null) {
            return RELATION_NONE;
        }
        // 如果查看的是自己的主页
        if (Objects.equals(currentUser.getId(), targetUser.getId())) {
            return RELATION_SELF;
        }
        // 检查是否为好友关系
        if (friendshipMapper.existsFriendship(currentUser.getId(), targetUser.getId())) {
            return RELATION_FRIEND;
        }
        // 作者主页需要带出好友申请状态，便于前端直接展示"已发送/待处理"等文案
        // 查询当前用户和目标用户之间是否有待处理的好友请求
        var pendingRequest = friendRequestMapper.findPendingBetweenUsers(currentUser.getId(), targetUser.getId());
        if (pendingRequest == null) {
            return RELATION_NONE;
        }
        // 根据请求发送者判断是已发送还是已收到
        return Objects.equals(pendingRequest.getSenderId(), currentUser.getId())
                ? RELATION_REQUEST_SENT
                : RELATION_REQUEST_RECEIVED;
    }

    /**
     * 解析待处理的好友请求ID方法。
     * @param currentUser 当前用户
     * @param targetUser 目标用户
     * @return 待处理的好友请求ID，如果没有则返回null
     */
    private Long resolvePendingRequestId(User currentUser, User targetUser) {
        // 如果是查看自己的主页或任一用户为空，返回null
        if (currentUser == null || targetUser == null || Objects.equals(currentUser.getId(), targetUser.getId())) {
            return null;
        }
        // 查询待处理的好友请求
        var pendingRequest = friendRequestMapper.findPendingBetweenUsers(currentUser.getId(), targetUser.getId());
        // 返回请求ID或null
        return pendingRequest == null ? null : pendingRequest.getId();
    }
}
