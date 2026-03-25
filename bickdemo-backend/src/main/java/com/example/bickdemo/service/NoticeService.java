package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.NoticeRequest;
import com.example.bickdemo.dto.NoticeResponse;
import com.example.bickdemo.entity.Notice;
import com.example.bickdemo.entity.NoticeStatus;
import com.example.bickdemo.entity.NoticeType;
import com.example.bickdemo.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公告管理服务
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    private final NoticeMapper noticeMapper;

    /**
     * 获取所有已发布的公告（用户可见）
     */
    @Cacheable(cacheNames = CacheNames.NOTICES_PUBLISHED, unless = "#result.isEmpty()")
    public List<NoticeResponse> getPublishedNotices() {
        log.debug("查询已发布公告列表");
        return noticeMapper.findAllPublished().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 获取公告详情
     */
    @Cacheable(cacheNames = CacheNames.NOTICE_DETAIL, key = "#id", unless = "#result == null")
    public NoticeResponse getNoticeById(Long id) {
        log.debug("根据 ID 查询公告：{}", id);
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }
        return convertToResponse(notice);
    }

    /**
     * 根据类型获取公告列表
     */
    public List<NoticeResponse> getNoticesByType(NoticeType type) {
        log.debug("根据类型查询公告：{}", type);
        return noticeMapper.findByType(type).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有公告（管理员）
     */
    @Cacheable(cacheNames = CacheNames.NOTICES_ALL, unless = "#result.isEmpty()")
    public List<NoticeResponse> getAllNotices() {
        log.debug("查询所有公告（管理员）");
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getDeleted, 0)
                .orderByDesc(Notice::getPriority)
                .orderByDesc(Notice::getPublishTime);
        return noticeMapper.selectList(wrapper).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取公告分页列表（管理员）
     */
    public List<NoticeResponse> getNoticesPage(int page, int size) {
        log.debug("分页查询公告：page={}, size={}", page, size);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getDeleted, 0)
                .orderByDesc(Notice::getPriority)
                .orderByDesc(Notice::getPublishTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Notice> noticePage =
                noticeMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        return noticePage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建公告
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL, CacheNames.NOTICE_DETAIL}, allEntries = true)
    public NoticeResponse createNotice(NoticeRequest request, Long authorId) {
        Notice notice = new Notice();
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setType(request.getType());
        notice.setCoverImage(request.getCoverImage());
        notice.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        notice.setPublishTime(request.getPublishTime());
        notice.setAuthorId(authorId);
        notice.setStatus(NoticeStatus.DRAFT);

        noticeMapper.insert(notice);
        return convertToResponse(notice);
    }

    /**
     * 更新公告
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL}, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.NOTICE_DETAIL, key = "#id")
    })
    public NoticeResponse updateNotice(Long id, NoticeRequest request) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }

        if (request.getTitle() != null) {
            notice.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            notice.setContent(request.getContent());
        }
        if (request.getType() != null) {
            notice.setType(request.getType());
        }
        if (request.getCoverImage() != null) {
            notice.setCoverImage(request.getCoverImage());
        }
        if (request.getPriority() != null) {
            notice.setPriority(request.getPriority());
        }
        if (request.getPublishTime() != null) {
            notice.setPublishTime(request.getPublishTime());
        }

        noticeMapper.updateById(notice);
        return convertToResponse(notice);
    }

    /**
     * 删除公告
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL}, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.NOTICE_DETAIL, key = "#id")
    })
    public void deleteNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }
        noticeMapper.deleteById(id);
    }

    /**
     * 发布公告
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL, CacheNames.NOTICE_DETAIL}, allEntries = true)
    public NoticeResponse publishNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }
        notice.setStatus(NoticeStatus.PUBLISHED);
        notice.setPublishTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
        return convertToResponse(notice);
    }

    /**
     * 隐藏公告
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL, CacheNames.NOTICE_DETAIL}, allEntries = true)
    public NoticeResponse hideNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }
        notice.setStatus(NoticeStatus.HIDDEN);
        noticeMapper.updateById(notice);
        return convertToResponse(notice);
    }

    private NoticeResponse convertToResponse(Notice notice) {
        NoticeResponse response = new NoticeResponse();
        response.setId(notice.getId());
        response.setTitle(notice.getTitle());
        response.setContent(notice.getContent());
        response.setType(notice.getType());
        response.setCoverImage(notice.getCoverImage());
        response.setStatus(notice.getStatus());
        response.setPriority(notice.getPriority());
        response.setPublishTime(notice.getPublishTime());
        response.setAuthorId(notice.getAuthorId());
        response.setCreatedAt(notice.getCreatedAt());
        response.setUpdatedAt(notice.getUpdatedAt());
        return response;
    }
}
