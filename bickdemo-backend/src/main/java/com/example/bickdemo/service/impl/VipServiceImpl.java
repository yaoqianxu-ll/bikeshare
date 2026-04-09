package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.VipPurchaseRequest;
import com.example.bickdemo.dto.VipStatusResponse;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.VipBenefit;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.mapper.VipBenefitMapper;
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

    /** VIP套餐配置 */
    private static final int MONTHLY_DAYS = 30;
    private static final int QUARTERLY_DAYS = 90;
    private static final int YEARLY_DAYS = 365;

    private static final int POINTS_MONTHLY = 500;
    private static final int POINTS_QUARTERLY = 1200;
    private static final int POINTS_YEARLY = 4000;

    @Override
    @Cacheable(value = CacheNames.VIP_STATUS, key = "#userId")
    public VipStatusResponse getVipStatus(Long userId) {
        User user = userMapper.selectById(userId);
        VipStatusResponse response = new VipStatusResponse();

        boolean isVip = isVipUser(user);
        response.setVipLevel(isVip ? 1 : 0);
        response.setVipExpireTime(user != null ? user.getVipExpireTime() : null);
        response.setIsVip(isVip);

        // VIP专属权益 - VIP用户自动获得这三个权益
        response.setHasVisitorHidden(isVip);
        response.setHasBurnAfterRead(isVip);
        response.setHasSpecialCare(isVip);

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

        LocalDateTime newExpireTime = extendVipTime(user, days);
        user.setVipLevel(1);
        user.setVipExpireTime(newExpireTime);
        userMapper.updateById(user);
    }

    @Override
    @CacheEvict(value = CacheNames.VIP_STATUS, key = "#userId")
    @Transactional
    public void redeemVip(Long userId, String packageType) {
        int pointsCost = getPointsCostByPackageType(packageType);
        int days = getDaysByPackageType(packageType);

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
        LocalDateTime newExpireTime = extendVipTime(user, days);
        user.setVipLevel(1);
        user.setVipExpireTime(newExpireTime);
        userMapper.updateById(user);
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
    public void grantVip(Long userId, Integer days) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        LocalDateTime newExpireTime = extendVipTime(user, days);
        user.setVipLevel(1);
        user.setVipExpireTime(newExpireTime);
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
}