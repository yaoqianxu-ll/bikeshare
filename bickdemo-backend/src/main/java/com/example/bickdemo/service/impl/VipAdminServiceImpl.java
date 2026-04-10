package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.vo.VipAdminDashboardVo;
import com.example.bickdemo.dto.VipAdminMemberAdjustDto;
import com.example.bickdemo.dto.VipAdminMemberDetailDto;
import com.example.bickdemo.dto.VipAdminMemberPageDto;
import com.example.bickdemo.dto.VipAdminOrderPageDto;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.VipMember;
import com.example.bickdemo.entity.VipMemberStatus;
import com.example.bickdemo.entity.VipOrder;
import com.example.bickdemo.entity.VipPlan;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.mapper.VipMemberMapper;
import com.example.bickdemo.mapper.VipOrderMapper;
import com.example.bickdemo.mapper.VipPlanMapper;
import com.example.bickdemo.service.VipAdminService;
import com.example.bickdemo.service.VipMemberService;
import com.example.bickdemo.service.VipPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VIP管理端服务实现
 *
 * @author BikeShare Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipAdminServiceImpl implements VipAdminService {

    private final VipMemberMapper vipMemberMapper;
    private final VipOrderMapper vipOrderMapper;
    private final VipPlanMapper vipPlanMapper;
    private final UserMapper userMapper;
    private final VipMemberService vipMemberService;
    private final VipPlanService vipPlanService;

    @Override
    public VipAdminDashboardVo getDashboard() {
        VipAdminDashboardVo vo = new VipAdminDashboardVo();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekLater = now.plusDays(7);

        // 活跃会员数
        Long activeCount = vipMemberMapper.selectCount(
                new LambdaQueryWrapper<VipMember>()
                        .eq(VipMember::getStatus, VipMemberStatus.ACTIVE.name())
        );
        vo.setActiveCount(activeCount);

        // 过期会员数
        Long expiredCount = vipMemberMapper.selectCount(
                new LambdaQueryWrapper<VipMember>()
                        .eq(VipMember::getStatus, VipMemberStatus.EXPIRED.name())
        );
        vo.setExpiredCount(expiredCount);

        // 即将到期会员数（7天内）
        Long expiringSoonCount = vipMemberMapper.selectCount(
                new LambdaQueryWrapper<VipMember>()
                        .eq(VipMember::getStatus, VipMemberStatus.ACTIVE.name())
                        .lt(VipMember::getExpireTime, weekLater)
                        .gt(VipMember::getExpireTime, now)
        );
        vo.setExpiringSoonCount(expiringSoonCount);

        // 本月订单数（已支付）
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        Long monthOrdersCount = vipOrderMapper.selectCount(
                new LambdaQueryWrapper<VipOrder>()
                        .eq(VipOrder::getStatus, "PAID")
                        .ge(VipOrder::getPaidAt, monthStart)
        );
        vo.setMonthOrdersCount(monthOrdersCount);

        // 本月收入
        List<VipOrder> monthOrders = vipOrderMapper.selectList(
                new LambdaQueryWrapper<VipOrder>()
                        .eq(VipOrder::getStatus, "PAID")
                        .ge(VipOrder::getPaidAt, monthStart)
        );
        BigDecimal monthRevenue = monthOrders.stream()
                .map(VipOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthRevenue(monthRevenue);

        return vo;
    }

    @Override
    public Page<VipAdminMemberDetailDto> pageMembers(VipAdminMemberPageDto dto) {
        // 构建用户查询条件 - 查询所有VIP用户（vip_level > 0）
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.gt(User::getVipLevel, 0);

        // 关键词搜索
        if (StringUtils.hasText(dto.getKeyword())) {
            boolean isNumeric = dto.getKeyword().matches("\\d+");
            if (isNumeric) {
                userQueryWrapper.eq(User::getId, Long.parseLong(dto.getKeyword()));
            } else {
                userQueryWrapper.like(User::getUsername, dto.getKeyword());
            }
        }

        // 状态筛选（基于到期时间）
        if (StringUtils.hasText(dto.getStatus())) {
            if ("ACTIVE".equals(dto.getStatus())) {
                // 生效中：未过期
                userQueryWrapper.isNotNull(User::getVipExpireTime)
                        .gt(User::getVipExpireTime, LocalDateTime.now());
            } else if ("EXPIRED".equals(dto.getStatus())) {
                // 已过期：已过期或到期时间为null
                userQueryWrapper.and(w -> w
                        .isNull(User::getVipExpireTime)
                        .or()
                        .le(User::getVipExpireTime, LocalDateTime.now())
                );
            }
        }

        // 到期时间范围筛选
        if (dto.getExpireTimeStart() != null) {
            userQueryWrapper.ge(User::getVipExpireTime, dto.getExpireTimeStart());
        }
        if (dto.getExpireTimeEnd() != null) {
            userQueryWrapper.le(User::getVipExpireTime, dto.getExpireTimeEnd());
        }

        // 获取符合条件的用户ID列表
        List<User> users = userMapper.selectList(userQueryWrapper);

        // 构建分页
        int total = users.size();
        int fromIndex = (dto.getPage() - 1) * dto.getSize();
        int toIndex = Math.min(fromIndex + dto.getSize(), total);

        List<User> pagedUsers = total > fromIndex
                ? users.subList(fromIndex, toIndex)
                : List.of();

        // 转换为DTO
        List<VipAdminMemberDetailDto> records = pagedUsers.stream().map(user -> {
            VipAdminMemberDetailDto detailDto = new VipAdminMemberDetailDto();
            detailDto.setUserId(user.getId());
            detailDto.setUsername(user.getUsername());
            detailDto.setVipLevel(user.getVipLevel());

            // 获取vip_member信息
            VipMember member = vipMemberService.getVipMemberByUserId(user.getId());
            if (member != null) {
                detailDto.setStatus(member.getStatus());
                detailDto.setStartTime(member.getStartTime());
                detailDto.setExpireTime(member.getExpireTime());
                detailDto.setLastOrderNo(member.getLastOrderNo());

                // 计算剩余天数
                if (member.getExpireTime() != null && VipMemberStatus.ACTIVE.name().equals(member.getStatus())) {
                    long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), member.getExpireTime());
                    detailDto.setRemainingDays(Math.max(0, remainingDays));
                } else {
                    detailDto.setRemainingDays(0L);
                }
            } else {
                // 用户表有VIP但vip_member没有记录
                detailDto.setStatus(VipMemberStatus.ACTIVE.name());
                detailDto.setExpireTime(user.getVipExpireTime());
                if (user.getVipExpireTime() != null && user.getVipExpireTime().isAfter(LocalDateTime.now())) {
                    long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), user.getVipExpireTime());
                    detailDto.setRemainingDays(Math.max(0, remainingDays));
                } else {
                    detailDto.setRemainingDays(0L);
                }
            }

            return detailDto;
        }).collect(Collectors.toList());

        Page<VipAdminMemberDetailDto> resultPage = new Page<>(dto.getPage(), dto.getSize());
        resultPage.setTotal(total);
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public VipAdminMemberDetailDto getMemberDetail(Long userId) {
        VipMember member = vipMemberService.getVipMemberByUserId(userId);
        if (member == null) {
            return null;
        }

        VipAdminMemberDetailDto dto = new VipAdminMemberDetailDto();
        dto.setUserId(member.getUserId());
        dto.setStatus(member.getStatus());
        dto.setStartTime(member.getStartTime());
        dto.setExpireTime(member.getExpireTime());
        dto.setLastOrderNo(member.getLastOrderNo());

        User user = userMapper.selectById(userId);
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setVipLevel(user.getVipLevel());
        }

        if (member.getExpireTime() != null && member.getStatus().equals(VipMemberStatus.ACTIVE.name())) {
            long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), member.getExpireTime());
            dto.setRemainingDays(Math.max(0, remainingDays));
        } else {
            dto.setRemainingDays(0L);
        }

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustMember(VipAdminMemberAdjustDto dto) {
        if (dto.getAction() == null) {
            throw new RuntimeException("调整动作不能为空");
        }

        switch (dto.getAction()) {
            case "ACTIVATE" -> {
                // 覆盖激活
                if (dto.getDays() == null || dto.getDays() <= 0) {
                    throw new RuntimeException("天数必须大于0");
                }
                vipMemberService.overwriteVip(dto.getUserId(), dto.getDays(), "ADMIN_" + System.currentTimeMillis());
            }
            case "EXTEND" -> {
                // 续期
                if (dto.getDays() == null || dto.getDays() <= 0) {
                    throw new RuntimeException("天数必须大于0");
                }
                // 续期实际上也是调用activateVip，内部会判断是续期还是新开通
                VipMember member = vipMemberService.getVipMemberByUserId(dto.getUserId());
                String orderNo = member != null ? member.getLastOrderNo() : null;
                vipMemberService.activateVip(dto.getUserId(), orderNo != null ? orderNo : "ADMIN_EXTEND", dto.getDays());
            }
            case "EXPIRE_NOW" -> {
                // 立即过期
                vipMemberService.expireVipImmediately(dto.getUserId());
            }
            default -> throw new RuntimeException("不支持的调整动作: " + dto.getAction());
        }
    }

    @Override
    public Page<?> pageOrders(VipAdminOrderPageDto dto) {
        Page<VipOrder> page = new Page<>(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<VipOrder> queryWrapper = new LambdaQueryWrapper<>();

        // 订单号
        if (StringUtils.hasText(dto.getOrderNo())) {
            queryWrapper.eq(VipOrder::getOrderNo, dto.getOrderNo());
        }

        // 套餐编码
        if (StringUtils.hasText(dto.getPlanCode())) {
            queryWrapper.eq(VipOrder::getPackageType, dto.getPlanCode());
        }

        // 状态
        if (StringUtils.hasText(dto.getStatus())) {
            queryWrapper.eq(VipOrder::getStatus, dto.getStatus());
        }

        // 时间范围
        if (dto.getCreateTimeStart() != null) {
            queryWrapper.ge(VipOrder::getCreatedAt, dto.getCreateTimeStart());
        }
        if (dto.getCreateTimeEnd() != null) {
            queryWrapper.le(VipOrder::getCreatedAt, dto.getCreateTimeEnd());
        }

        // 用户关键词
        if (StringUtils.hasText(dto.getUserKeyword())) {
            boolean isNumeric = dto.getUserKeyword().matches("\\d+");
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            if (isNumeric) {
                userQuery.eq(User::getId, Long.parseLong(dto.getUserKeyword()));
            }
            userQuery.or().like(User::getUsername, dto.getUserKeyword());
            List<User> users = userMapper.selectList(userQuery);
            if (users.isEmpty()) {
                Page<Object> resultPage = new Page<>(dto.getPage(), dto.getSize());
                resultPage.setTotal(0);
                resultPage.setRecords(List.of());
                return resultPage;
            }
            List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            queryWrapper.in(VipOrder::getUserId, userIds);
        }

        queryWrapper.orderByDesc(VipOrder::getCreatedAt);

        Page<VipOrder> orderPage = vipOrderMapper.selectPage(page, queryWrapper);

        // 转换为Map列表
        List<Map<String, Object>> records = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("userId", order.getUserId());
            map.put("packageType", order.getPackageType());
            map.put("amount", order.getAmount());
            map.put("status", order.getStatus());
            map.put("tradeNo", order.getTradeNo());
            map.put("paidAt", order.getPaidAt());
            map.put("createdAt", order.getCreatedAt());

            // 获取用户名
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                map.put("username", user.getUsername());
            }

            return map;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(dto.getPage(), dto.getSize());
        resultPage.setTotal(orderPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public List<Object> listPlans() {
        return vipPlanService.getAllPlans().stream().map(plan -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", plan.getId());
            map.put("code", plan.getCode());
            map.put("name", plan.getName());
            map.put("days", plan.getDays());
            map.put("priceFen", plan.getPriceFen());
            map.put("price", plan.getPriceFen() != null ? new BigDecimal(plan.getPriceFen()).divide(new BigDecimal(100)) : null);
            map.put("enabled", plan.getEnabled());
            map.put("description", plan.getDescription());
            return (Object) map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(Long planId, String name, Integer days, Integer priceFen, Boolean enabled, String description) {
        VipPlan plan = vipPlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("套餐不存在");
        }

        plan.setName(name);
        plan.setDays(days);
        plan.setPriceFen(priceFen);
        plan.setEnabled(enabled);
        plan.setDescription(description);

        vipPlanMapper.updateById(plan);
    }
}
