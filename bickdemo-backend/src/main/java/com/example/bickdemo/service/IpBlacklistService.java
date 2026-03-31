package com.example.bickdemo.service;

// 引入 MyBatis-Plus 的 Lambda 查询包装器，用于构建类型安全的查询条件
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 引入 MyBatis-Plus 的分页插件，用于分页查询
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// 引入黑名单条目响应 DTO，用于返回给前端的黑名单数据
import com.example.bickdemo.dto.BlacklistEntryResponse;
// 引入 IP 黑名单实体类，对应数据库中的 ip_blacklist 表
import com.example.bickdemo.entity.IpBlacklist;
// 引入 IP 黑名单 Mapper，用于数据库操作
import com.example.bickdemo.mapper.IpBlacklistMapper;
// 引入 IP 地址工具类，用于通过 IP 获取地理地址
import com.example.bickdemo.util.IpAddressUtils;
// 引入 Jackson 的 JSON 处理异常类
import com.fasterxml.jackson.core.JsonProcessingException;
// 引入 Jackson 的对象映射器，用于 JSON 序列化/反序列化
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入 Lombok 注解，生成所有字段的构造函数
import lombok.AllArgsConstructor;
// 引入 Lombok 注解，生成 getter/setter 方法
import lombok.Data;
// 引入 Lombok 注解，生成无参构造函数
import lombok.NoArgsConstructor;
// 引入 Lombok 注解，生成必需参数构造函数（final 字段）
import lombok.RequiredArgsConstructor;
// 引入 Lombok 注解，生成 SLF4J 日志对象
import lombok.extern.slf4j.Slf4j;
// 引入 Spring 注解，用于读取配置文件的值
import org.springframework.beans.factory.annotation.Value;
// 引入 Redis 模板类，用于 Redis 操作
import org.springframework.data.redis.core.StringRedisTemplate;
// 引入 Redis ZSet 操作类，用于有序集合操作
import org.springframework.data.redis.core.ZSetOperations;
// 引入 Spring 注解，标识这是一个服务类
import org.springframework.stereotype.Service;
// 引入 Spring 事务注解，用于事务管理
import org.springframework.transaction.annotation.Transactional;
// 引入 Spring 字符串工具类
import org.springframework.util.StringUtils;

// 引入 Java 时间相关类
import java.time.Duration;       // 用于表示时间 duration
import java.time.Instant;        // 用于表示时间戳
import java.time.LocalDateTime;  // 用于表示本地日期时间
import java.time.ZoneId;         // 用于表示时区
import java.util.ArrayList;      // 引入 ArrayList（虽然未直接使用）
import java.util.List;           // 引入 List 接口
import java.util.Set;            // 引入 Set 接口

// Lombok 生成 SLF4J 日志对象，简化日志记录
@Slf4j
// Spring 注解，标识这是一个服务类，由 Spring 容器管理
@Service
// Lombok 注解，为所有 final 字段生成构造函数，实现依赖注入
@RequiredArgsConstructor
/**
 * IP 黑名单与访问频控服务。
 * 通过 Redis 记录访问频率和封禁信息，既支持自动限流封禁，也支持后台手动封禁/解封。
 * 同时使用数据库持久化存储黑名单记录，Redis 作为高速缓存层。
 */
public class IpBlacklistService {

    // 限流时间窗口长度，设置为 1 分钟
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    // Redis 中限流计数器 key 的前缀
    private static final String RATE_KEY_PREFIX = "security:blacklist:rate:";
    // Redis 中封禁详情 key 的前缀
    private static final String BAN_KEY_PREFIX = "security:blacklist:ban:";
    // Redis 中封禁 IP 索引 key，用于按过期时间排序的有序集合
    private static final String BAN_INDEX_KEY = "security:blacklist:index";

    // Redis 操作模板，由 Spring 通过构造函数注入
    private final StringRedisTemplate stringRedisTemplate;
    // JSON 对象映射器，用于序列化/反序列化黑名单元数据
    private final ObjectMapper objectMapper;
    // IP 黑名单数据库 Mapper，用于数据库操作
    private final IpBlacklistMapper ipBlacklistMapper;
    // 管理端通知发布器
    private final AdminNotificationPublisher adminNotificationPublisher;

    // Redis key 前缀，从配置文件读取，默认值为 "bickdemo:"
    @Value("${app.redis.key-prefix:bickdemo:}")
    private String redisKeyPrefix;

    // 未登录用户每分钟最大请求数，从配置文件读取，默认值为 120
    @Value("${app.security.ip-control.guest-max-requests-per-minute:120}")
    private int guestMaxRequestsPerMinute;

    // 已登录用户每分钟最大请求数，从配置文件读取，默认值为 240
    @Value("${app.security.ip-control.authenticated-max-requests-per-minute:240}")
    private int authenticatedMaxRequestsPerMinute;

    // 封禁持续时间（分钟），从配置文件读取，默认值为 15
    @Value("${app.security.ip-control.ban-duration-minutes:15}")
    private long banDurationMinutes;

    /**
     * 判断某个 IP 是否允许继续访问。
     * 若一分钟内请求数超限，会自动加入黑名单并返回阻断结果。
     */
    public AccessDecision evaluateAccess(String ip, boolean authenticated) {
        // 清理已过期的索引，避免后台列表一直显示过期封禁
        cleanupExpiredIndex();
        // 获取该 IP 的封禁元数据
        BanMeta meta = getBanMeta(ip);
        // 如果该 IP 已被封禁，返回阻断决策
        if (meta != null) {
            return new AccessDecision(true, false, meta.getReason(), meta.getExpireAt());
        }

        // 访客和已登录用户分开限流，避免前后端分离页面的并发请求把正常用户误伤。
        // 根据是否认证选择对应的请求限制值，确保至少为 1
        int requestLimit = Math.max(authenticated ? authenticatedMaxRequestsPerMinute : guestMaxRequestsPerMinute, 1);
        // 确保封禁时长至少为 1 分钟
        Duration banDuration = Duration.ofMinutes(Math.max(banDurationMinutes, 1L));
        // 构建限流 Redis key，包含分钟桶和身份类型
        String windowKey = buildRateKey(ip, authenticated);
        // 增加该 IP 在当前时间窗口内的请求计数
        Long currentCount = stringRedisTemplate.opsForValue().increment(windowKey);
        // 如果是第一次请求，设置该 key 的过期时间为一个时间窗口
        if (currentCount != null && currentCount == 1L) {
            stringRedisTemplate.expire(windowKey, RATE_WINDOW);
        }

        // 如果请求计数超过限制，则该 IP 被封禁
        if (currentCount != null && currentCount > requestLimit) {
            // 访问频率超过阈值后立即封禁，防止恶意刷接口继续打到业务层。
            // 创建自动封禁的元数据
            BanMeta banMeta = banInternal(
                    ip,
                    buildAutoBanReason(requestLimit, banDuration, authenticated),
                    banDuration
            );
            // 返回阻断决策，blocked=true 表示被阻断，newlyBlocked=true 表示是新封禁
            return new AccessDecision(true, true, banMeta.getReason(), banMeta.getExpireAt());
        }

        // 请求未超限，允许访问
        return new AccessDecision(false, false, null, null);
    }

    /**
     * 分页查询当前黑名单记录。
     * 优先从数据库读取，Redis 作为缓存层自动过期。
     * 包含 ACTIVE 和 EXPIRED 状态的记录，但只显示最近 7 天内到期的记录。
     */
    public Page<BlacklistEntryResponse> getEntries(int page, int size, String keyword) {
        // 清理已过期的索引
        cleanupExpiredIndex();

        // 构建数据库查询条件
        LambdaQueryWrapper<IpBlacklist> wrapper = new LambdaQueryWrapper<IpBlacklist>()
                // 只查询未删除的记录
                .eq(IpBlacklist::getDeleted, 0)
                // 按创建时间倒序排序
                .orderByDesc(IpBlacklist::getCreatedAt)
                // 按 ID 倒序排序
                .orderByDesc(IpBlacklist::getId);

        // 如果有搜索关键字，添加模糊查询条件
        if (StringUtils.hasText(keyword)) {
            // 去除关键字首尾空格
            String normalizedKeyword = keyword.trim();
            // 使用 AND 条件组合模糊查询
            wrapper.and(q -> q
                    // 模糊匹配 IP 地址
                    .like(IpBlacklist::getIp, normalizedKeyword)
                    // 或者模糊匹配地理地址
                    .or()
                    .like(IpBlacklist::getAddress, normalizedKeyword)
                    // 或者模糊匹配封禁原因
                    .or()
                    .like(IpBlacklist::getReason, normalizedKeyword)
            );
        }

        // 执行分页查询
        Page<IpBlacklist> dbPage = ipBlacklistMapper.selectPage(new Page<>(page, size), wrapper);

        // 将数据库实体转换为响应对象列表
        List<BlacklistEntryResponse> rows = dbPage.getRecords().stream()
                // 将每个 IpBlacklist 实体转换为 BlacklistEntryResponse
                .map(this::toResponseFromDb)
                // 转换为 List
                .toList();

        // 构建分页响应对象
        Page<BlacklistEntryResponse> result = new Page<>(page, size, dbPage.getTotal());
        // 设置查询结果列表
        result.setRecords(rows);
        // 返回分页结果
        return result;
    }

    /**
     * 手动封禁指定 IP。
     */
    public void banIp(String ip, String reason, Duration duration) {
        // 校验 IP 地址不能为空
        if (!StringUtils.hasText(ip)) {
            throw new RuntimeException("IP 不能为空");
        }
        // 调用内部封禁方法，如果 duration 为 null 则使用默认封禁时长
        banInternal(ip.trim(), reason, duration == null ? Duration.ofMinutes(Math.max(banDurationMinutes, 1L)) : duration);
        // 发送管理端通知
        adminNotificationPublisher.notifyBlacklistAdded(ip.trim(), reason);
    }

    /**
     * 解除指定 IP 的封禁。
     */
    public void unbanIp(String ip) {
        // 校验 IP 地址不能为空，如果为空则直接返回
        if (!StringUtils.hasText(ip)) {
            return;
        }
        // 规范化 IP 地址，去除首尾空格
        String normalizedIp = ip.trim();
        // 从 Redis 中删除该 IP 的封禁详情
        stringRedisTemplate.delete(buildBanKey(normalizedIp));
        // 从 Redis 有序集合中移除该 IP 的索引
        stringRedisTemplate.opsForZSet().remove(indexKey(), normalizedIp);

        // 同步更新数据库，将状态标记为过期
        removeFromDatabase(normalizedIp);

        // 发送管理端通知
        adminNotificationPublisher.notifyBlacklistRemoved(normalizedIp);
    }

    /**
     * 从数据库移除黑名单记录（逻辑删除）
     */
    @Transactional
    public void removeFromDatabase(String ip) {
        // 查询该 IP 的有效黑名单记录
        IpBlacklist existing = ipBlacklistMapper.selectOne(new LambdaQueryWrapper<IpBlacklist>()
                // 匹配 IP 地址
                .eq(IpBlacklist::getIp, ip)
                // 只查询未删除的记录
                .eq(IpBlacklist::getDeleted, 0)
                // 只查询未过期的记录
                .gt(IpBlacklist::getExpireAt, LocalDateTime.now())
                // 限制查询 1 条
                .last("LIMIT 1"));

        // 如果存在有效记录，则更新为过期状态
        if (existing != null) {
            // 设置状态为已过期
            existing.setStatus("EXPIRED");
            // 设置过期时间为当前时间
            existing.setExpireAt(LocalDateTime.now());
            // 更新数据库记录
            ipBlacklistMapper.updateById(existing);
            // 记录日志
            log.info("解除黑名单数据库记录：ip={}", ip);
        }
    }

    /**
     * 统计当前仍有效的封禁数量（用于后台总览）。
     * 只统计状态为 ACTIVE 且未过期的记录。
     */
    public long countActiveBans() {
        // 查询状态为 ACTIVE 且未过期的记录数量
        Long count = ipBlacklistMapper.selectCount(new LambdaQueryWrapper<IpBlacklist>()
                // 只查询未删除的记录
                .eq(IpBlacklist::getDeleted, 0)
                // 匹配状态为 ACTIVE
                .eq(IpBlacklist::getStatus, "ACTIVE")
                // 只查询未过期的记录
                .gt(IpBlacklist::getExpireAt, LocalDateTime.now()));
        // 如果 count 为 null 返回 0，否则返回 count 值
        return count != null ? count : 0L;
    }

    // 内部封禁方法，执行实际的封禁逻辑
    private BanMeta banInternal(String ip, String reason, Duration duration) {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 计算过期时间
        LocalDateTime expireAt = now.plus(duration);
        // 创建封禁元数据对象
        BanMeta meta = new BanMeta(
                ip,                                          // IP 地址
                IpAddressUtils.resolveAddress(ip),          // 通过 IP 获取地理地址
                StringUtils.hasText(reason) ? reason.trim() : "访问频率异常，已被临时封禁",  // 封禁原因
                now,                                         // 创建时间
                expireAt                                     // 过期时间
        );
        try {
            // 黑名单详情单独存成 JSON，方便后台展示封禁原因、创建时间和到期时间。
            // 将元数据对象序列化为 JSON 字符串并存储到 Redis
            stringRedisTemplate.opsForValue().set(
                    buildBanKey(ip),                         // Redis key
                    objectMapper.writeValueAsString(meta),   // JSON 字符串
                    duration                                 // 过期时间
            );
        } catch (JsonProcessingException e) {
            // 如果序列化失败，抛出运行时异常
            throw new RuntimeException("写入黑名单缓存失败");
        }
        // 将 IP 添加到有序集合索引中，分数为过期时间戳，用于按时间清理过期数据
        stringRedisTemplate.opsForZSet().add(indexKey(), ip, toEpochSecond(expireAt));

        // 同步写入数据库，持久化黑名单记录
        saveToDatabase(meta, duration);

        // 返回封禁元数据
        return meta;
    }

    /**
     * 将黑名单记录保存到数据库
     */
    @Transactional
    public void saveToDatabase(BanMeta meta, Duration duration) {
        // 先检查是否已存在该 IP 的有效记录
        IpBlacklist existing = ipBlacklistMapper.selectOne(new LambdaQueryWrapper<IpBlacklist>()
                // 匹配 IP 地址
                .eq(IpBlacklist::getIp, meta.getIp())
                // 只查询未删除的记录
                .eq(IpBlacklist::getDeleted, 0)
                // 只查询未过期的记录
                .gt(IpBlacklist::getExpireAt, LocalDateTime.now())
                // 限制查询 1 条
                .last("LIMIT 1"));

        // 如果已存在该 IP 的有效记录，则更新
        if (existing != null) {
            // 更新封禁原因
            existing.setReason(meta.getReason());
            // 更新状态为活跃
            existing.setStatus("ACTIVE");
            // 更新创建时间
            existing.setCreatedAt(meta.getCreatedAt());
            // 更新过期时间
            existing.setExpireAt(meta.getExpireAt());
            // 执行更新操作
            ipBlacklistMapper.updateById(existing);
            // 记录日志
            log.info("更新黑名单数据库记录：ip={}", meta.getIp());
        } else {
            // 如果不存在，则插入新记录
            // 创建新的黑名单实体
            IpBlacklist record = new IpBlacklist();
            // 设置 IP 地址
            record.setIp(meta.getIp());
            // 设置地理地址
            record.setAddress(meta.getAddress());
            // 设置封禁原因
            record.setReason(meta.getReason());
            // 设置状态为活跃
            record.setStatus("ACTIVE");
            // 设置创建时间
            record.setCreatedAt(meta.getCreatedAt());
            // 设置过期时间
            record.setExpireAt(meta.getExpireAt());
            // 插入数据库
            ipBlacklistMapper.insert(record);
            // 记录日志
            log.info("新增黑名单数据库记录：ip={}", meta.getIp());
        }
    }

    // 从 Redis 获取指定 IP 的封禁元数据
    private BanMeta getBanMeta(String ip) {
        // 构建 Redis key 并获取存储的 JSON 字符串
        String raw = stringRedisTemplate.opsForValue().get(buildBanKey(ip));
        // 如果为空，说明该 IP 未被封禁或已过期
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            // 将 JSON 字符串反序列化为 BanMeta 对象
            return objectMapper.readValue(raw, BanMeta.class);
        } catch (JsonProcessingException e) {
            // 反序列化失败通常说明脏数据或结构升级，直接清掉避免持续报错。
            // 记录警告日志
            log.warn("Failed to parse blacklist metadata for ip {}", ip, e);
            // 删除无效的 key
            stringRedisTemplate.delete(buildBanKey(ip));
            // 从有序集合中移除该 IP
            stringRedisTemplate.opsForZSet().remove(indexKey(), ip);
            // 返回 null
            return null;
        }
    }

    // 将 BanMeta 转换为响应对象（用于 Redis 数据）
    private BlacklistEntryResponse toResponse(BanMeta meta) {
        // 计算剩余有效时间（秒）
        long remainingSeconds = Math.max(Duration.between(LocalDateTime.now(), meta.getExpireAt()).getSeconds(), 0L);
        // 构建并返回响应对象
        return new BlacklistEntryResponse(
                meta.getIp(),                      // IP 地址
                meta.getAddress(),                  // 地理地址
                meta.getReason(),                   // 封禁原因
                remainingSeconds > 0 ? "ACTIVE" : "EXPIRED",  // 根据剩余时间判断状态
                meta.getCreatedAt(),                // 创建时间
                meta.getExpireAt(),                // 过期时间
                remainingSeconds                   // 剩余有效时间（秒）
        );
    }

    /**
     * 从数据库实体转换为响应对象
     */
    private BlacklistEntryResponse toResponseFromDb(IpBlacklist record) {
        // 计算剩余有效时间（秒）
        long remainingSeconds = Math.max(Duration.between(LocalDateTime.now(), record.getExpireAt()).getSeconds(), 0L);
        // 构建并返回响应对象
        return new BlacklistEntryResponse(
                record.getIp(),                    // IP 地址
                record.getAddress(),                // 地理地址
                record.getReason(),                 // 封禁原因
                remainingSeconds > 0 ? "ACTIVE" : "EXPIRED",  // 根据剩余时间判断状态
                record.getCreatedAt(),              // 创建时间
                record.getExpireAt(),              // 过期时间
                remainingSeconds                   // 剩余有效时间（秒）
        );
    }

    // 清理已过期的索引，避免后台列表一直显示过期封禁
    private void cleanupExpiredIndex() {
        // 移除有序集合中分数（过期时间戳）小于当前时间的元素
        stringRedisTemplate.opsForZSet().removeRangeByScore(indexKey(), 0, Instant.now().getEpochSecond());
    }

    // 构建限流 Redis key，包含分钟桶和身份类型，跨分钟自动归零
    private String buildRateKey(String ip, boolean authenticated) {
        // 计算当前分钟桶（以分钟为单位的整数时间戳）
        long minuteBucket = Instant.now().getEpochSecond() / 60;
        // 构建 key 格式：前缀 + rate: + 分钟桶 + : + auth/guest + : + IP
        return redisKeyPrefix + RATE_KEY_PREFIX + minuteBucket + ":" + (authenticated ? "auth" : "guest") + ":" + ip;
    }

    // 构建自动封禁的原因描述
    private String buildAutoBanReason(int requestLimit, Duration banDuration, boolean authenticated) {
        // 计算封禁时长（分钟），确保至少为 1
        long minutes = Math.max(banDuration.toMinutes(), 1L);
        // 根据是否认证选择不同的描述
        String subject = authenticated ? "当前登录用户所在 IP" : "当前 IP";
        // 返回封禁原因描述
        return subject + " 1 分钟内访问超过 " + requestLimit + " 次，已自动封禁 " + minutes + " 分钟";
    }

    // 构建封禁详情的 Redis key
    private String buildBanKey(String ip) {
        return redisKeyPrefix + BAN_KEY_PREFIX + ip;
    }

    // 获取封禁索引的 Redis key
    private String indexKey() {
        return redisKeyPrefix + BAN_INDEX_KEY;
    }

    // 将 LocalDateTime 转换为 epoch 秒数（用于 Redis 有序集合的分数）
    private double toEpochSecond(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    // 访问决策记录类，用于返回访问评估结果
    public record AccessDecision(boolean blocked, boolean newlyBlocked, String reason, LocalDateTime expireAt) {
    }

    // BanMeta 内部类，用于存储封禁元数据
    @Data                      // Lombok 注解，生成 getter/setter
    @NoArgsConstructor         // Lombok 注解，生成无参构造函数
    @AllArgsConstructor        // Lombok 注解，生成全参构造函数
    private static class BanMeta {
        private String ip;           // IP 地址
        private String address;       // 地理地址
        private String reason;        // 封禁原因
        private LocalDateTime createdAt;  // 创建时间
        private LocalDateTime expireAt;  // 过期时间
    }
}
