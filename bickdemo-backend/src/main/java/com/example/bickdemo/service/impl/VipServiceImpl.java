package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.VipPurchaseRequest;
import com.example.bickdemo.dto.VipStatusResponse;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.VipBenefit;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.mapper.VipBenefitMapper;
import com.example.bickdemo.service.AdminNotificationPublisher;
import com.example.bickdemo.service.PointsService;
import com.example.bickdemo.service.VipService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VipServiceImpl implements VipService {

    private final UserMapper userMapper;
    private final VipBenefitMapper vipBenefitMapper;
    private final PointsService pointsService;
    private final AdminNotificationPublisher adminNotificationPublisher;

    /** VIP套餐配置 */
    private static final int MONTHLY_DAYS = 30;
    private static final int QUARTERLY_DAYS = 90;
    private static final int YEARLY_DAYS = 365;

    private static final int POINTS_MONTHLY = 500;
    private static final int POINTS_QUARTERLY = 1200;
    private static final int POINTS_YEARLY = 4000;

    /** VIP经验值常量 - 充值获得的经验 */
    private static final int EXP_MONTHLY = 50;
    private static final int EXP_QUARTERLY = 150;
    private static final int EXP_YEARLY = 500;

    /** VIP等级经验阈值 */
    private static final int[] VIP_LEVEL_THRESHOLDS = {0, 100, 300, 600, 1000, 1500};
    private static final int MAX_VIP_LEVEL = 6;

    @Override
    @Cacheable(value = CacheNames.VIP_STATUS, key = "#userId")
    public VipStatusResponse getVipStatus(Long userId) {
        User user = userMapper.selectById(userId);
        VipStatusResponse response = new VipStatusResponse();

        int exp = user.getExperiencePoints() != null ? user.getExperiencePoints() : 0;
        int level = calculateVipLevel(exp);

        response.setVipLevel(level);
        response.setIsVip(level > 0);
        response.setExperiencePoints(exp);
        response.setCurrentLevel(level);

        if (level >= MAX_VIP_LEVEL) {
            response.setNextLevelExp(null);
            response.setExperienceToNext(0);
        } else {
            int nextExp = VIP_LEVEL_THRESHOLDS[level];
            response.setNextLevelExp(nextExp);
            response.setExperienceToNext(nextExp - exp);
        }

        // VIP权益 - VIP1及以上都有
        response.setHasVisitorHidden(level > 0);
        response.setHasBurnAfterRead(level > 0);
        response.setHasSpecialCare(level > 0);

        return response;
    }

    @Override
    @CacheEvict(value = CacheNames.VIP_STATUS, key = "#userId")
    @Transactional
    public void purchaseVip(Long userId, VipPurchaseRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int days = getDaysByPackageType(request.getPackageType());
        if (days <= 0) {
            throw new RuntimeException("无效的套餐类型");
        }

        // 获得经验值
        int expGain = getExpByPackageType(request.getPackageType());
        int newExp = (user.getExperiencePoints() != null ? user.getExperiencePoints() : 0) + expGain;
        newExp = Math.min(newExp, VIP_LEVEL_THRESHOLDS[MAX_VIP_LEVEL - 1]);

        LocalDateTime newExpireTime = extendVipTime(user, days);
        user.setVipLevel(calculateVipLevel(newExp));
        user.setVipExpireTime(newExpireTime);
        user.setExperiencePoints(newExp);
        userMapper.updateById(user);

        // 发送管理员通知
        adminNotificationPublisher.notifyVipPurchased(userId, user.getUsername(), request.getPackageType(), "购买");
    }

    @Override
    @CacheEvict(value = CacheNames.VIP_STATUS, key = "#userId")
    @Transactional
    public void redeemVip(Long userId, String packageType) {
        int pointsCost = getPointsCostByPackageType(packageType);
        int days = getDaysByPackageType(packageType);
        int expGain = getExpByPackageType(packageType);

        if (pointsCost <= 0 || days <= 0) {
            throw new RuntimeException("无效的套餐类型");
        }

        // 扣除积分
        pointsService.subtractPoints(userId, pointsCost, "兑换" + getPackageName(packageType), null);

        // 发放VIP
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        int newExp = (user.getExperiencePoints() != null ? user.getExperiencePoints() : 0) + expGain;
        newExp = Math.min(newExp, VIP_LEVEL_THRESHOLDS[MAX_VIP_LEVEL - 1]);

        LocalDateTime newExpireTime = extendVipTime(user, days);
        user.setVipLevel(calculateVipLevel(newExp));
        user.setVipExpireTime(newExpireTime);
        user.setExperiencePoints(newExp);
        userMapper.updateById(user);

        // 发送管理员通知
        adminNotificationPublisher.notifyVipPurchased(userId, user.getUsername(), packageType, "兑换");
    }

    @Override
    public boolean hasVipBenefit(Long userId, String benefitKey) {
        User user = userMapper.selectById(userId);
        if (!isVipUser(user)) return false;

        // 检查权益是否启用
        VipBenefit benefit = vipBenefitMapper.selectOne(
                new LambdaQueryWrapper<VipBenefit>()
                        .eq(VipBenefit::getBenefitKey, benefitKey)
                        .eq(VipBenefit::getIsActive, true)
        );
        return benefit != null;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.VIP_STATUS, key = "#userId")
    public void grantVip(Long userId, Integer days, Integer experience) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int expGain = (experience != null) ? experience : 0;
        int newExp = (user.getExperiencePoints() != null ? user.getExperiencePoints() : 0) + expGain;
        newExp = Math.min(newExp, VIP_LEVEL_THRESHOLDS[MAX_VIP_LEVEL - 1]);

        LocalDateTime newExpireTime = extendVipTime(user, days);
        user.setVipLevel(calculateVipLevel(newExp));
        user.setVipExpireTime(newExpireTime);
        user.setExperiencePoints(newExp);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.VIP_STATUS, key = "#userId")
    public void revokeVip(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setVipLevel(0);
        user.setVipExpireTime(null);
        // 注意：revoke 不清除经验值，保留等级进度
        userMapper.updateById(user);
    }

    @Override
    public Object getAllBenefits() {
        return vipBenefitMapper.selectList(
                new LambdaQueryWrapper<VipBenefit>()
                        .eq(VipBenefit::getIsActive, true)
        );
    }

    private boolean isVipUser(User user) {
        if (user == null || user.getVipLevel() == null || user.getVipLevel() == 0) {
            return false;
        }
        LocalDateTime expireTime = user.getVipExpireTime();
        return expireTime == null || expireTime.isAfter(LocalDateTime.now());
    }

    private LocalDateTime extendVipTime(User user, int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpire = user.getVipExpireTime();

        if (currentExpire == null || currentExpire.isBefore(now)) {
            return now.plusDays(days);
        }
        return currentExpire.plusDays(days);
    }

    private int getDaysByPackageType(String packageType) {
        if (packageType == null) return 0;
        return switch (packageType) {
            case "MONTHLY" -> MONTHLY_DAYS;
            case "QUARTERLY" -> QUARTERLY_DAYS;
            case "YEARLY" -> YEARLY_DAYS;
            default -> 0;
        };
    }

    private int getPointsCostByPackageType(String packageType) {
        if (packageType == null) return 0;
        return switch (packageType) {
            case "MONTHLY" -> POINTS_MONTHLY;
            case "QUARTERLY" -> POINTS_QUARTERLY;
            case "YEARLY" -> POINTS_YEARLY;
            default -> 0;
        };
    }

    private String getPackageName(String packageType) {
        if (packageType == null) return "未知套餐";
        return switch (packageType) {
            case "MONTHLY" -> "月卡";
            case "QUARTERLY" -> "季卡";
            case "YEARLY" -> "年卡";
            default -> "未知套餐";
        };
    }

    /**
     * 根据经验值计算VIP等级
     * 阈值：0=无VIP, 1-99=VIP1, 100-299=VIP2, 300-599=VIP3, 600-999=VIP4, 1000-1499=VIP5, 1500+=VIP6
     */
    private int calculateVipLevel(int experiencePoints) {
        if (experiencePoints <= 0) return 0;
        for (int i = VIP_LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (experiencePoints >= VIP_LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * 获取指定套餐的经验值
     */
    private int getExpByPackageType(String packageType) {
        if (packageType == null) return 0;
        return switch (packageType) {
            case "MONTHLY" -> EXP_MONTHLY;
            case "QUARTERLY" -> EXP_QUARTERLY;
            case "YEARLY" -> EXP_YEARLY;
            default -> 0;
        };
    }
}