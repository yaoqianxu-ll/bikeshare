package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计数据响应 DTO
 * @author Administrator
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {
    /** 总租赁次数 */
    private Long totalRentals;
    /** 活跃租赁数（进行中） */
    private Long activeRentals;
    /** 可用自行车数 */
    private Long availableBicycles;
    /** 总自行车数 */
    private Long totalBicycles;
    /** 维修中车辆数 */
    private Long maintenanceBicycles;
    /** 不可用车辆数 */
    private Long disabledBicycles;
    /** 自行车类型统计 */
    private BicycleTypeStats[] typeStats;
    /** 热门自行车排行 */
    private PopularBicycle[] popularBicycles;

    /**
     * 自行车类型统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BicycleTypeStats {
        /** 类型名称 */
        private String type;
        /** 数量 */
        private Long count;
    }

    /**
     * 热门自行车信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularBicycle {
        /** 自行车 ID */
        private Long bicycleId;
        /** 自行车名称 */
        private String bicycleName;
        /** 租赁次数 */
        private Long rentalCount;
    }
}
