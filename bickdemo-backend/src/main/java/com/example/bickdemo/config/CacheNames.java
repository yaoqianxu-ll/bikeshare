package com.example.bickdemo.config;

/**
 * Redis cache names used across the application.
 */
public final class CacheNames {

    public static final String STATISTICS_OVERVIEW = "statistics:overview";
    public static final String BACKGROUND_ENABLED = "background:enabled";
    public static final String BACKGROUND_SELECTABLE = "background:selectable";
    public static final String BACKGROUND_ALL = "background:all";

    // 自行车缓存
    public static final String BICYCLES_AVAILABLE = "bicycles:available";
    public static final String BICYCLES_PAGE = "bicycles:page";
    public static final String BICYCLE_DETAIL = "bicycle:detail";

    // 公告缓存
    public static final String NOTICES_PUBLISHED = "notices:published";
    public static final String NOTICES_ALL = "notices:all";
    public static final String NOTICE_DETAIL = "notice:detail";

    private CacheNames() {
    }
}
