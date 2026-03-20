package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.IpBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * IP 黑名单 Mapper 接口
 */
@Mapper
public interface IpBlacklistMapper extends BaseMapper<IpBlacklist> {
}
