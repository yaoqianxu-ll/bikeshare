package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.Bicycle;
import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 自行车 Mapper 接口
 * 用于自行车数据的数据库访问
 * @author Administrator
 */
@Mapper
public interface BicycleMapper extends BaseMapper<Bicycle> {

    /**
     * 根据状态查询自行车
     */
    @Select("SELECT * FROM bicycles WHERE status = #{status} AND deleted = 0")
    List<Bicycle> findByStatus(@Param("status") BicycleStatus status);

    /**
     * 根据类型查询自行车
     */
    @Select("SELECT * FROM bicycles WHERE type = #{type} AND deleted = 0")
    List<Bicycle> findByType(@Param("type") BicycleType type);

    /**
     * 根据状态和类型查询自行车
     */
    @Select("SELECT * FROM bicycles WHERE status = #{status} AND type = #{type} AND deleted = 0")
    List<Bicycle> findByStatusAndType(@Param("status") BicycleStatus status, @Param("type") BicycleType type);

    /**
     * 根据状态和位置模糊查询自行车
     */
    @Select("SELECT * FROM bicycles WHERE status = #{status} AND location LIKE CONCAT('%', #{location}, '%') AND deleted = 0")
    List<Bicycle> findByStatusAndLocationContaining(@Param("status") BicycleStatus status, @Param("location") String location);

    /**
     * 统计各类型自行车数量
     */
    @Select("SELECT type, COUNT(*) as count FROM bicycles WHERE deleted = 0 GROUP BY type")
    List<TypeCountVO> countByType();

    /**
     * 类型统计 VO
     */
    class TypeCountVO {
        private BicycleType type;
        private Long count;
        public BicycleType getType() { return type; }
        public void setType(BicycleType type) { this.type = type; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }
}
