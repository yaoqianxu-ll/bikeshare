package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.BackgroundImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 背景图片 Mapper 接口
 */
@Mapper
public interface BackgroundImageMapper extends BaseMapper<BackgroundImage> {

    /**
     * 查询所有启用的背景图片
     */
    @Select("SELECT * FROM background_images WHERE enabled = 1 AND deleted = 0 ORDER BY sort ASC")
    List<BackgroundImage> findAllEnabled();
}
