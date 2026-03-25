package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.*;
import com.example.bickdemo.entity.*;
import com.example.bickdemo.mapper.TicketMapper;
import com.example.bickdemo.mapper.TicketMessageMapper;
import com.example.bickdemo.mapper.UserMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 工单服务类
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketMessageMapper ticketMessageMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    private static final Random RANDOM = new Random();

    /**
     * 生成工单编号：TK + timestamp + 4位随机数
     */
    private String generateTicketNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = RANDOM.nextInt(10000);
        return "TK" + timestamp + String.format("%04d", random);
    }

    /**
     * 序列化图片列表为 JSON 字符串
     */
    private String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            log.error("序列化图片列表失败", e);
            return null;
        }
    }

    /**
     * 反序列化 JSON 字符串为图片列表
     */
    private List<String> deserializeImages(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("反序列化图片列表失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }
        String username = authentication.getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在：" + username);
        }
        return user.getId();
    }

    /**
     * 创建工单
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.TICKETS_STATS, allEntries = true)
    public TicketResponse createTicket(TicketRequest request) {
        Long userId = getCurrentUserId();

        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setTitle(request.getTitle());
        ticket.setContent(request.getContent());
        ticket.setType(request.getType());
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.NORMAL);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setImages(serializeImages(request.getImages()));
        ticket.setUserId(userId);

        ticketMapper.insert(ticket);
        return convertToResponse(ticket);
    }

    /**
     * 获取当前用户的工单列表
     */
    public List<TicketResponse> getUserTickets() {
        Long userId = getCurrentUserId();
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getUserId, userId)
                .eq(Ticket::getDeleted, 0)
                .orderByDesc(Ticket::getCreatedAt);
        return ticketMapper.selectList(wrapper).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前用户的工单详情（包含消息列表）
     */
    public TicketResponse getUserTicketById(Long id) {
        Long userId = getCurrentUserId();
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在：" + id);
        }
        if (!ticket.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此工单");
        }
        return convertToResponseWithMessages(ticket);
    }

    /**
     * 用户发送消息
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.TICKETS_STATS, allEntries = true)
    public TicketMessageResponse addUserMessage(Long ticketId, TicketMessageRequest request) {
        Long userId = getCurrentUserId();
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在：" + ticketId);
        }
        if (!ticket.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此工单");
        }

        TicketMessage message = new TicketMessage();
        message.setTicketId(ticketId);
        message.setSenderId(userId);
        message.setSenderType(SenderType.USER);
        message.setContent(request.getContent());
        message.setImages(serializeImages(request.getImages()));

        ticketMessageMapper.insert(message);

        // 如果工单状态是 OPEN，改为 PROCESSING
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.PROCESSING);
            ticketMapper.updateById(ticket);
        }

        return convertToMessageResponse(message);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员获取所有工单（分页，支持筛选）
     */
    @Cacheable(cacheNames = CacheNames.TICKETS_PAGE,
               key = "'page:' + #p1 + ':size:' + #p2 + ':status:' + (#p0 != null ? #p0.name() : 'all') + ':type:' + (#p3 != null ? #p3.name() : 'all') + ':priority:' + (#p4 != null ? #p4.name() : 'all')")
    public Page<TicketResponse> getAdminTicketsPage(TicketStatus status, int page, int size,
                                                     TicketType type, TicketPriority priority,
                                                     String keyword) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getDeleted, 0)
                .eq(status != null, Ticket::getStatus, status)
                .eq(type != null, Ticket::getType, type)
                .eq(priority != null, Ticket::getPriority, priority)
                .orderByDesc(Ticket::getCreatedAt);

        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(Ticket::getTicketNo, keyword)
                    .or()
                    .like(Ticket::getTitle, keyword)
                    .or()
                    .inSql(Ticket::getUserId, "SELECT id FROM users WHERE username LIKE '%" + keyword + "%'")
            );
        }

        Page<Ticket> ticketPage = ticketMapper.selectPage(new Page<>(page, size), wrapper);
        Page<TicketResponse> result = new Page<>(ticketPage.getCurrent(), ticketPage.getSize());
        result.setTotal(ticketPage.getTotal());
        result.setRecords(ticketPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        return result;
    }

    /**
     * 管理员获取工单详情
     */
    public TicketResponse getAdminTicketById(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在：" + id);
        }
        return convertToResponseWithMessages(ticket);
    }

    /**
     * 管理员分配工单
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true)
    public TicketResponse assignTicket(Long id, Long assigneeId) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在：" + id);
        }

        User assignee = userMapper.selectById(assigneeId);
        if (assignee == null) {
            throw new RuntimeException("分配的客服不存在：" + assigneeId);
        }

        ticket.setAssigneeId(assigneeId);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }
        ticketMapper.updateById(ticket);
        return convertToResponse(ticket);
    }

    /**
     * 管理员标记工单为处理中
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true)
    public TicketResponse processTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在：" + id);
        }

        ticket.setStatus(TicketStatus.PROCESSING);
        ticketMapper.updateById(ticket);
        return convertToResponse(ticket);
    }

    /**
     * 管理员回复工单
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true)
    public TicketMessageResponse replyTicket(Long id, TicketReplyRequest request, Long adminId) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在：" + id);
        }

        // 添加管理员消息
        TicketMessage message = new TicketMessage();
        message.setTicketId(id);
        message.setSenderId(adminId);
        message.setSenderType(SenderType.ADMIN);
        message.setContent(request.getContent());
        ticketMessageMapper.insert(message);

        // 更新工单的回复信息
        ticket.setReplyContent(request.getContent());
        ticket.setReplyTime(LocalDateTime.now());
        ticket.setStatus(TicketStatus.PROCESSING);
        ticketMapper.updateById(ticket);

        return convertToMessageResponse(message);
    }

    /**
     * 管理员解决工单
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true)
    public TicketResponse resolveTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在：" + id);
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        return convertToResponse(ticket);
    }

    /**
     * 管理员关闭工单
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.TICKETS_STATS, CacheNames.TICKETS_PAGE}, allEntries = true)
    public TicketResponse closeTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在：" + id);
        }

        ticket.setStatus(TicketStatus.CLOSED);
        ticketMapper.updateById(ticket);
        return convertToResponse(ticket);
    }

    /**
     * 管理员获取工单统计
     */
    @Cacheable(cacheNames = CacheNames.TICKETS_STATS, key = "'stats'")
    public TicketStatsResponse getStats() {
        TicketStatsResponse stats = new TicketStatsResponse();
        stats.setPendingCount(ticketMapper.countByStatus(TicketStatus.OPEN));
        stats.setProcessingCount(ticketMapper.countByStatus(TicketStatus.PROCESSING));
        stats.setResolvedCount(ticketMapper.countResolved());
        stats.setClosedCount(ticketMapper.countClosed());
        stats.setTotalCount(ticketMapper.countAll());

        // 今日新增
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        stats.setTodayNewCount(ticketMapper.countNewSince(todayStart));

        // 本周新增
        LocalDateTime weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        stats.setWeekNewCount(ticketMapper.countNewSince(weekStart));

        // 本月新增
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        stats.setMonthNewCount(ticketMapper.countNewSince(monthStart));

        // 平均响应时间
        stats.setAvgResponseTime(ticketMapper.avgResponseTime());

        // 平均解决时间
        stats.setAvgResolveTime(ticketMapper.avgResolveTime());

        // 用户满意度（评分4星及以上的比例）
        Long totalRated = ticketMapper.countRated();
        Long highRating = ticketMapper.countHighRating();
        if (totalRated != null && totalRated > 0) {
            stats.setSatisfactionRate((double) highRating / totalRated * 100);
        } else {
            stats.setSatisfactionRate(0.0);
        }

        return stats;
    }

    /**
     * 转换实体为响应 DTO
     */
    private TicketResponse convertToResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setTicketNo(ticket.getTicketNo());
        response.setTitle(ticket.getTitle());
        response.setContent(ticket.getContent());
        response.setType(ticket.getType());
        response.setPriority(ticket.getPriority());
        response.setStatus(ticket.getStatus());
        response.setImages(deserializeImages(ticket.getImages()));
        response.setUserId(ticket.getUserId());
        response.setAssigneeId(ticket.getAssigneeId());
        response.setReplyContent(ticket.getReplyContent());
        response.setReplyTime(ticket.getReplyTime());
        response.setResolvedTime(ticket.getResolvedTime());
        response.setRating(ticket.getRating());
        response.setFeedback(ticket.getFeedback());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());

        // 获取用户名
        if (ticket.getUserId() != null) {
            User user = userMapper.selectById(ticket.getUserId());
            if (user != null) {
                response.setUsername(user.getUsername());
            }
        }

        // 获取分配的管理员名
        if (ticket.getAssigneeId() != null) {
            User assignee = userMapper.selectById(ticket.getAssigneeId());
            if (assignee != null) {
                response.setAssigneeName(assignee.getUsername());
            }
        }

        return response;
    }

    /**
     * 转换实体为响应 DTO（包含消息列表）
     */
    private TicketResponse convertToResponseWithMessages(Ticket ticket) {
        TicketResponse response = convertToResponse(ticket);

        // 获取消息列表
        List<TicketMessage> messages = ticketMessageMapper.findByTicketId(ticket.getId());
        response.setMessages(messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * 转换消息实体为响应 DTO
     */
    private TicketMessageResponse convertToMessageResponse(TicketMessage message) {
        TicketMessageResponse response = new TicketMessageResponse();
        response.setId(message.getId());
        response.setTicketId(message.getTicketId());
        response.setSenderId(message.getSenderId());
        response.setSenderType(message.getSenderType());
        response.setContent(message.getContent());
        response.setImages(deserializeImages(message.getImages()));
        response.setCreatedAt(message.getCreatedAt());

        // 获取发送者名称
        if (message.getSenderId() != null) {
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                response.setSenderName(sender.getUsername());
            }
        }

        return response;
    }
}
