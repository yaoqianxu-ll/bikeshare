package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.entity.BackgroundImage;
import com.example.bickdemo.mapper.BackgroundImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final CacheManager cacheManager;

    /**
     * 手动清除所有背景图相关缓存
     */
    private void evictAllBackgroundCaches() {
        try {
            if (cacheManager.getCache(CacheNames.BACKGROUND_ENABLED) != null) {
                cacheManager.getCache(CacheNames.BACKGROUND_ENABLED).clear();
            }
            if (cacheManager.getCache(CacheNames.BACKGROUND_SELECTABLE) != null) {
                cacheManager.getCache(CacheNames.BACKGROUND_SELECTABLE).clear();
            }
            if (cacheManager.getCache(CacheNames.BACKGROUND_ALL) != null) {
                cacheManager.getCache(CacheNames.BACKGROUND_ALL).clear();
            }
            log.info("背景图缓存已清除");
        } catch (Exception e) {
            log.warn("清除背景图缓存失败：{}", e.getMessage());
        }
    }

    /**
     * 获取当前启用的背景图列表。
     */
    @Cacheable(cacheNames = CacheNames.BACKGROUND_ENABLED)
    public List<BackgroundImage> getAllEnabled() {
        return backgroundImageMapper.findAllEnabled();
    }

    /**
     * 获取所有可供游客/普通用户自行选择的背景图。
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
     * 分页获取背景图列表，供管理员后台管理页面使用。
     */
    public IPage<BackgroundImage> getPage(int page, int size) {
        Page<BackgroundImage> pageObj = new Page<>(page, size);
        return backgroundImageMapper.selectPage(pageObj);
    }

    /**
     * 根据 ID 查询背景图详情。
     */
    public BackgroundImage getById(Long id) {
        return backgroundImageMapper.selectById(id);
    }

    /**
     * 新增背景图记录。
     */
    @Transactional
    public BackgroundImage create(BackgroundImage image) {
        backgroundImageMapper.insert(image);
        evictAllBackgroundCaches();
        return image;
    }

    /**
     * 更新背景图信息。
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
        evictAllBackgroundCaches();
        return existing;
    }

    /**
     * 删除背景图记录。
     */
    @Transactional
    public void delete(Long id) {
        backgroundImageMapper.deleteById(id);
        evictAllBackgroundCaches();
    }

    /**
     * 设置系统当前启用的背景图。
     */
    @Transactional
    public void setEnabled(Long id, Boolean enabled) {
        List<BackgroundImage> all = backgroundImageMapper.selectList(null);
        for (BackgroundImage image : all) {
            if (image.getEnabled()) {
                image.setEnabled(false);
                backgroundImageMapper.updateById(image);
            }
        }

        if (enabled) {
            BackgroundImage image = backgroundImageMapper.selectById(id);
            if (image != null) {
                image.setEnabled(true);
                backgroundImageMapper.updateById(image);
            }
        }
        evictAllBackgroundCaches();
    }
}
