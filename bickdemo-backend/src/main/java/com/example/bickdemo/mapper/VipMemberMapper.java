package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.VipMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP会员Mapper
 *
 * @author BikeShare Team
 */
@Mapper
public interface VipMemberMapper extends BaseMapper<VipMember> {
}
