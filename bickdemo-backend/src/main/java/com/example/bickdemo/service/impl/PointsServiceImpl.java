package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.PointsRecordResponse;
import com.example.bickdemo.entity.PointsRecord;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.PointsRecordMapper;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private final UserMapper userMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 积分常量 */
    private static final int POINTS_RENTAL = 10;       // 租车
    private static final int POINTS_POST = 5;           // 发帖/回帖
    private static final int POINTS_ACTIVITY = 15;     // 参与活动
    private static final int POINTS_SIGNIN = 3;        // 签到

    @Override
    @Cacheable(value = CacheNames.POINTS_BALANCE, key = "#userId", unless = "#result == null")
    public Integer getPoints(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getPoints() != null ? user.getPoints() : 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.POINTS_BALANCE, key = "#userId")
    public void addPoints(Long userId, Integer points, String reason, Long bizId) {
        if (points == null || points <= 0) return;
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        int newPoints = (user.getPoints() == null ? 0 : user.getPoints()) + points;
        user.setPoints(newPoints);
        userMapper.updateById(user);

        // 记录积分变动
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("EARN");
        record.setPoints(points);
        record.setReason(reason);
        record.setBizId(bizId);
        pointsRecordMapper.insert(record);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.POINTS_BALANCE, key = "#userId")
    public void subtractPoints(Long userId, Integer points, String reason, Long bizId) {
        if (points == null || points <= 0) throw new RuntimeException("积分必须为正整数");
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
        if (currentPoints < points) {
            throw new RuntimeException("积分不足");
        }

        int newPoints = currentPoints - points;
        user.setPoints(newPoints);
        userMapper.updateById(user);

        // 记录积分变动
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("SPEND");
        record.setPoints(-points);
        record.setReason(reason);
        record.setBizId(bizId);
        pointsRecordMapper.insert(record);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.POINTS_BALANCE, key = "#userId")
    public void deductPoints(Long userId, Integer points, String reason) {
        if (points == null || points <= 0) throw new RuntimeException("积分必须为正整数");
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
        int newPoints = Math.max(0, currentPoints - points);
        user.setPoints(newPoints);
        userMapper.updateById(user);

        // 记录积分扣减
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("DEDUCT");
        record.setPoints(-points);
        record.setReason(reason);
        pointsRecordMapper.insert(record);
    }

    @Override
    public Page<PointsRecordResponse> getPointsRecords(Long userId, int page, int size) {
        Page<PointsRecord> recordPage = pointsRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getUserId, userId)
                        .orderByDesc(PointsRecord::getCreatedAt)
        );

        Page<PointsRecordResponse> responsePage = new Page<>(page, size, recordPage.getTotal());
        List<PointsRecordResponse> records = recordPage.getRecords().stream()
                .map(this::convertToResponse)
                .toList();
        responsePage.setRecords(records);
        return responsePage;
    }

    @Override
    @Transactional
    public boolean signIn(Long userId) {
        String signKey = String.format("signin:%d:%s", userId, LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        Boolean exists = redisTemplate.hasKey(signKey);
        if (Boolean.TRUE.equals(exists)) {
            return false; // 今日已签到
        }

        // 设置签到标记（24小时过期）
        redisTemplate.opsForValue().set(signKey, 1, 24, TimeUnit.HOURS);

        // 增加积分
        addPoints(userId, POINTS_SIGNIN, "每日签到", null);
        return true;
    }

    @Override
    public boolean hasSignedToday(Long userId) {
        String signKey = String.format("signin:%d:%s", userId, LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        return Boolean.TRUE.equals(redisTemplate.hasKey(signKey));
    }

    private PointsRecordResponse convertToResponse(PointsRecord record) {
        PointsRecordResponse response = new PointsRecordResponse();
        response.setId(record.getId());
        response.setType(record.getType());
        response.setPoints(record.getPoints());
        response.setReason(record.getReason());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }
}