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

    private static final String LOCATION_SOURCE = "IP_LOOKUP";
    private static final String ENGLISH_LOCATION_SEGMENT_REGEX = "\\b[A-Za-z]+(?:[\\s-]+[A-Za-z]+)*\\b";

    /**
     * 使用 ipwhois.app 获取 IP 对应的地区信息
     * 文档：https://ipwhois.app/documentation/
     * 免费不限次数，支持中文
     */
    private static final String IP_LOCATION_URL_TEMPLATE =
            "https://ipwhois.app/json/%s?lang=zh-CN";

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final Cache<String, ClientLocationResponse> locationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(6))
            .maximumSize(2048)
            .build();

    /**
     * 根据当前请求推断客户端所在地区。
     * 内网、本机地址或者外部接口失败时返回 null，由前端回退到默认发现模式。
     */
    public ClientLocationResponse resolveClientLocation(HttpServletRequest request) {
        String clientIp = IpAddressUtils.resolveClientIp(request);
        if (!StringUtils.hasText(clientIp) || IpAddressUtils.isLocalOrPrivateIp(clientIp)) {
            return null;
        }

        ClientLocationResponse cached = locationCache.getIfPresent(clientIp);
        if (cached != null) {
            return cached;
        }

        try {
            // 使用 ipwhois.app 直接获取地区信息
            IpLocationResponse payload = fetchLocationFromIp(clientIp);
            if (payload != null && payload.getSuccess()) {
                ClientLocationResponse result = new ClientLocationResponse();
                result.setIp(clientIp);
                result.setSource(LOCATION_SOURCE);
                result.setLatitude(payload.getLatitude());
                result.setLongitude(payload.getLongitude());
                result.setCountry(cleanLocationPart(payload.getCountry()));
                result.setProvince(cleanLocationPart(payload.getRegion()));
                result.setCity(cleanLocationPart(payload.getCity()));
                // ipwhois.app 的 district 字段通常为空，这里不使用
                result.setLocationText(joinLocationText(
                    result.getCountry(),
                    result.getProvince(),
                    result.getCity(),
                    null
                ));

                locationCache.put(clientIp, result);
                return result;
            }

            log.debug("Failed to fetch location from IP {}", clientIp);
            return null;
        } catch (Exception ex) {
            log.debug("Exception while resolving IP location for {}: {}", clientIp, ex.getMessage());
            return null;
        }
    }

    /**
     * 使用 ipwhois.app 获取 IP 地区信息
     */
    private IpLocationResponse fetchLocationFromIp(String ip) throws Exception {
        String encodedIp = URLEncoder.encode(ip, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(IP_LOCATION_URL_TEMPLATE.formatted(encodedIp)))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.debug("IP location lookup failed with status {} for ip {}", response.statusCode(), ip);
            return null;
        }

        IpLocationResponse payload = objectMapper.readValue(response.body(), IpLocationResponse.class);
        if (payload == null || !payload.getSuccess()) {
            log.debug("IP location lookup returned failure for ip {}: {}", ip, payload == null ? "empty" : payload.getMessage());
            return null;
        }

        return payload;
    }

    private String joinLocationText(String country, String province, String city, String district) {
        Set<String> parts = new LinkedHashSet<>();
        addIfText(parts, country);
        addIfText(parts, province);
        addIfText(parts, city);
        addIfText(parts, district);
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private void addIfText(Set<String> parts, String value) {
        String text = cleanLocationPart(value);
        if (text != null) {
            parts.add(text);
        }
    }

    private String cleanLocationPart(String value) {
        String text = trimToNull(value);
        if (text == null || !containsChinese(text)) {
            return text;
        }
        String sanitized = text
                .replaceAll(ENGLISH_LOCATION_SEGMENT_REGEX, " ")
                .replaceAll("\\s+", " ")
                .trim();
        return StringUtils.hasText(sanitized) ? sanitized : text;
    }

    private boolean containsChinese(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.UnicodeScript.of(value.charAt(i)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * ipwhois.app 响应
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IpLocationResponse {
        private String ip;
        private Boolean success;
        private String message;
        private String country;
        private String region;
        private String city;
        private Double latitude;
        private Double longitude;
    }
}
