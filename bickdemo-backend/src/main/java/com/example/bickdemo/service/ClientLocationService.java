package com.example.bickdemo.service;

import com.example.bickdemo.dto.ClientLocationResponse;
import com.example.bickdemo.util.IpAddressUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 基于公网 IP 做静默归属地推断。
 * 使用 ipwhois.app 直接获取 IP 对应的地区信息（免费、无需 key、支持中文）。
 *
 * 目的不是替代 GPS，而是在不弹浏览器定位权限框的前提下，
 * 给"附近可租"提供一个足够接近国家/地区/城市的默认推荐点。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientLocationService {

    private static final String LOCATION_SOURCE = "IP_LOOKUP"; // 数据来源标识，标记为IP查询方式

    private static final String ENGLISH_LOCATION_SEGMENT_REGEX = "\\b[A-Za-z]+(?:[\\s-]+[A-Za-z]+)*\\b"; // 匹配英文单词或词组的正则表达式，用于清洗地点中的英文字符

    /**
     * 使用 ipwhois.app 获取 IP 对应的地区信息
     * 文档：https://ipwhois.app/documentation/
     * 免费不限次数，支持中文
     */
    private static final String IP_LOCATION_URL_TEMPLATE =
            "https://ipwhois.app/json/%s?lang=zh-CN"; // IP查询API模板，使用中文语言

    private final ObjectMapper objectMapper; // Jackson对象映射器，用于解析JSON响应

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)) // 设置连接超时时间为3秒
            .build(); // 构建HTTP客户端实例

    private final Cache<String, ClientLocationResponse> locationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(6)) // 缓存写入后6小时过期
            .maximumSize(2048) // 最大缓存2048条记录
            .build(); // 构建本地缓存实例，用于缓存IP地理位置结果

    /**
     * 根据当前请求推断客户端所在地区。
     * 内网、本机地址或者外部接口失败时返回 null，由前端回退到默认发现模式。
     */
    public ClientLocationResponse resolveClientLocation(HttpServletRequest request) {
        String clientIp = IpAddressUtils.resolveClientIp(request); // 从请求中解析客户端真实IP地址
        if (!StringUtils.hasText(clientIp) || IpAddressUtils.isLocalOrPrivateIp(clientIp)) { // 检查IP是否为空或为内网/本地IP
            return null; // 空或内网IP直接返回null，不进行地理位置查询
        }

        ClientLocationResponse cached = locationCache.getIfPresent(clientIp); // 尝试从缓存中获取已查询过的结果
        if (cached != null) { // 如果缓存命中
            return cached; // 直接返回缓存的地理位置结果
        }

        try {
            // 使用 ipwhois.app 直接获取地区信息
            IpLocationResponse payload = fetchLocationFromIp(clientIp); // 调用外部API查询IP对应的地理位置
            if (payload != null && payload.getSuccess()) { // 判断API返回是否成功
                ClientLocationResponse result = new ClientLocationResponse(); // 创建响应结果对象
                result.setIp(clientIp); // 设置客户端IP地址
                result.setSource(LOCATION_SOURCE); // 设置数据来源为IP查询
                result.setLatitude(payload.getLatitude()); // 设置纬度
                result.setLongitude(payload.getLongitude()); // 设置经度
                result.setCountry(cleanLocationPart(payload.getCountry())); // 清洗并设置国家名称
                result.setProvince(cleanLocationPart(payload.getRegion())); // 清洗并设置省份/地区名称
                result.setCity(cleanLocationPart(payload.getCity())); // 清洗并设置城市名称
                // ipwhois.app 的 district 字段通常为空，这里不使用
                result.setLocationText(joinLocationText( // 拼接完整的地区文本描述
                    result.getCountry(), // 国家
                    result.getProvince(), // 省份
                    result.getCity(), // 城市
                    null // 区县（不使用）
                ));

                locationCache.put(clientIp, result); // 将结果存入缓存
                return result; // 返回地理位置结果
            }

            log.debug("Failed to fetch location from IP {}", clientIp); // 记录调试日志：API返回失败
            return null; // 返回null
        } catch (Exception ex) { // 捕获所有异常
            log.debug("Exception while resolving IP location for {}: {}", clientIp, ex.getMessage()); // 记录异常信息
            return null; // 发生异常时返回null
        }
    }

    /**
     * 使用 ipwhois.app 获取 IP 地区信息
     */
    private IpLocationResponse fetchLocationFromIp(String ip) throws Exception {
        String encodedIp = URLEncoder.encode(ip, StandardCharsets.UTF_8); // 对IP地址进行URL编码，防止特殊字符问题
        HttpRequest request = HttpRequest.newBuilder() // 构建HTTP请求
                .uri(URI.create(IP_LOCATION_URL_TEMPLATE.formatted(encodedIp))) // 设置请求的API地址
                .timeout(Duration.ofSeconds(5)) // 设置请求超时时间为5秒
                .header("Accept", "application/json") // 设置Accept头，表明期望接收JSON格式
                .GET() // 使用GET方法
                .build(); // 构建请求对象

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // 发送HTTP请求并获取响应
        if (response.statusCode() < 200 || response.statusCode() >= 300) { // 检查HTTP状态码是否在成功范围内
            log.debug("IP location lookup failed with status {} for ip {}", response.statusCode(), ip); // 记录失败的响应状态码
            return null; // 非成功状态码返回null
        }

        IpLocationResponse payload = objectMapper.readValue(response.body(), IpLocationResponse.class); // 解析响应体为IpLocationResponse对象
        if (payload == null || !payload.getSuccess()) { // 检查解析结果和API返回的success字段
            log.debug("IP location lookup returned failure for ip {}: {}", ip, payload == null ? "empty" : payload.getMessage()); // 记录失败信息
            return null; // 查询失败返回null
        }

        return payload; // 成功返回解析后的对象
    }

    private String joinLocationText(String country, String province, String city, String district) {
        Set<String> parts = new LinkedHashSet<>(); // 使用LinkedHashSet存储非空部分，保持插入顺序并去重
        addIfText(parts, country); // 如果国家不为空则添加到集合
        addIfText(parts, province); // 如果省份不为空则添加到集合
        addIfText(parts, city); // 如果城市不为空则添加到集合
        addIfText(parts, district); // 如果区县不为空则添加到集合
        return parts.isEmpty() ? null : String.join(" ", parts); // 如果集合为空返回null，否则用空格拼接各部分
    }

    private void addIfText(Set<String> parts, String value) {
        String text = cleanLocationPart(value); // 清洗地点文本
        if (text != null) { // 如果清洗后不为空
            parts.add(text); // 添加到集合中
        }
    }

    private String cleanLocationPart(String value) {
        String text = trimToNull(value); // 去除首尾空白并转为null
        if (text == null || !containsChinese(text)) { // 如果为空或不包含中文字符
            return text; // 直接返回原文本
        }
        String sanitized = text // 清洗包含英文的地点名称
                .replaceAll(ENGLISH_LOCATION_SEGMENT_REGEX, " ") // 将英文部分替换为空格
                .replaceAll("\\s+", " ") // 将多个连续空格替换为单个空格
                .trim(); // 去除首尾空格
        return StringUtils.hasText(sanitized) ? sanitized : text; // 如果清洗后有有效文本返回清洗结果，否则返回原文本
    }

    private boolean containsChinese(String value) {
        if (!StringUtils.hasText(value)) { // 如果文本为空
            return false; // 返回false，不包含中文
        }
        for (int i = 0; i < value.length(); i++) { // 遍历文本中的每个字符
            if (Character.UnicodeScript.of(value.charAt(i)) == Character.UnicodeScript.HAN) { // 判断字符是否为中文
                return true; // 发现中文字符返回true
            }
        }
        return false; // 遍历完未发现中文字符返回false
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) { // 如果文本为空或仅包含空白
            return null; // 返回null
        }
        return value.trim(); // 否则返回去除首尾空白后的文本
    }

    /**
     * ipwhois.app 响应
     */
    @Data // Lombok注解，自动生成getter、setter、toString等方法
    @JsonIgnoreProperties(ignoreUnknown = true) // 解析JSON时忽略未知属性，避免API返回新字段时出错
    private static class IpLocationResponse { // 内部类，用于映射ipwhois.app的API响应
        private String ip; // IP地址
        private Boolean success; // 查询是否成功
        private String message; // 响应消息
        private String country; // 国家
        private String region; // 省份/地区
        private String city; // 城市
        private Double latitude; // 纬度
        private Double longitude; // 经度
    }
}
