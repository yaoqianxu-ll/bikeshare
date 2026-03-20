package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.BlacklistEntryResponse;
import com.example.bickdemo.entity.IpBlacklist;
import com.example.bickdemo.mapper.IpBlacklistMapper;
import com.example.bickdemo.util.IpAddressUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * IP 黑名单与访问频控服务。
 * 通过 Redis 记录访问频率和封禁信息，既支持自动限流封禁，也支持后台手动封禁/解封。
 * 同时使用数据库持久化存储黑名单记录，Redis 作为高速缓存层。
 */
public class IpBlacklistService {

    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final String RATE_KEY_PREFIX = "security:blacklist:rate:";
    private static final String BAN_KEY_PREFIX = "security:blacklist:ban:";
    private static final String BAN_INDEX_KEY = "security:blacklist:index";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final IpBlacklistMapper ipBlacklistMapper;

    @Value("${app.redis.key-prefix:bickdemo:}")
    private String redisKeyPrefix;

    @Value("${app.security.ip-control.guest-max-requests-per-minute:120}")
    private int guestMaxRequestsPerMinute;

    @Value("${app.security.ip-control.authenticated-max-requests-per-minute:240}")
    private int authenticatedMaxRequestsPerMinute;

    @Value("${app.security.ip-control.ban-duration-minutes:15}")
    private long banDurationMinutes;

    /**
     * 判断某个 IP 是否允许继续访问。
     * 若一分钟内请求数超限，会自动加入黑名单并返回阻断结果。
     */
    public AccessDecision evaluateAccess(String ip, boolean authenticated) {
        cleanupExpiredIndex();
        BanMeta meta = getBanMeta(ip);
        if (meta != null) {
            return new AccessDecision(true, false, meta.getReason(), meta.getExpireAt());
        }

        // 访客和已登录用户分开限流，避免前后端分离页面的并发请求把正常用户误伤。
        int requestLimit = Math.max(authenticated ? authenticatedMaxRequestsPerMinute : guestMaxRequestsPerMinute, 1);
        Duration banDuration = Duration.ofMinutes(Math.max(banDurationMinutes, 1L));
        String windowKey = buildRateKey(ip, authenticated);
        Long currentCount = stringRedisTemplate.opsForValue().increment(windowKey);
        if (currentCount != null && currentCount == 1L) {
            stringRedisTemplate.expire(windowKey, RATE_WINDOW);
        }

        if (currentCount != null && currentCount > requestLimit) {
            // 访问频率超过阈值后立即封禁，防止恶意刷接口继续打到业务层。
            BanMeta banMeta = banInternal(
                    ip,
                    buildAutoBanReason(requestLimit, banDuration, authenticated),
                    banDuration
            );
            return new AccessDecision(true, true, banMeta.getReason(), banMeta.getExpireAt());
        }

        return new AccessDecision(false, false, null, null);
    }

    /**
     * 分页查询当前黑名单记录。
     * 优先从数据库读取，Redis 作为缓存层自动过期。
     * 包含 ACTIVE 和 EXPIRED 状态的记录，但只显示最近 7 天内到期的记录。
     */
    public Page<BlacklistEntryResponse> getEntries(int page, int size, String keyword) {
        cleanupExpiredIndex();

        // 构建数据库查询条件
        LambdaQueryWrapper<IpBlacklist> wrapper = new LambdaQueryWrapper<IpBlacklist>()
                .eq(IpBlacklist::getDeleted, 0)
                .orderByDesc(IpBlacklist::getCreatedAt)
                .orderByDesc(IpBlacklist::getId);

        // 如果有搜索关键字，添加模糊查询条件
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(q -> q
                    .like(IpBlacklist::getIp, normalizedKeyword)
                    .or()
                    .like(IpBlacklist::getAddress, normalizedKeyword)
                    .or()
                    .like(IpBlacklist::getReason, normalizedKeyword)
            );
        }

        Page<IpBlacklist> dbPage = ipBlacklistMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为响应对象
        List<BlacklistEntryResponse> rows = dbPage.getRecords().stream()
                .map(this::toResponseFromDb)
                .toList();

        Page<BlacklistEntryResponse> result = new Page<>(page, size, dbPage.getTotal());
        result.setRecords(rows);
        return result;
    }

    /**
     * 手动封禁指定 IP。
     */
    public void banIp(String ip, String reason, Duration duration) {
        if (!StringUtils.hasText(ip)) {
            throw new RuntimeException("IP 不能为空");
        }
        banInternal(ip.trim(), reason, duration == null ? Duration.ofMinutes(Math.max(banDurationMinutes, 1L)) : duration);
    }

    /**
     * 解除指定 IP 的封禁。
     */
    public void unbanIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return;
        }
        String normalizedIp = ip.trim();
        stringRedisTemplate.delete(buildBanKey(normalizedIp));
        stringRedisTemplate.opsForZSet().remove(indexKey(), normalizedIp);

        // 同步更新数据库
        removeFromDatabase(normalizedIp);
    }

    /**
     * 从数据库移除黑名单记录（逻辑删除）
     */
    @Transactional
    public void removeFromDatabase(String ip) {
        IpBlacklist existing = ipBlacklistMapper.selectOne(new LambdaQueryWrapper<IpBlacklist>()
                .eq(IpBlacklist::getIp, ip)
                .eq(IpBlacklist::getDeleted, 0)
                .gt(IpBlacklist::getExpireAt, LocalDateTime.now())
                .last("LIMIT 1"));

        if (existing != null) {
            existing.setStatus("EXPIRED");
            existing.setExpireAt(LocalDateTime.now());
            ipBlacklistMapper.updateById(existing);
            log.info("解除黑名单数据库记录：ip={}", ip);
        }
    }

    /**
     * 统计当前仍有效的封禁数量（用于后台总览）。
     * 只统计状态为 ACTIVE 且未过期的记录。
     */
    public long countActiveBans() {
        Long count = ipBlacklistMapper.selectCount(new LambdaQueryWrapper<IpBlacklist>()
                .eq(IpBlacklist::getDeleted, 0)
                .eq(IpBlacklist::getStatus, "ACTIVE")
                .gt(IpBlacklist::getExpireAt, LocalDateTime.now()));
        return count != null ? count : 0L;
    }

    private BanMeta banInternal(String ip, String reason, Duration duration) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plus(duration);
        BanMeta meta = new BanMeta(
                ip,
                IpAddressUtils.resolveAddress(ip),
                StringUtils.hasText(reason) ? reason.trim() : "访问频率异常，已被临时封禁",
                now,
                expireAt
        );
        try {
            // 黑名单详情单独存成 JSON，方便后台展示封禁原因、创建时间和到期时间。
            stringRedisTemplate.opsForValue().set(
                    buildBanKey(ip),
                    objectMapper.writeValueAsString(meta),
                    duration
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("写入黑名单缓存失败");
        }
        stringRedisTemplate.opsForZSet().add(indexKey(), ip, toEpochSecond(expireAt));

        // 同步写入数据库
        saveToDatabase(meta, duration);

        return meta;
    }

    /**
     * 将黑名单记录保存到数据库
     */
    @Transactional
    public void saveToDatabase(BanMeta meta, Duration duration) {
        // 先检查是否已存在该 IP 的有效记录
        IpBlacklist existing = ipBlacklistMapper.selectOne(new LambdaQueryWrapper<IpBlacklist>()
                .eq(IpBlacklist::getIp, meta.getIp())
                .eq(IpBlacklist::getDeleted, 0)
                .gt(IpBlacklist::getExpireAt, LocalDateTime.now())
                .last("LIMIT 1"));

        if (existing != null) {
            // 更新现有记录
            existing.setReason(meta.getReason());
            existing.setStatus("ACTIVE");
            existing.setCreatedAt(meta.getCreatedAt());
            existing.setExpireAt(meta.getExpireAt());
            ipBlacklistMapper.updateById(existing);
            log.info("更新黑名单数据库记录：ip={}", meta.getIp());
        } else {
            // 插入新记录
            IpBlacklist record = new IpBlacklist();
            record.setIp(meta.getIp());
            record.setAddress(meta.getAddress());
            record.setReason(meta.getReason());
            record.setStatus("ACTIVE");
            record.setCreatedAt(meta.getCreatedAt());
            record.setExpireAt(meta.getExpireAt());
            ipBlacklistMapper.insert(record);
            log.info("新增黑名单数据库记录：ip={}", meta.getIp());
        }
    }

    private BanMeta getBanMeta(String ip) {
        String raw = stringRedisTemplate.opsForValue().get(buildBanKey(ip));
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, BanMeta.class);
        } catch (JsonProcessingException e) {
            // 反序列化失败通常说明脏数据或结构升级，直接清掉避免持续报错。
            log.warn("Failed to parse blacklist metadata for ip {}", ip, e);
            stringRedisTemplate.delete(buildBanKey(ip));
            stringRedisTemplate.opsForZSet().remove(indexKey(), ip);
            return null;
        }
    }

    private BlacklistEntryResponse toResponse(BanMeta meta) {
        long remainingSeconds = Math.max(Duration.between(LocalDateTime.now(), meta.getExpireAt()).getSeconds(), 0L);
        return new BlacklistEntryResponse(
                meta.getIp(),
                meta.getAddress(),
                meta.getReason(),
                remainingSeconds > 0 ? "ACTIVE" : "EXPIRED",
                meta.getCreatedAt(),
                meta.getExpireAt(),
                remainingSeconds
        );
    }

    /**
     * 从数据库实体转换为响应对象
     */
    private BlacklistEntryResponse toResponseFromDb(IpBlacklist record) {
        long remainingSeconds = Math.max(Duration.between(LocalDateTime.now(), record.getExpireAt()).getSeconds(), 0L);
        return new BlacklistEntryResponse(
                record.getIp(),
                record.getAddress(),
                record.getReason(),
                remainingSeconds > 0 ? "ACTIVE" : "EXPIRED",
                record.getCreatedAt(),
                record.getExpireAt(),
                remainingSeconds
        );
    }

    private void cleanupExpiredIndex() {
        // 有效期到了就把索引移除，避免后台列表一直显示过期封禁。
        stringRedisTemplate.opsForZSet().removeRangeByScore(indexKey(), 0, Instant.now().getEpochSecond());
    }

    private String buildRateKey(String ip, boolean authenticated) {
        // 限流 key 带上分钟桶和身份类型，跨分钟自动归零，也不会把游客和登录用户混算。
        long minuteBucket = Instant.now().getEpochSecond() / 60;
        return redisKeyPrefix + RATE_KEY_PREFIX + minuteBucket + ":" + (authenticated ? "auth" : "guest") + ":" + ip;
    }

    private String buildAutoBanReason(int requestLimit, Duration banDuration, boolean authenticated) {
        long minutes = Math.max(banDuration.toMinutes(), 1L);
        String subject = authenticated ? "当前登录用户所在 IP" : "当前 IP";
        return subject + " 1 分钟内访问超过 " + requestLimit + " 次，已自动封禁 " + minutes + " 分钟";
    }

    private String buildBanKey(String ip) {
        return redisKeyPrefix + BAN_KEY_PREFIX + ip;
    }

    private String indexKey() {
        return redisKeyPrefix + BAN_INDEX_KEY;
    }

    private double toEpochSecond(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    public record AccessDecision(boolean blocked, boolean newlyBlocked, String reason, LocalDateTime expireAt) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class BanMeta {
        private String ip;
        private String address;
        private String reason;
        private LocalDateTime createdAt;
        private LocalDateTime expireAt;
    }
}
