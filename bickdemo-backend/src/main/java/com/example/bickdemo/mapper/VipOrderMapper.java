package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.VipOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP订单Mapper接口
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 *
 * @author BikeShare Team
 */
@Mapper
public interface VipOrderMapper extends BaseMapper<VipOrder> {

    /**
     * 无需额外方法
     * BaseMapper已提供：
     * - insert(T entity) - 插入订单
     * - selectById(Long id) - 根据ID查询
     * - selectList(Wrapper queryWrapper) - 条件查询
     * - update(Wrapper updateWrapper) - 条件更新
     * - delete(Wrapper deleteWrapper) - 条件删除
     */
}
