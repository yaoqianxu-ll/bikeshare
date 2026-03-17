package com.example.bickdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.BlacklistEntryResponse;
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
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * IP 黑名单与访问频控服务。
 * 通过 Redis 记录访问频率和封禁信息，既支持自动限流封禁，也支持后台手动封禁/解封。
 */
public class IpBlacklistService {

    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final String RATE_KEY_PREFIX = "security:blacklist:rate:";
    private static final String BAN_KEY_PREFIX = "security:blacklist:ban:";
    private static final String BAN_INDEX_KEY = "security:blacklist:index";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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
     * 黑名单主体数据存 Redis，分页在内存里完成，因为数据规模通常较小。
     */
    public Page<BlacklistEntryResponse> getEntries(int page, int size, String keyword) {
        cleanupExpiredIndex();
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(indexKey(), 0, -1);
        if (tuples == null || tuples.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        List<BlacklistEntryResponse> rows = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String ip = tuple.getValue();
            if (!StringUtils.hasText(ip)) {
                continue;
            }
            BanMeta meta = getBanMeta(ip);
            if (meta == null) {
                stringRedisTemplate.opsForZSet().remove(indexKey(), ip);
                continue;
            }
            BlacklistEntryResponse row = toResponse(meta);
            if (matchesKeyword(row, keyword)) {
                rows.add(row);
            }
        }

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, rows.size());
        int toIndex = Math.min(fromIndex + safeSize, rows.size());
        List<BlacklistEntryResponse> pageRows = fromIndex >= toIndex ? Collections.emptyList() : rows.subList(fromIndex, toIndex);

        Page<BlacklistEntryResponse> result = new Page<>(safePage, safeSize, rows.size());
        result.setRecords(pageRows);
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
    }

    /**
     * 统计当前仍有效的封禁数量。
     */
    public long countActiveBans() {
        cleanupExpiredIndex();
        Long count = stringRedisTemplate.opsForZSet().zCard(indexKey());
        return count == null ? 0L : count;
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
        return meta;
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

    private boolean matchesKeyword(BlacklistEntryResponse row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim();
        return row.getIp().contains(normalized)
                || (row.getAddress() != null && row.getAddress().contains(normalized))
                || (row.getReason() != null && row.getReason().contains(normalized));
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
