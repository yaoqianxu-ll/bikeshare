package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.MarketplaceListing;
import org.apache.ibatis.annotations.Mapper;

/**
 * 个人出租挂牌 Mapper。
 */
@Mapper
public interface MarketplaceListingMapper extends BaseMapper<MarketplaceListing> {
}
