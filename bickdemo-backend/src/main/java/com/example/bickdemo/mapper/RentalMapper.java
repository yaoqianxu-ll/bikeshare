package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.Rental;
import com.example.bickdemo.entity.RentalStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租赁记录 Mapper 接口
 * 用于租赁数据的数据库访问
 * @author Administrator
 */
@Mapper
public interface RentalMapper extends BaseMapper<Rental> {

    /**
     * 根据用户 ID 查询租赁记录
     */
    @Select("SELECT * FROM rentals WHERE user_id = #{userId} AND deleted = 0")
    List<Rental> findByUserId(@Param("userId") Long userId);

    /**
     * 根据状态查询租赁记录
     */
    @Select("SELECT * FROM rentals WHERE status = #{status} AND deleted = 0")
    List<Rental> findByStatus(@Param("status") RentalStatus status);

    /**
     * 统计进行中租赁中的车辆数量
     */
    @Select("""
        SELECT COALESCE(SUM(quantity), 0)
        FROM rentals
        WHERE status = #{status}
          AND deleted = 0
    """)
    Long sumQuantityByStatus(@Param("status") RentalStatus status);

    /**
     * 查询用户的租赁历史
     */
    @Select("SELECT * FROM rentals WHERE user_id = #{userId} AND status = #{status} AND deleted = 0 ORDER BY start_time DESC")
    List<Rental> findUserRentalHistory(@Param("userId") Long userId, @Param("status") RentalStatus status);

    /**
     * 查询最受欢迎的自行车 TOP10
     */
    @Select("""
        SELECT r.bicycle_id, b.name as bicycleName, COALESCE(SUM(r.quantity),0) as rentalCount
        FROM rentals r
        LEFT JOIN bicycles b ON r.bicycle_id = b.id
        WHERE r.deleted = 0
        GROUP BY r.bicycle_id, b.name
        ORDER BY rentalCount DESC
        LIMIT 10
    """)
    List<PopularBicycleVO> findMostPopularBicycles();

    /**
     * 统计指定时间段内的租赁次数
     */
    @Select("SELECT COUNT(*) FROM rentals WHERE start_time BETWEEN #{start} AND #{end} AND deleted = 0")
    Long countRentalsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计进行中租赁按类型分布的数量
     */
    @Select("""
        SELECT b.type AS type, COALESCE(SUM(r.quantity), 0) AS count
        FROM rentals r
        INNER JOIN bicycles b ON r.bicycle_id = b.id
        WHERE r.status = #{status}
          AND r.deleted = 0
          AND b.deleted = 0
        GROUP BY b.type
    """)
    List<ActiveTypeCountVO> sumQuantityByTypeForStatus(@Param("status") RentalStatus status);

    /**
     * 热门自行车 VO
     */
    class PopularBicycleVO {
        private Long bicycleId;
        private String bicycleName;
        private Long rentalCount;
        public Long getBicycleId() { return bicycleId; }
        public void setBicycleId(Long bicycleId) { this.bicycleId = bicycleId; }
        public String getBicycleName() { return bicycleName; }
        public void setBicycleName(String bicycleName) { this.bicycleName = bicycleName; }
        public Long getRentalCount() { return rentalCount; }
        public void setRentalCount(Long rentalCount) { this.rentalCount = rentalCount; }
    }

    class ActiveTypeCountVO {
        private BicycleType type;
        private Long count;
        public BicycleType getType() { return type; }
        public void setType(BicycleType type) { this.type = type; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }
}
