package com.example.bickdemo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 网站首次进入上报请求。
 * entryPath / entryTitle 表示用户第一次进入网站时的落地页信息，
 * 同时兼容旧的 pagePath / pageTitle 字段，避免前后端灰度期间请求失败。
 */
@Data
public class SiteVisitRequest {

    @JsonAlias("pagePath")
    @NotBlank(message = "落地页路径不能为空")
    private String entryPath;

    @JsonAlias("pageTitle")
    private String entryTitle;

    private String source;
}
