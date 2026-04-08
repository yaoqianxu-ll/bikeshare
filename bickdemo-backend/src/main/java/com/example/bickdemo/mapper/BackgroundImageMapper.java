package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 查询所有可选择的背景图片（不区分 enabled，仅过滤 deleted）
     */
    @Select("SELECT * FROM background_images WHERE deleted = 0 ORDER BY sort ASC")
    List<BackgroundImage> findAllSelectable();

    /**
     * 分页查询所有背景图片（包含已禁用的）
     */
    @Select("SELECT * FROM background_images WHERE deleted = 0 ORDER BY sort ASC")
    IPage<BackgroundImage> selectPage(Page<BackgroundImage> page);
}
