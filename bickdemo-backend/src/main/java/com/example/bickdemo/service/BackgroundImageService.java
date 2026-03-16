package com.example.bickdemo.service;

import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.entity.BackgroundImage;
import com.example.bickdemo.mapper.BackgroundImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 背景图管理服务。
 * 负责前台背景图展示、管理员背景图维护，以及相关缓存的读写与失效控制。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackgroundImageService {

    private final BackgroundImageMapper backgroundImageMapper;

    /**
     * 获取当前启用的背景图列表。
     * 面向首页、登录页等直接展示场景，结果会缓存起来减少数据库压力。
     */
    @Cacheable(cacheNames = CacheNames.BACKGROUND_ENABLED)
    public List<BackgroundImage> getAllEnabled() {
        return backgroundImageMapper.findAllEnabled();
    }

    /**
     * 获取所有可供游客/普通用户自行选择的背景图。
     * 这类选择是前端本地偏好，不会改变系统全局启用状态。
     */
    @Cacheable(cacheNames = CacheNames.BACKGROUND_SELECTABLE)
    public List<BackgroundImage> getAllSelectable() {
        return backgroundImageMapper.findAllSelectable();
    }

    /**
     * 获取全部背景图，供管理员后台管理页面使用。
     */
    @Cacheable(cacheNames = CacheNames.BACKGROUND_ALL)
    public List<BackgroundImage> getAll() {
        return backgroundImageMapper.selectList(null);
    }

    /**
     * 根据 ID 查询背景图详情。
     */
    public BackgroundImage getById(Long id) {
        return backgroundImageMapper.selectById(id);
    }

    /**
     * 新增背景图记录。
     * 写操作后会同时清理三个背景图缓存，确保前台和后台读取到的是最新数据。
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ENABLED, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_SELECTABLE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ALL, allEntries = true)
    })
    public BackgroundImage create(BackgroundImage image) {
        backgroundImageMapper.insert(image);
        return image;
    }

    /**
     * 更新背景图信息。
     * 仅覆盖显式传入的字段，未提供的字段保持不变。
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ENABLED, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_SELECTABLE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ALL, allEntries = true)
    })
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
     * 删除背景图记录。
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ENABLED, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_SELECTABLE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ALL, allEntries = true)
    })
    public void delete(Long id) {
        backgroundImageMapper.deleteById(id);
    }

    /**
     * 设置系统当前启用的背景图。
     * 业务规则是全局同一时刻只允许一张背景图处于 enabled=true。
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ENABLED, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_SELECTABLE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ALL, allEntries = true)
    })
    public void setEnabled(Long id, Boolean enabled) {
        // 先批量关闭所有启用项，保证全局启用背景图始终只有一个。
        List<BackgroundImage> all = backgroundImageMapper.selectList(null);
        for (BackgroundImage image : all) {
            if (image.getEnabled()) {
                image.setEnabled(false);
                backgroundImageMapper.updateById(image);
            }
        }

        // 再按需启用目标背景图；如果 enabled=false，则效果就是“全部关闭”。
        if (enabled) {
            BackgroundImage image = backgroundImageMapper.selectById(id);
            if (image != null) {
                image.setEnabled(true);
                backgroundImageMapper.updateById(image);
            }
        }
    }
}
