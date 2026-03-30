package com.example.bickdemo.service; // 定义包路径

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // MyBatis-Plus 条件构造器
import com.example.bickdemo.config.CacheNames; // 缓存名称常量
import com.example.bickdemo.dto.*; // 引入所有 DTO 类
import com.example.bickdemo.entity.*; // 引入所有实体类
import com.example.bickdemo.mapper.TicketMapper; // 工单 Mapper
import com.example.bickdemo.mapper.TicketMessageMapper; // 工单消息 Mapper
import com.example.bickdemo.mapper.UserMapper; // 用户 Mapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // MyBatis-Plus 分页
import com.fasterxml.jackson.core.JsonProcessingException; // JSON 处理异常
import com.fasterxml.jackson.core.type.TypeReference; // Jackson 类型引用
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 序列化/反序列化
import lombok.RequiredArgsConstructor; // Lombok 生成构造函数
import lombok.extern.slf4j.Slf4j; // 日志注解
import org.springframework.cache.annotation.CacheEvict; // 缓存清除注解
import org.springframework.cache.annotation.Cacheable; // 缓存注解
import org.springframework.security.core.Authentication; // 认证信息
import org.springframework.security.core.context.SecurityContextHolder; // 安全上下文持有器
import org.springframework.stereotype.Service; // 服务层注解
import org.springframework.transaction.annotation.Transactional; // 事务注解

import java.time.LocalDate; // 日期类
import java.time.LocalDateTime; // 日期时间类
import java.time.LocalTime; // 时间类
import java.time.format.DateTimeFormatter; // 日期时间格式化
import java.util.Collections; // 集合工具类
import java.util.List; // 列表接口
import java.util.Random; // 随机数生成
import java.util.stream.Collectors; // Stream 收集器

/**
 * 工单服务类
 * @author Administrator
 */
@Service // 标识为服务层组件
@RequiredArgsConstructor // 生成包含所有 final 字段的构造函数
@Slf4j // 生成日志对象
public class TicketService {

    private final TicketMapper ticketMapper; // 工单数据访问层
    private final TicketMessageMapper ticketMessageMapper; // 工单消息数据访问层
    private final UserMapper userMapper; // 用户数据访问层
    private final ObjectMapper objectMapper; // JSON 处理工具

    private static final Random RANDOM = new Random(); // 随机数生成器实例

    /**
     * 生成工单编号：TK + timestamp + 4位随机数
     */
    private String generateTicketNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")); // 当前时间格式化为 14 位时间戳
        int random = RANDOM.nextInt(10000); // 生成 0-9999 之间的随机数
        return "TK" + timestamp + String.format("%04d", random); // 拼接工单编号，格式：TK + 时间戳 + 4 位随机数
    }

    /**
     * 序列化图片列表为 JSON 字符串
     */
    private String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) { // 检查图片列表是否为空
            return null; // 为空返回 null
        }
        try {
            return objectMapper.writeValueAsString(images); // 将图片列表序列化为 JSON 字符串
        } catch (JsonProcessingException e) {
            log.error("序列化图片列表失败", e); // 序列化失败时记录错误日志
            return null; // 返回 null
        }
    }

    /**
     * 反序列化 JSON 字符串为图片列表
     */
    private List<String> deserializeImages(String json) {
        if (json == null || json.isEmpty()) { // 检查 JSON 字符串是否为空
            return Collections.emptyList(); // 为空返回空列表
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {}); // 将 JSON 反序列化为字符串列表
        } catch (JsonProcessingException e) {
            log.error("反序列化图片列表失败", e); // 反序列化失败时记录错误日志
            return Collections.emptyList(); // 返回空列表
        }
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // 获取当前认证信息
        if (authentication == null || !authentication.isAuthenticated()) { // 检查是否已认证
            throw new RuntimeException("用户未登录"); // 未认证抛出异常
        }
        String username = authentication.getName(); // 获取当前用户名
        User user = userMapper.findByUsername(username); // 根据用户名查询用户
        if (user == null) {
            throw new RuntimeException("用户不存在：" + username); // 用户不存在抛出异常
        }
        return user.getId(); // 返回用户 ID
    }

    /**
     * 创建工单
     */
    @Transactional // 开启事务
    @CacheEvict(cacheNames = CacheNames.TICKETS_STATS, allEntries = true) // 清除工单统计缓存
    public TicketResponse createTicket(TicketRequest request) {
        Long userId = getCurrentUserId(); // 获取当前用户 ID

        Ticket ticket = new Ticket(); // 创建工单实例
        ticket.setTicketNo(generateTicketNo()); // 生成并设置工单编号
        ticket.setTitle(request.getTitle()); // 设置工单标题
        ticket.setContent(request.getContent()); // 设置工单内容
        ticket.setType(request.getType()); // 设置工单类型
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.NORMAL); // 设置优先级，默认普通
        ticket.setStatus(TicketStatus.OPEN); // 设置工单状态为待处理
        ticket.setImages(serializeImages(request.getImages())); // 序列化并设置图片列表
        ticket.setUserId(userId); // 设置创建用户 ID

        ticketMapper.insert(ticket); // 插入工单到数据库
        return convertToResponse(ticket); // 转换为响应 DTO 并返回
    }

    /**
     * 获取当前用户的工单列表
     */
    public List<TicketResponse> getUserTickets() {
        Long userId = getCurrentUserId(); // 获取当前用户 ID
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>() // 创建条件构造器
                .eq(Ticket::getUserId, userId) // 筛选当前用户的工单
                .eq(Ticket::getDeleted, 0) // 筛选未删除的工单
                .orderByDesc(Ticket::getCreatedAt); // 按创建时间倒序排列
        return ticketMapper.selectList(wrapper).stream() // 查询工单列表并转换为 Stream
                .map(this::convertToResponse) // 转换为响应 DTO
                .collect(Collectors.toList()); // 收集为列表
    }

    /**
     * 获取当前用户的工单详情（包含消息列表）
     */
    public TicketResponse getUserTicketById(Long id) {
        Long userId = getCurrentUserId(); // 获取当前用户 ID
        Ticket ticket = ticketMapper.selectById(id); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + id); // 不存在抛出异常
        }
        if (!ticket.getUserId().equals(userId)) { // 检查是否有权限访问
            throw new RuntimeException("无权访问此工单"); // 无权限抛出异常
        }
        return convertToResponseWithMessages(ticket); // 转换为带消息列表的响应 DTO
    }

    /**
     * 用户发送消息
     */
    @Transactional // 开启事务
    @CacheEvict(cacheNames = CacheNames.TICKETS_STATS, allEntries = true) // 清除工单统计缓存
    public TicketMessageResponse addUserMessage(Long ticketId, TicketMessageRequest request) {
        Long userId = getCurrentUserId(); // 获取当前用户 ID
        Ticket ticket = ticketMapper.selectById(ticketId); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + ticketId); // 不存在抛出异常
        }
        if (!ticket.getUserId().equals(userId)) { // 检查是否有权限访问
            throw new RuntimeException("无权访问此工单"); // 无权限抛出异常
        }

        TicketMessage message = new TicketMessage(); // 创建消息实例
        message.setTicketId(ticketId); // 设置关联的工单 ID
        message.setSenderId(userId); // 设置发送者 ID
        message.setSenderType(SenderType.USER); // 设置发送者类型为用户
        message.setContent(request.getContent()); // 设置消息内容
        message.setImages(serializeImages(request.getImages())); // 序列化并设置图片列表

        ticketMessageMapper.insert(message); // 插入消息到数据库

        // 如果工单状态是 OPEN，改为 PROCESSING
        if (ticket.getStatus() == TicketStatus.OPEN) { // 检查工单状态是否为待处理
            ticket.setStatus(TicketStatus.PROCESSING); // 更新状态为处理中
            ticketMapper.updateById(ticket); // 更新工单
        }

        return convertToMessageResponse(message); // 转换为消息响应 DTO 并返回
    }

    /**
     * 用户提交工单反馈
     */
    @Transactional // 开启事务
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true) // 清除工单统计和分页缓存
    public TicketResponse submitFeedback(Long id, TicketFeedbackRequest request) {
        Long userId = getCurrentUserId(); // 获取当前用户 ID
        Ticket ticket = ticketMapper.selectById(id); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + id); // 不存在抛出异常
        }
        if (!ticket.getUserId().equals(userId)) { // 检查是否有权限访问
            throw new RuntimeException("无权访问此工单"); // 无权限抛出异常
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED) { // 检查工单是否已解决
            throw new RuntimeException("只能在工单已解决后提交反馈"); // 未解决抛出异常
        }
        if (ticket.getRating() != null) { // 检查是否已提交过反馈
            throw new RuntimeException("您已提交过反馈，无法重复提交"); // 已提交抛出异常
        }

        ticket.setRating(request.getRating()); // 设置用户评分
        ticket.setFeedback(request.getFeedback()); // 设置用户反馈内容
        ticketMapper.updateById(ticket); // 更新工单
        return convertToResponse(ticket); // 转换为响应 DTO 并返回
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员获取所有工单（分页，支持筛选）
     */
    @Cacheable(cacheNames = CacheNames.TICKETS_PAGE, // 缓存工单分页列表
               key = "'page:' + #p1 + ':size:' + #p2 + ':status:' + (#p0 != null ? #p0.name() : 'all') + ':type:' + (#p3 != null ? #p3.name() : 'all') + ':priority:' + (#p4 != null ? #p4.name() : 'all')") // 缓存 key
    public Page<TicketResponse> getAdminTicketsPage(TicketStatus status, int page, int size, // 分页查询工单方法
                                                     TicketType type, TicketPriority priority, // 类型和优先级筛选
                                                     String keyword) { // 关键词搜索
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>() // 创建条件构造器
                .eq(Ticket::getDeleted, 0) // 筛选未删除的工单
                .eq(status != null, Ticket::getStatus, status) // 按状态筛选（可选）
                .eq(type != null, Ticket::getType, type) // 按类型筛选（可选）
                .eq(priority != null, Ticket::getPriority, priority) // 按优先级筛选（可选）
                .orderByDesc(Ticket::getCreatedAt); // 按创建时间倒序排列

        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) { // 检查关键词是否为空
            wrapper.and(w -> w // 组合查询条件
                    .like(Ticket::getTicketNo, keyword) // 模糊匹配工单编号
                    .or() // 或
                    .like(Ticket::getTitle, keyword) // 模糊匹配工单标题
                    .or() // 或
                    .inSql(Ticket::getUserId, "SELECT id FROM users WHERE username LIKE '%" + keyword + "%'") // 子查询匹配用户名
            );
        }

        Page<Ticket> ticketPage = ticketMapper.selectPage(new Page<>(page, size), wrapper); // 执行分页查询
        Page<TicketResponse> result = new Page<>(ticketPage.getCurrent(), ticketPage.getSize()); // 创建分页响应对象
        result.setTotal(ticketPage.getTotal()); // 设置总记录数
        result.setRecords(ticketPage.getRecords().stream() // 转换工单列表为响应 DTO 列表
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        return result; // 返回分页响应
    }

    /**
     * 管理员获取工单详情
     */
    public TicketResponse getAdminTicketById(Long id) {
        Ticket ticket = ticketMapper.selectById(id); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + id); // 不存在抛出异常
        }
        return convertToResponseWithMessages(ticket); // 转换为带消息列表的响应 DTO 并返回
    }

    /**
     * 管理员分配工单
     */
    @Transactional // 开启事务
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true) // 清除缓存
    public TicketResponse assignTicket(Long id, Long assigneeId) {
        Ticket ticket = ticketMapper.selectById(id); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + id); // 不存在抛出异常
        }

        User assignee = userMapper.selectById(assigneeId); // 查询分配的客服用户
        if (assignee == null) { // 检查客服是否存在
            throw new RuntimeException("分配的客服不存在：" + assigneeId); // 不存在抛出异常
        }

        ticket.setAssigneeId(assigneeId); // 设置分配给的管理员 ID
        if (ticket.getStatus() == TicketStatus.OPEN) { // 检查工单状态是否为待处理
            ticket.setStatus(TicketStatus.ASSIGNED); // 更新状态为已分配
        }
        ticketMapper.updateById(ticket); // 更新工单
        return convertToResponse(ticket); // 转换为响应 DTO 并返回
    }

    /**
     * 管理员标记工单为处理中
     */
    @Transactional // 开启事务
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true) // 清除缓存
    public TicketResponse processTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + id); // 不存在抛出异常
        }

        ticket.setStatus(TicketStatus.PROCESSING); // 设置工单状态为处理中
        ticketMapper.updateById(ticket); // 更新工单
        return convertToResponse(ticket); // 转换为响应 DTO 并返回
    }

    /**
     * 管理员回复工单
     */
    @Transactional // 开启事务
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true) // 清除缓存
    public TicketMessageResponse replyTicket(Long id, TicketReplyRequest request, Long adminId) {
        Ticket ticket = ticketMapper.selectById(id); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + id); // 不存在抛出异常
        }

        // 添加管理员消息
        TicketMessage message = new TicketMessage(); // 创建消息实例
        message.setTicketId(id); // 设置关联的工单 ID
        message.setSenderId(adminId); // 设置发送者（管理员）ID
        message.setSenderType(SenderType.ADMIN); // 设置发送者类型为管理员
        message.setContent(request.getContent()); // 设置消息内容
        ticketMessageMapper.insert(message); // 插入消息到数据库

        // 更新工单的回复信息
        ticket.setReplyContent(request.getContent()); // 设置回复内容
        ticket.setReplyTime(LocalDateTime.now()); // 设置回复时间
        ticket.setStatus(TicketStatus.PROCESSING); // 更新工单状态为处理中
        ticketMapper.updateById(ticket); // 更新工单

        return convertToMessageResponse(message); // 转换为消息响应 DTO 并返回
    }

    /**
     * 管理员解决工单
     */
    @Transactional // 开启事务
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true) // 清除缓存
    public TicketResponse resolveTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + id); // 不存在抛出异常
        }

        ticket.setStatus(TicketStatus.RESOLVED); // 设置工单状态为已解决
        ticket.setResolvedTime(LocalDateTime.now()); // 设置解决时间
        ticketMapper.updateById(ticket); // 更新工单
        return convertToResponse(ticket); // 转换为响应 DTO 并返回
    }

    /**
     * 管理员关闭工单
     */
    @Transactional // 开启事务
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true) // 清除缓存
    public TicketResponse closeTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + id); // 不存在抛出异常
        }

        ticket.setStatus(TicketStatus.CLOSED); // 设置工单状态为已关闭
        ticketMapper.updateById(ticket); // 更新工单
        return convertToResponse(ticket); // 转换为响应 DTO 并返回
    }

    /**
     * 重新开启已关闭的工单
     */
    @Transactional // 开启事务
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true) // 清除缓存
    public TicketResponse reopenTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id); // 根据 ID 查询工单
        if (ticket == null) { // 检查工单是否存在
            throw new RuntimeException("工单不存在：" + id); // 不存在抛出异常
        }

        ticket.setStatus(TicketStatus.OPEN); // 重置工单状态为待处理
        ticket.setResolvedTime(null); // 清除解决时间
        ticketMapper.updateById(ticket); // 更新工单
        return convertToResponse(ticket); // 转换为响应 DTO 并返回
    }

    /**
     * 管理员获取工单统计
     */
    @Cacheable(cacheNames = CacheNames.TICKETS_STATS, key = "'stats'") // 缓存工单统计
    public TicketStatsResponse getStats() {
        TicketStatsResponse stats = new TicketStatsResponse(); // 创建统计响应对象
        stats.setPendingCount(ticketMapper.countByStatus(TicketStatus.OPEN)); // 待处理工单数
        stats.setProcessingCount(ticketMapper.countByStatus(TicketStatus.PROCESSING)); // 处理中工单数
        stats.setResolvedCount(ticketMapper.countResolved()); // 已解决工单数
        stats.setClosedCount(ticketMapper.countClosed()); // 已关闭工单数
        stats.setTotalCount(ticketMapper.countAll()); // 工单总数

        // 今日新增
        LocalDateTime todayStart = LocalDate.now().atStartOfDay(); // 获取今天零点时间
        stats.setTodayNewCount(ticketMapper.countNewSince(todayStart)); // 今日新增工单数

        // 本周新增
        LocalDateTime weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay(); // 获取本周一零点时间
        stats.setWeekNewCount(ticketMapper.countNewSince(weekStart)); // 本周新增工单数

        // 本月新增
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay(); // 获取本月一日零点时间
        stats.setMonthNewCount(ticketMapper.countNewSince(monthStart)); // 本月新增工单数

        // 平均响应时间
        stats.setAvgResponseTime(ticketMapper.avgResponseTime()); // 计算平均响应时间

        // 平均解决时间
        stats.setAvgResolveTime(ticketMapper.avgResolveTime()); // 计算平均解决时间

        // 用户满意度（评分4星及以上的比例）
        Long totalRated = ticketMapper.countRated(); // 获取已评分工单数
        Long highRating = ticketMapper.countHighRating(); // 获取高评分（4星及以上）工单数
        if (totalRated != null && totalRated > 0) { // 检查是否有已评分工单
            stats.setSatisfactionRate((double) highRating / totalRated * 100); // 计算满意度百分比
        } else {
            stats.setSatisfactionRate(0.0); // 无评分数据时设为 0
        }

        return stats; // 返回统计结果
    }

    /**
     * 转换实体为响应 DTO
     */
    private TicketResponse convertToResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse(); // 创建响应对象
        response.setId(ticket.getId()); // 设置 ID
        response.setTicketNo(ticket.getTicketNo()); // 设置工单编号
        response.setTitle(ticket.getTitle()); // 设置标题
        response.setContent(ticket.getContent()); // 设置内容
        response.setType(ticket.getType()); // 设置类型
        response.setPriority(ticket.getPriority()); // 设置优先级
        response.setStatus(ticket.getStatus()); // 设置状态
        response.setImages(deserializeImages(ticket.getImages())); // 反序列化图片列表
        response.setUserId(ticket.getUserId()); // 设置用户 ID
        response.setAssigneeId(ticket.getAssigneeId()); // 设置分配给的管理员 ID
        response.setReplyContent(ticket.getReplyContent()); // 设置回复内容
        response.setReplyTime(ticket.getReplyTime()); // 设置回复时间
        response.setResolvedTime(ticket.getResolvedTime()); // 设置解决时间
        response.setRating(ticket.getRating()); // 设置评分
        response.setFeedback(ticket.getFeedback()); // 设置反馈内容
        response.setCreatedAt(ticket.getCreatedAt()); // 设置创建时间
        response.setUpdatedAt(ticket.getUpdatedAt()); // 设置更新时间

        // 获取用户名
        if (ticket.getUserId() != null) { // 检查用户 ID 是否为空
            User user = userMapper.selectById(ticket.getUserId()); // 查询用户信息
            if (user != null) { // 检查用户是否存在
                response.setUsername(user.getUsername()); // 设置用户名
            }
        }

        // 获取分配的管理员名
        if (ticket.getAssigneeId() != null) { // 检查分配的管理员 ID 是否为空
            User assignee = userMapper.selectById(ticket.getAssigneeId()); // 查询管理员用户信息
            if (assignee != null) { // 检查管理员是否存在
                response.setAssigneeName(assignee.getUsername()); // 设置管理员用户名
            }
        }

        return response; // 返回响应对象
    }

    /**
     * 转换实体为响应 DTO（包含消息列表）
     */
    private TicketResponse convertToResponseWithMessages(Ticket ticket) {
        TicketResponse response = convertToResponse(ticket); // 先转换基本信息

        // 获取消息列表
        List<TicketMessage> messages = ticketMessageMapper.findByTicketId(ticket.getId()); // 查询工单关联的消息列表
        response.setMessages(messages.stream() // 将消息列表转换为响应 DTO 列表
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList()));

        return response; // 返回包含消息列表的响应对象
    }

    /**
     * 转换消息实体为响应 DTO
     */
    private TicketMessageResponse convertToMessageResponse(TicketMessage message) {
        TicketMessageResponse response = new TicketMessageResponse(); // 创建消息响应对象
        response.setId(message.getId()); // 设置消息 ID
        response.setTicketId(message.getTicketId()); // 设置关联的工单 ID
        response.setSenderId(message.getSenderId()); // 设置发送者 ID
        response.setSenderType(message.getSenderType()); // 设置发送者类型
        response.setContent(message.getContent()); // 设置消息内容
        response.setImages(deserializeImages(message.getImages())); // 反序列化消息图片列表
        response.setCreatedAt(message.getCreatedAt()); // 设置创建时间

        // 获取发送者名称
        if (message.getSenderId() != null) { // 检查发送者 ID 是否为空
            User sender = userMapper.selectById(message.getSenderId()); // 查询发送者用户信息
            if (sender != null) { // 检查发送者是否存在
                response.setSenderName(sender.getUsername()); // 设置发送者用户名
            }
        }

        return response; // 返回消息响应对象
    }
}
