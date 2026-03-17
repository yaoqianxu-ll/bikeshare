package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.MarketplaceApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 个人出租申请 Mapper。
 */
@Mapper
public interface MarketplaceApplicationMapper extends BaseMapper<MarketplaceApplication> {
}
