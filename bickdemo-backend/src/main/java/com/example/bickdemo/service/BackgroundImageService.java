package com.example.bickdemo.service;

import com.example.bickdemo.entity.BackgroundImage;
import com.example.bickdemo.mapper.BackgroundImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 背景图片服务类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackgroundImageService {

    private final BackgroundImageMapper backgroundImageMapper;

    /**
     * 获取所有启用的背景图片
     */
    public List<BackgroundImage> getAllEnabled() {
        return backgroundImageMapper.findAllEnabled();
    }

    /**
     * 获取所有背景图片（管理员）
     */
    public List<BackgroundImage> getAll() {
        return backgroundImageMapper.selectList(null);
    }

    /**
     * 根据 ID 获取背景图片
     */
    public BackgroundImage getById(Long id) {
        return backgroundImageMapper.selectById(id);
    }

    /**
     * 创建背景图片
     */
    @Transactional
    public BackgroundImage create(BackgroundImage image) {
        backgroundImageMapper.insert(image);
        return image;
    }

    /**
     * 更新背景图片
     */
    @Transactional
    public BackgroundImage update(Long id, BackgroundImage image) {
        BackgroundImage existing = backgroundImageMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("背景图片不存在：" + id);
        }

        if (image.getName() != null) {
            existing.setName(image.getName());
        }
        if (image.getImageUrl() != null) {
            existing.setImageUrl(image.getImageUrl());
        }
        if (image.getType() != null) {
            existing.setType(image.getType());
        }
        if (image.getEnabled() != null) {
            existing.setEnabled(image.getEnabled());
        }
        if (image.getSort() != null) {
            existing.setSort(image.getSort());
        }

        backgroundImageMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除背景图片
     */
    @Transactional
    public void delete(Long id) {
        backgroundImageMapper.deleteById(id);
    }

    /**
     * 设置启用的背景图片
     */
    @Transactional
    public void setEnabled(Long id, Boolean enabled) {
        // 先禁用所有图片
        List<BackgroundImage> all = backgroundImageMapper.selectList(null);
        for (BackgroundImage image : all) {
            if (image.getEnabled()) {
                image.setEnabled(false);
                backgroundImageMapper.updateById(image);
            }
        }

        // 启用指定的图片
        if (enabled) {
            BackgroundImage image = backgroundImageMapper.selectById(id);
            if (image != null) {
                image.setEnabled(true);
                backgroundImageMapper.updateById(image);
            }
        }
    }
}
