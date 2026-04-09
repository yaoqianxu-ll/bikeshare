package com.example.bickdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.PointsRecordResponse;

public interface PointsService {

    /** 获取用户积分余额 */
    Integer getPoints(Long userId);

    /** 增加积分 */
    void addPoints(Long userId, Integer points, String reason, Long bizId);

    /** 扣除积分 */
    void subtractPoints(Long userId, Integer points, String reason, Long bizId);

    /** 管理端扣减积分 */
    void deductPoints(Long userId, Integer points, String reason);

    /** 分页获取积分记录 */
    Page<PointsRecordResponse> getPointsRecords(Long userId, int page, int size);

    /** 签到 */
    boolean signIn(Long userId);

    /** 检查用户今日是否已签到 */
    boolean hasSignedToday(Long userId);
}