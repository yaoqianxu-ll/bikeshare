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
public class IpBlacklistService {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final Duration DEFAULT_BAN_DURATION = Duration.ofHours(1);
    private static final String RATE_KEY_PREFIX = "security:blacklist:rate:";
    private static final String BAN_KEY_PREFIX = "security:blacklist:ban:";
    private static final String BAN_INDEX_KEY = "security:blacklist:index";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.key-prefix:bickdemo:}")
    private String redisKeyPrefix;

    public AccessDecision evaluateAccess(String ip) {
        cleanupExpiredIndex();
        BanMeta meta = getBanMeta(ip);
        if (meta != null) {
            return new AccessDecision(true, false, meta.getReason(), meta.getExpireAt());
        }

        String windowKey = buildRateKey(ip);
        Long currentCount = stringRedisTemplate.opsForValue().increment(windowKey);
        if (currentCount != null && currentCount == 1L) {
            stringRedisTemplate.expire(windowKey, RATE_WINDOW);
        }

        if (currentCount != null && currentCount > MAX_REQUESTS_PER_MINUTE) {
            BanMeta banMeta = banInternal(ip, "1 分钟内访问超过 30 次，已自动封禁 1 小时", DEFAULT_BAN_DURATION);
            return new AccessDecision(true, true, banMeta.getReason(), banMeta.getExpireAt());
        }

        return new AccessDecision(false, false, null, null);
    }

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

    public void banIp(String ip, String reason, Duration duration) {
        if (!StringUtils.hasText(ip)) {
            throw new RuntimeException("IP 不能为空");
        }
        banInternal(ip.trim(), reason, duration == null ? DEFAULT_BAN_DURATION : duration);
    }

    public void unbanIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return;
        }
        String normalizedIp = ip.trim();
        stringRedisTemplate.delete(buildBanKey(normalizedIp));
        stringRedisTemplate.opsForZSet().remove(indexKey(), normalizedIp);
    }

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
        stringRedisTemplate.opsForZSet().removeRangeByScore(indexKey(), 0, Instant.now().getEpochSecond());
    }

    private String buildRateKey(String ip) {
        long minuteBucket = Instant.now().getEpochSecond() / 60;
        return redisKeyPrefix + RATE_KEY_PREFIX + minuteBucket + ":" + ip;
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
