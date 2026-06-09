package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.VipExchangeRecordResponse;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.VipExchangeRecord;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.mapper.VipExchangeRecordMapper;
import com.example.bickdemo.service.VipExchangeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * VIP积分兑换记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipExchangeRecordServiceImpl implements VipExchangeRecordService {

    private final VipExchangeRecordMapper recordMapper;
    private final UserMapper userMapper;

    @Override
    public VipExchangeRecord createRecord(Long userId, String packageType, String planName,
                                          Integer planDays, Integer pointsCost, Integer expGain) {
        VipExchangeRecord record = new VipExchangeRecord()
                .setExchangeNo(generateExchangeNo())
                .setUserId(userId)
                .setPackageType(packageType)
                .setPlanName(planName)
                .setPlanDays(planDays)
                .setPointsCost(pointsCost)
                .setExpGain(expGain)
                .setStatus("SUCCESS");
        recordMapper.insert(record);
        log.info("创建积分兑换记录: exchangeNo={}, userId={}, packageType={}, pointsCost={}",
                record.getExchangeNo(), userId, packageType, pointsCost);
        return record;
    }

    @Override
    public Page<VipExchangeRecord> getUserRecords(Long userId, int page, int size) {
        Page<VipExchangeRecord> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<VipExchangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VipExchangeRecord::getUserId, userId)
               .orderByDesc(VipExchangeRecord::getCreatedAt);
        return recordMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public Page<VipExchangeRecordResponse> adminPageRecords(int page, int size, String exchangeNo,
                                                            String userKeyword, String packageType, String status) {
        LambdaQueryWrapper<VipExchangeRecord> wrapper = new LambdaQueryWrapper<>();

        // 兑换单号模糊查询
        if (StringUtils.hasText(exchangeNo)) {
            wrapper.like(VipExchangeRecord::getExchangeNo, exchangeNo);
        }

        // 套餐类型筛选
        if (StringUtils.hasText(packageType)) {
            wrapper.eq(VipExchangeRecord::getPackageType, packageType);
        }

        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(VipExchangeRecord::getStatus, status);
        }

        // 用户关键词筛选
        if (StringUtils.hasText(userKeyword)) {
            boolean isNumeric = userKeyword.matches("\\d+");
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            if (isNumeric) {
                userQuery.eq(User::getId, Long.parseLong(userKeyword));
            }
            userQuery.or().like(User::getUsername, userKeyword);
            List<User> users = userMapper.selectList(userQuery);
            if (users.isEmpty()) {
                Page<VipExchangeRecordResponse> emptyPage = new Page<>(page, size);
                emptyPage.setTotal(0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            wrapper.in(VipExchangeRecord::getUserId, userIds);
        }

        wrapper.orderByDesc(VipExchangeRecord::getCreatedAt);

        Page<VipExchangeRecord> pageObj = new Page<>(page, size);
        Page<VipExchangeRecord> recordPage = recordMapper.selectPage(pageObj, wrapper);

        // 转换为Response（附加username，批量查询避免N+1）
        List<Long> userIds = recordPage.getRecords().stream()
                .map(VipExchangeRecord::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> usernameMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> usernameMap.put(u.getId(), u.getUsername()));
        }

        List<VipExchangeRecordResponse> records = recordPage.getRecords().stream().map(record -> {
            VipExchangeRecordResponse resp = new VipExchangeRecordResponse();
            resp.setId(record.getId());
            resp.setExchangeNo(record.getExchangeNo());
            resp.setUserId(record.getUserId());
            resp.setPackageType(record.getPackageType());
            resp.setPlanName(record.getPlanName());
            resp.setPlanDays(record.getPlanDays());
            resp.setPointsCost(record.getPointsCost());
            resp.setExpGain(record.getExpGain());
            resp.setStatus(record.getStatus());
            resp.setRemark(record.getRemark());
            resp.setCreatedAt(record.getCreatedAt());
            resp.setUsername(usernameMap.get(record.getUserId()));
            return resp;
        }).collect(Collectors.toList());

        Page<VipExchangeRecordResponse> resultPage = new Page<>(page, size);
        resultPage.setTotal(recordPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public void deleteRecord(Long id) {
        VipExchangeRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("兑换记录不存在");
        }
        recordMapper.deleteById(id);
        log.info("逻辑删除兑换记录: id={}, exchangeNo={}", id, record.getExchangeNo());
    }

    /**
     * 生成兑换单号：EXC + yyyyMMddHHmmss + 4位随机数
     */
    private String generateExchangeNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "EXC" + timestamp + random;
    }
}
