package com.example.bickdemo.service;

// 引入缓存名称常量，用于标识不同的缓存空间
import com.example.bickdemo.config.CacheNames;
// 引入背景图实体类，对应数据库中的背景图表
import com.example.bickdemo.entity.BackgroundImage;
// 引入背景图Mapper，提供数据库访问方法
import com.example.bickdemo.mapper.BackgroundImageMapper;
// Lombok注解：生成带有required参数构造函数
import lombok.RequiredArgsConstructor;
// Lombok注解：生成日志logger对象
import lombok.extern.slf4j.Slf4j;
// Spring缓存注解：用于删除缓存
import org.springframework.cache.annotation.CacheEvict;
// Spring缓存注解：用于缓存方法返回值
import org.springframework.cache.annotation.Cacheable;
// Spring缓存注解：组合多个缓存操作
import org.springframework.cache.annotation.Caching;
// Spring注解：标识该类为服务层组件
import org.springframework.stereotype.Service;
// Spring注解：开启事务管理
import org.springframework.transaction.annotation.Transactional;

// 引入List集合类，用于返回背景图列表
import java.util.List;

/**
 * 背景图管理服务。
 * 负责前台背景图展示、管理员背景图维护，以及相关缓存的读写与失效控制。
 */
// 标识这是一个Spring服务组件，由Spring容器管理
@Service
// Lombok自动生成构造函数，注入所有final字段
@RequiredArgsConstructor
// Lombok自动生成日志对象，可以使用log.info()等方法
@Slf4j
public class BackgroundImageService {

    // 背景图Mapper实例，用于数据库操作
    private final BackgroundImageMapper backgroundImageMapper;

    /**
     * 获取当前启用的背景图列表。
     * 面向首页、登录页等直接展示场景，结果会缓存起来减少数据库压力。
     */
    // 将方法返回值缓存到名为BACKGROUND_ENABLED的缓存中
    @Cacheable(cacheNames = CacheNames.BACKGROUND_ENABLED)
    // 返回所有启用状态的背景图列表
    public List<BackgroundImage> getAllEnabled() {
        // 调用Mapper查询所有启用的背景图
        return backgroundImageMapper.findAllEnabled();
    }

    /**
     * 获取所有可供游客/普通用户自行选择的背景图。
     * 这类选择是前端本地偏好，不会改变系统全局启用状态。
     */
    // 将方法返回值缓存到名为BACKGROUND_SELECTABLE的缓存中
    @Cacheable(cacheNames = CacheNames.BACKGROUND_SELECTABLE)
    // 返回所有可选择的背景图列表
    public List<BackgroundImage> getAllSelectable() {
        // 调用Mapper查询所有可选择的背景图
        return backgroundImageMapper.findAllSelectable();
    }

    /**
     * 获取全部背景图，供管理员后台管理页面使用。
     */
    // 将方法返回值缓存到名为BACKGROUND_ALL的缓存中
    @Cacheable(cacheNames = CacheNames.BACKGROUND_ALL)
    // 返回所有背景图列表，包括禁用的
    public List<BackgroundImage> getAll() {
        // 调用Mapper查询所有背景图，null表示查询全部
        return backgroundImageMapper.selectList(null);
    }

    /**
     * 根据 ID 查询背景图详情。
     */
    // 根据主键ID查询单条背景图记录
    public BackgroundImage getById(Long id) {
        // 调用Mapper根据ID查询背景图
        return backgroundImageMapper.selectById(id);
    }

    /**
     * 新增背景图记录。
     * 写操作后会同时清理三个背景图缓存，确保前台和后台读取到的是最新数据。
     */
    // 开启事务管理，保证数据一致性
    @Transactional
    // 组合多个缓存清除操作
    @Caching(evict = {
            // 清除BACKGROUND_ENABLED缓存中的所有条目
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ENABLED, allEntries = true),
            // 清除BACKGROUND_SELECTABLE缓存中的所有条目
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_SELECTABLE, allEntries = true),
            // 清除BACKGROUND_ALL缓存中的所有条目
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ALL, allEntries = true)
    })
    // 创建新的背景图记录
    public BackgroundImage create(BackgroundImage image) {
        // 调用Mapper插入新的背景图记录
        backgroundImageMapper.insert(image);
        // 返回插入后的背景图对象（包含自动生成的ID）
        return image;
    }

    /**
     * 更新背景图信息。
     * 仅覆盖显式传入的字段，未提供的字段保持不变。
     */
    // 开启事务管理
    @Transactional
    // 组合多个缓存清除操作
    @Caching(evict = {
            // 清除BACKGROUND_ENABLED缓存
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ENABLED, allEntries = true),
            // 清除BACKGROUND_SELECTABLE缓存
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_SELECTABLE, allEntries = true),
            // 清除BACKGROUND_ALL缓存
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ALL, allEntries = true)
    })
    // 更新指定ID的背景图信息
    public BackgroundImage update(Long id, BackgroundImage image) {
        // 根据ID查询现有的背景图记录
        BackgroundImage existing = backgroundImageMapper.selectById(id);
        // 如果记录不存在，抛出运行时异常
        if (existing == null) {
            throw new RuntimeException("背景图片不存在：" + id);
        }

        // 如果传入的名称不为空，则更新名称
        if (image.getName() != null) {
            existing.setName(image.getName());
        }
        // 如果传入的图片URL不为空，则更新图片URL
        if (image.getImageUrl() != null) {
            existing.setImageUrl(image.getImageUrl());
        }
        // 如果传入的类型不为空，则更新类型
        if (image.getType() != null) {
            existing.setType(image.getType());
        }
        // 如果传入的启用状态不为空，则更新启用状态
        if (image.getEnabled() != null) {
            existing.setEnabled(image.getEnabled());
        }
        // 如果传入的排序值不为空，则更新排序
        if (image.getSort() != null) {
            existing.setSort(image.getSort());
        }

        // 调用Mapper更新背景图记录
        backgroundImageMapper.updateById(existing);
        // 返回更新后的背景图对象
        return existing;
    }

    /**
     * 删除背景图记录。
     */
    // 开启事务管理
    @Transactional
    // 组合多个缓存清除操作
    @Caching(evict = {
            // 清除BACKGROUND_ENABLED缓存
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ENABLED, allEntries = true),
            // 清除BACKGROUND_SELECTABLE缓存
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_SELECTABLE, allEntries = true),
            // 清除BACKGROUND_ALL缓存
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ALL, allEntries = true)
    })
    // 根据ID删除背景图记录
    public void delete(Long id) {
        // 调用Mapper根据ID删除背景图
        backgroundImageMapper.deleteById(id);
    }

    /**
     * 设置系统当前启用的背景图。
     * 业务规则是全局同一时刻只允许一张背景图处于 enabled=true。
     */
    // 开启事务管理
    @Transactional
    // 组合多个缓存清除操作
    @Caching(evict = {
            // 清除BACKGROUND_ENABLED缓存
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ENABLED, allEntries = true),
            // 清除BACKGROUND_SELECTABLE缓存
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_SELECTABLE, allEntries = true),
            // 清除BACKGROUND_ALL缓存
            @CacheEvict(cacheNames = CacheNames.BACKGROUND_ALL, allEntries = true)
    })
    // 设置指定背景图的启用状态
    public void setEnabled(Long id, Boolean enabled) {
        // 先批量关闭所有启用项，保证全局启用背景图始终只有一个。
        // 查询所有背景图记录
        List<BackgroundImage> all = backgroundImageMapper.selectList(null);
        // 遍历所有背景图
        for (BackgroundImage image : all) {
            // 如果当前背景图处于启用状态
            if (image.getEnabled()) {
                // 将其设置为禁用
                image.setEnabled(false);
                // 更新数据库中的记录
                backgroundImageMapper.updateById(image);
            }
        }

        // 再按需启用目标背景图；如果 enabled=false，则效果就是"全部关闭"。
        // 只有当enabled为true时才执行启用操作
        if (enabled) {
            // 根据ID查询目标背景图
            BackgroundImage image = backgroundImageMapper.selectById(id);
            // 如果目标背景图存在
            if (image != null) {
                // 将其设置为启用状态
                image.setEnabled(true);
                // 更新数据库中的记录
                backgroundImageMapper.updateById(image);
            }
        }
    }
}
