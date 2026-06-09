package com.example.bickdemo.service;

// 引入 MyBatis-Plus 的 Lambda 查询包装器，用于构建类型安全的查询条件
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 引入缓存名称常量，用于标识不同的缓存空间
import com.example.bickdemo.config.CacheNames;
// 引入公告请求数据传输对象，包含创建/更新公告的请求数据
import com.example.bickdemo.dto.NoticeRequest;
// 引入公告响应数据传输对象，用于返回公告数据给前端
import com.example.bickdemo.dto.NoticeResponse;
// 引入公告实体类，对应数据库中的公告表
import com.example.bickdemo.entity.Notice;
// 引入公告状态枚举，定义公告的发布状态（如草稿、已发布、已隐藏）
import com.example.bickdemo.entity.NoticeStatus;
// 引入公告类型枚举，定义公告的分类类型
import com.example.bickdemo.entity.NoticeType;
// 引入公告 Mapper 接口，用于数据库操作
import com.example.bickdemo.mapper.NoticeMapper;
// 引入 Lombok 注解，用于生成构造函数（自动注入依赖）
import lombok.RequiredArgsConstructor;
// 引入 Lombok 注解，用于生成日志记录器
import lombok.extern.slf4j.Slf4j;
// 引入 Spring Cache 注解，用于缓存驱逐（删除缓存）
import org.springframework.cache.annotation.CacheEvict;
// 引入 Spring Cache 注解，用于缓存存储
import org.springframework.cache.annotation.Cacheable;
// 引入 Spring Cache 注解，用于组合多个缓存操作
import org.springframework.cache.annotation.Caching;
// 引入 Spring 服务注解，标识这是一个服务层组件
import org.springframework.stereotype.Service;
// 引入事务注解，用于管理数据库事务
import org.springframework.transaction.annotation.Transactional;

// 引入 Java 8 日期时间 API，用于处理发布时间等时间字段
import java.time.LocalDateTime;
// 引入列表接口，用于返回公告列表
import java.util.List;
// 引入流收集器，用于将流转换为列表
import java.util.stream.Collectors;

/**
 * 公告管理服务
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    // 公告 Mapper 接口，通过构造函数注入，用于数据库操作
    private final NoticeMapper noticeMapper;

    // 用户 Mapper 接口，用于查询用户列表
    private final com.example.bickdemo.mapper.UserMapper userMapper;

    // 用户邮件通知服务
    private final UserEmailNotificationService userEmailNotificationService;

    /**
     * 获取所有已发布的公告（用户可见）
     * 使用缓存存储已发布公告列表，缓存名为 CacheNames.NOTICES_PUBLISHED
     * unless = "#result.isEmpty()" 表示当结果为空时不缓存
     * @return 已发布公告的响应列表
     */
    @Cacheable(cacheNames = CacheNames.NOTICES_PUBLISHED, unless = "#result.isEmpty()")
    public List<NoticeResponse> getPublishedNotices() {
        // 记录调试日志，表示正在查询已发布公告列表
        log.debug("查询已发布公告列表");
        // 调用 noticeMapper 的 findAllPublished 方法查询所有已发布公告
        // 使用 stream() 将查询结果转换为流，进行数据转换
        // 使用 map() 将每个 Notice 实体转换为 NoticeResponse DTO
        // 使用 collect() 将流收集为 List 列表返回
        return noticeMapper.findAllPublished().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 获取公告详情
     * 使用缓存存储公告详情，缓存名为 CacheNames.NOTICE_DETAIL，key 为公告 ID
     * unless = "#result == null" 表示当结果为 null 时不缓存
     * @param id 公告的唯一标识 ID
     * @return 公告的响应数据
     * @throws RuntimeException 当公告不存在时抛出异常
     */
    @Cacheable(cacheNames = CacheNames.NOTICE_DETAIL, key = "#id", unless = "#result == null")
    public NoticeResponse getNoticeById(Long id) {
        // 记录调试日志，包含公告 ID 参数
        log.debug("根据 ID 查询公告：{}", id);
        // 根据 ID 从数据库查询公告实体
        Notice notice = noticeMapper.selectById(id);
        // 判断公告是否存在，若不存在则抛出运行时异常
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }
        // 将公告实体转换为响应 DTO 并返回
        return convertToResponse(notice);
    }

    /**
     * 根据类型获取公告列表
     * @param type 公告类型枚举值
     * @return 该类型的所有公告响应列表
     */
    public List<NoticeResponse> getNoticesByType(NoticeType type) {
        // 记录调试日志，包含公告类型参数
        log.debug("根据类型查询公告：{}", type);
        // 调用 noticeMapper 的 findByType 方法查询指定类型的公告
        // 使用 stream() 将查询结果转换为流
        // 使用 map() 将每个 Notice 实体转换为 NoticeResponse DTO
        // 使用 collect() 将流收集为 List 列表返回
        return noticeMapper.findByType(type).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取已发布公告的分页列表（用户端）
     * @param page 页码，从 1 开始
     * @param size 每页显示的公告数量
     * @return 分页结果
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<NoticeResponse> getPublishedNoticesPage(int page, int size) {
        log.debug("分页查询已发布公告：page={}, size={}", page, size);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, NoticeStatus.PUBLISHED)
                .eq(Notice::getDeleted, 0)
                .orderByDesc(Notice::getPriority)
                .orderByDesc(Notice::getPublishTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Notice> noticePage =
                noticeMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        // 转换为 Response DTO 分页
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<NoticeResponse> resultPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(noticePage.getCurrent(), noticePage.getSize(), noticePage.getTotal());
        resultPage.setRecords(noticePage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 获取所有公告（管理员）
     * 使用缓存存储所有公告列表，缓存名为 CacheNames.NOTICES_ALL
     * 管理员可以查看包括已删除在内的所有公告
     * @return 所有公告的响应列表
     */
    @Cacheable(cacheNames = CacheNames.NOTICES_ALL, unless = "#result.isEmpty()")
    public List<NoticeResponse> getAllNotices() {
        // 记录调试日志，表示正在查询所有公告（管理员视图）
        log.debug("查询所有公告（管理员）");
        // 创建 Lambda 查询条件包装器
        // eq(Notice::getDeleted, 0) 表示只查询未删除的公告（逻辑删除标记为 0）
        // orderByDesc(Notice::getPriority) 按优先级降序排列，优先级高的排在前面
        // orderByDesc(Notice::getPublishTime) 按发布时间降序排列，最新发布的排在前面
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getDeleted, 0)
                .orderByDesc(Notice::getPriority)
                .orderByDesc(Notice::getPublishTime);
        // 执行查询并使用 stream() 转换为响应列表
        return noticeMapper.selectList(wrapper).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取公告分页列表（管理员）
     * @param page 页码，从 1 开始
     * @param size 每页显示的公告数量
     * @return 当前页的公告响应列表
     */
    public List<NoticeResponse> getNoticesPage(int page, int size) {
        // 记录调试日志，包含分页参数
        log.debug("分页查询公告：page={}, size={}", page, size);
        // 创建 Lambda 查询条件包装器，设置查询条件为未删除的公告
        // 按优先级和发布时间降序排列
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getDeleted, 0)
                .orderByDesc(Notice::getPriority)
                .orderByDesc(Notice::getPublishTime);
        // 创建 MyBatis-Plus 分页对象，传入当前页码和每页大小
        // 调用 noticeMapper 的 selectPage 方法进行分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Notice> noticePage =
                noticeMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        // 获取分页结果中的公告记录列表，并转换为响应列表返回
        return noticePage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建公告
     * 使用事务保证数据一致性，创建成功后清除相关缓存
     * @param request 公告创建请求数据，包含标题、内容、类型等信息
     * @param authorId 作者 ID，表示创建公告的用户
     * @return 创建的公告响应数据
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL, CacheNames.NOTICE_DETAIL}, allEntries = true)
    public NoticeResponse createNotice(NoticeRequest request, Long authorId) {
        // 创建新的公告实体对象
        Notice notice = new Notice();
        // 设置公告标题为请求中的标题
        notice.setTitle(request.getTitle());
        // 设置公告内容为请求中的内容
        notice.setContent(request.getContent());
        // 设置公告类型为请求中的类型
        notice.setType(request.getType());
        // 设置公告封面图片为请求中的封面图（可选）
        notice.setCoverImage(request.getCoverImage());
        // 设置公告优先级，若请求中未提供则默认为 0
        notice.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        // 设置公告发布时间为请求中指定的时间
        notice.setPublishTime(request.getPublishTime());
        // 设置公告作者 ID
        notice.setAuthorId(authorId);
        // 设置公告状态为草稿（DRAFT），新建公告默认是草稿状态
        notice.setStatus(NoticeStatus.DRAFT);

        // 将公告数据插入数据库
        noticeMapper.insert(notice);
        // 将创建的公告实体转换为响应 DTO 并返回
        return convertToResponse(notice);
    }

    /**
     * 更新公告
     * 使用事务保证数据一致性，更新成功后清除相关缓存
     * @param id 要更新的公告 ID
     * @param request 公告更新请求数据，包含需要修改的字段
     * @return 更新后的公告响应数据
     * @throws RuntimeException 当公告不存在时抛出异常
     */
    @Transactional
    @Caching(evict = {
            // 清除已发布公告缓存和所有公告缓存
            @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL}, allEntries = true),
            // 清除指定公告的详情缓存
            @CacheEvict(cacheNames = CacheNames.NOTICE_DETAIL, key = "#id")
    })
    public NoticeResponse updateNotice(Long id, NoticeRequest request) {
        // 根据 ID 从数据库查询要更新的公告
        Notice notice = noticeMapper.selectById(id);
        // 判断公告是否存在，若不存在则抛出运行时异常
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }

        // 判断请求中的标题是否为空，若不为空则更新标题
        if (request.getTitle() != null) {
            notice.setTitle(request.getTitle());
        }
        // 判断请求中的内容是否为空，若不为空则更新内容
        if (request.getContent() != null) {
            notice.setContent(request.getContent());
        }
        // 判断请求中的类型是否为空，若不为空则更新类型
        if (request.getType() != null) {
            notice.setType(request.getType());
        }
        // 判断请求中的封面图是否为空，若不为空则更新封面图
        if (request.getCoverImage() != null) {
            notice.setCoverImage(request.getCoverImage());
        }
        // 判断请求中的优先级是否为空，若不为空则更新优先级
        if (request.getPriority() != null) {
            notice.setPriority(request.getPriority());
        }
        // 判断请求中的发布时间是否为空，若不为空则更新时间
        if (request.getPublishTime() != null) {
            notice.setPublishTime(request.getPublishTime());
        }

        // 将更新后的公告数据保存到数据库
        noticeMapper.updateById(notice);
        // 将更新后的公告实体转换为响应 DTO 并返回
        return convertToResponse(notice);
    }

    /**
     * 删除公告
     * 使用事务保证数据一致性，删除成功后清除相关缓存
     * @param id 要删除的公告 ID
     * @throws RuntimeException 当公告不存在时抛出异常
     */
    @Transactional
    @Caching(evict = {
            // 清除已发布公告缓存和所有公告缓存
            @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL}, allEntries = true),
            // 清除指定公告的详情缓存
            @CacheEvict(cacheNames = CacheNames.NOTICE_DETAIL, key = "#id")
    })
    public void deleteNotice(Long id) {
        // 根据 ID 从数据库查询要删除的公告
        Notice notice = noticeMapper.selectById(id);
        // 判断公告是否存在，若不存在则抛出运行时异常
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }
        // 调用 Mapper 的 deleteById 方法从数据库中删除该公告
        noticeMapper.deleteById(id);
    }

    /**
     * 发布公告
     * 将公告状态从草稿（DRAFT）改为已发布（PUBLISHED），同时设置发布时间
     * 使用事务保证数据一致性，发布成功后清除相关缓存
     * @param id 要发布的公告 ID
     * @return 发布后的公告响应数据
     * @throws RuntimeException 当公告不存在时抛出异常
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL, CacheNames.NOTICE_DETAIL}, allEntries = true)
    public NoticeResponse publishNotice(Long id) {
        // 根据 ID 从数据库查询要发布的公告
        Notice notice = noticeMapper.selectById(id);
        // 判断公告是否存在，若不存在则抛出运行时异常
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }
        // 将公告状态设置为已发布（PUBLISHED）
        notice.setStatus(NoticeStatus.PUBLISHED);
        // 设置发布时间为当前时间
        notice.setPublishTime(LocalDateTime.now());
        // 将更新后的公告数据保存到数据库
        noticeMapper.updateById(notice);

        // 公告发布后，给开启系统邮件通知的用户发送通知邮件
        notifyAllSystemEmailUsers(
                "新公告：" + notice.getTitle(),
                "管理员发布了一则新公告：" + notice.getTitle(),
                "/notices"
        );

        // 将更新后的公告实体转换为响应 DTO 并返回
        return convertToResponse(notice);
    }

    /**
     * 隐藏公告
     * 将公告状态从已发布（PUBLISHED）改为已隐藏（HIDDEN）
     * 使用事务保证数据一致性，隐藏成功后清除相关缓存
     * @param id 要隐藏的公告 ID
     * @return 隐藏后的公告响应数据
     * @throws RuntimeException 当公告不存在时抛出异常
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.NOTICES_PUBLISHED, CacheNames.NOTICES_ALL, CacheNames.NOTICE_DETAIL}, allEntries = true)
    public NoticeResponse hideNotice(Long id) {
        // 根据 ID 从数据库查询要隐藏的公告
        Notice notice = noticeMapper.selectById(id);
        // 判断公告是否存在，若不存在则抛出运行时异常
        if (notice == null) {
            throw new RuntimeException("公告不存在：" + id);
        }
        // 将公告状态设置为已隐藏（HIDDEN）
        notice.setStatus(NoticeStatus.HIDDEN);
        // 将更新后的公告数据保存到数据库
        noticeMapper.updateById(notice);
        // 将更新后的公告实体转换为响应 DTO 并返回
        return convertToResponse(notice);
    }

    /**
     * 将公告实体转换为响应 DTO
     * 用于将数据库中查询到的 Notice 实体对象转换为前端需要的 NoticeResponse 对象
     * @param notice 公告实体对象
     * @return 公告响应数据传输对象
     */
    private NoticeResponse convertToResponse(Notice notice) {
        // 创建新的公告响应对象
        NoticeResponse response = new NoticeResponse();
        // 设置响应对象的 ID 为实体的 ID
        response.setId(notice.getId());
        // 设置响应对象的标题为实体的标题
        response.setTitle(notice.getTitle());
        // 设置响应对象的内容为实体的内容
        response.setContent(notice.getContent());
        // 设置响应对象的类型为实体的类型
        response.setType(notice.getType());
        // 设置响应对象的封面图为实体的封面图
        response.setCoverImage(notice.getCoverImage());
        // 设置响应对象的状态为实体的状态
        response.setStatus(notice.getStatus());
        // 设置响应对象的优先级为实体的优先级
        response.setPriority(notice.getPriority());
        // 设置响应对象的发布时间为实体的发布时间
        response.setPublishTime(notice.getPublishTime());
        // 设置响应对象的作者 ID 为实体的作者 ID
        response.setAuthorId(notice.getAuthorId());
        // 设置响应对象的创建时间为实体的创建时间
        response.setCreatedAt(notice.getCreatedAt());
        // 设置响应对象的更新时间为实体的更新时间
        response.setUpdatedAt(notice.getUpdatedAt());
        // 返回填充好的响应对象
        return response;
    }

    /**
     * 给所有开启系统邮件通知的用户发送系统通知邮件。
     */
    private void notifyAllSystemEmailUsers(String subject, String content, String actionUrl) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.bickdemo.entity.User> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.eq("deleted", 0).eq("enabled", 1);
            List<com.example.bickdemo.entity.User> allUsers = userMapper.selectList(wrapper);
            for (com.example.bickdemo.entity.User user : allUsers) {
                userEmailNotificationService.sendSystemEmail(user, subject, subject, content, actionUrl);
            }
        } catch (Exception e) {
            log.error("批量发送系统邮件通知失败: {}", e.getMessage());
        }
    }
}
