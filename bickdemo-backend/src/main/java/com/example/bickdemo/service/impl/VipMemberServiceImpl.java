package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.VipMember;
import com.example.bickdemo.entity.VipMemberStatus;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.mapper.VipMemberMapper;
import com.example.bickdemo.service.VipMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * VIP会员服务实现
 *
 * @author BikeShare Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipMemberServiceImpl extends ServiceImpl<VipMemberMapper, VipMember> implements VipMemberService {

    private final VipMemberMapper vipMemberMapper;
    private final UserMapper userMapper;

    @Override
    public VipMember getVipMemberByUserId(Long userId) {
        return vipMemberMapper.selectOne(
                new LambdaQueryWrapper<VipMember>()
                        .eq(VipMember::getUserId, userId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateVip(Long userId, String orderNo, Integer days) {
        LocalDateTime now = LocalDateTime.now();
        VipMember vipMember = getVipMemberByUserId(userId);

        Date startTime = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date expireTime = Date.from(now.plusDays(days).atZone(ZoneId.systemDefault()).toInstant());

        if (vipMember == null) {
            // 首次开通
            vipMember = new VipMember();
            vipMember.setUserId(userId);
            vipMember.setStatus(VipMemberStatus.ACTIVE.name());
            vipMember.setStartTime(now);
            vipMember.setExpireTime(now.plusDays(days));
            vipMember.setLastOrderNo(orderNo);
            vipMember.setCreateTime(now); // 显式设置创建时间
            vipMember.setUpdateTime(now);
            vipMemberMapper.insert(vipMember);
        } else {
            // 续期：判断当前是否有效，有效则从当前到期时间延长，无效则从当前时间开始
            Date currentExpireTime = vipMember.getExpireTime() != null
                    ? Date.from(vipMember.getExpireTime().atZone(ZoneId.systemDefault()).toInstant())
                    : null;

            if (currentExpireTime != null && currentExpireTime.after(new Date()) && VipMemberStatus.ACTIVE.name().equals(vipMember.getStatus())) {
                // 当前VIP有效，从当前到期时间延长
                expireTime = Date.from(vipMember.getExpireTime().plusDays(days).atZone(ZoneId.systemDefault()).toInstant());
            } else {
                // 当前VIP已过期或不存在，从当前时间开始
                startTime = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
                expireTime = Date.from(now.plusDays(days).atZone(ZoneId.systemDefault()).toInstant());
            }

            vipMember.setStatus(VipMemberStatus.ACTIVE.name());
            vipMember.setStartTime(now);
            vipMember.setExpireTime(now.plusDays(days));
            vipMember.setLastOrderNo(orderNo);
            vipMemberMapper.updateById(vipMember);
        }

        // 同步更新User表的VIP相关字段
        syncUserVipFields(userId, now.plusDays(days));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void overwriteVip(Long userId, Integer days, String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        VipMember vipMember = getVipMemberByUserId(userId);

        if (vipMember == null) {
            // 不存在则创建
            vipMember = new VipMember();
            vipMember.setUserId(userId);
            vipMember.setStatus(VipMemberStatus.ACTIVE.name());
            vipMember.setStartTime(now);
            vipMember.setExpireTime(now.plusDays(days));
            vipMember.setLastOrderNo(orderNo);
            vipMember.setCreateTime(now); // 显式设置创建时间
            vipMember.setUpdateTime(now);
            vipMemberMapper.insert(vipMember);
        } else {
            // 直接覆盖，从当前时间开始计算
            vipMember.setStatus(VipMemberStatus.ACTIVE.name());
            vipMember.setStartTime(now);
            vipMember.setExpireTime(now.plusDays(days));
            if (orderNo != null) {
                vipMember.setLastOrderNo(orderNo);
            }
            vipMember.setUpdateTime(now);
            vipMemberMapper.updateById(vipMember);
        }

        // 同步更新User表的VIP相关字段
        syncUserVipFields(userId, now.plusDays(days));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireVipImmediately(Long userId) {
        VipMember vipMember = getVipMemberByUserId(userId);
        if (vipMember == null) {
            return;
        }

        vipMember.setStatus(VipMemberStatus.EXPIRED.name());
        vipMember.setExpireTime(LocalDateTime.now());
        vipMemberMapper.updateById(vipMember);

        // 同步更新User表
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setVipLevel(0);
            user.setVipExpireTime(null);
            userMapper.updateById(user);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncExpiredVipRoles() {
        LocalDateTime now = LocalDateTime.now();

        // 查询所有状态为ACTIVE但已过期的会员
        List<VipMember> expiredMembers = vipMemberMapper.selectList(
                new LambdaQueryWrapper<VipMember>()
                        .eq(VipMember::getStatus, VipMemberStatus.ACTIVE.name())
                        .lt(VipMember::getExpireTime, now)
        );

        if (expiredMembers.isEmpty()) {
            return;
        }

        log.info("[VIP定时任务] 发现{}个过期会员需要同步", expiredMembers.size());

        // 批量更新状态为EXPIRED
        for (VipMember member : expiredMembers) {
            member.setStatus(VipMemberStatus.EXPIRED.name());
            vipMemberMapper.updateById(member);

            // 同步更新User表
            User user = userMapper.selectById(member.getUserId());
            if (user != null) {
                user.setVipLevel(0);
                user.setVipExpireTime(null);
                userMapper.updateById(user);
            }
        }

        log.info("[VIP定时任务] 已同步{}个过期会员", expiredMembers.size());
    }

    @Override
    public boolean hasVipAccess(Long userId) {
        VipMember vipMember = getVipMemberByUserId(userId);
        if (vipMember == null) {
            return false;
        }
        if (!VipMemberStatus.ACTIVE.name().equals(vipMember.getStatus())) {
            return false;
        }
        if (vipMember.getExpireTime() == null) {
            return false;
        }
        return vipMember.getExpireTime().isAfter(LocalDateTime.now());
    }

    @Override
    public LocalDateTime getVipExpireTime(Long userId) {
        VipMember vipMember = getVipMemberByUserId(userId);
        if (vipMember == null) {
            return null;
        }
        return vipMember.getExpireTime();
    }

    /**
     * 同步更新User表的VIP字段
     */
    private void syncUserVipFields(Long userId, LocalDateTime expireTime) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        // 根据到期时间计算VIP等级（沿用现有经验值系统）
        // 这里简化处理，实际可以按经验值计算
        int exp = user.getExperiencePoints() != null ? user.getExperiencePoints() : 0;
        int level = calculateVipLevel(exp);

        user.setVipLevel(level);
        user.setVipExpireTime(expireTime);
        userMapper.updateById(user);
    }

    /**
     * 根据经验值计算VIP等级
     */
    private int calculateVipLevel(int experiencePoints) {
        int[] thresholds = {0, 100, 300, 600, 1000, 1500};
        if (experiencePoints <= 0) return 0;
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (experiencePoints >= thresholds[i]) {
                return i + 1;
            }
        }
        return 0;
    }
}
