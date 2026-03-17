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
 * 目的不是替代 GPS，而是在不弹浏览器定位权限框的前提下，
 * 给“附近可租”提供一个足够接近国家/地区/城市的默认推荐点。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientLocationService {

    private static final String LOCATION_SOURCE = "IP_LOOKUP";
    private static final String LOOKUP_URL_TEMPLATE =
            "http://ip-api.com/json/%s?fields=status,message,country,countryCode,regionName,city,district,lat,lon,query&lang=zh-CN";

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
            String encodedIp = URLEncoder.encode(clientIp, StandardCharsets.UTF_8);
            HttpRequest lookupRequest = HttpRequest.newBuilder()
                    .uri(URI.create(LOOKUP_URL_TEMPLATE.formatted(encodedIp)))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(lookupRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("IP location lookup failed with status {} for ip {}", response.statusCode(), clientIp);
                return null;
            }

            IpApiLocationPayload payload = objectMapper.readValue(response.body(), IpApiLocationPayload.class);
            if (payload == null || !"success".equalsIgnoreCase(payload.getStatus())) {
                log.debug("IP location lookup returned failure for ip {}: {}", clientIp, payload == null ? "empty" : payload.getMessage());
                return null;
            }
            ClientLocationResponse result = new ClientLocationResponse();
            result.setIp(StringUtils.hasText(payload.getQuery()) ? payload.getQuery() : clientIp);
            result.setCountry(trimToNull(payload.getCountry()));
            result.setCountryCode(payload.getCountryCode());
            result.setProvince(trimToNull(payload.getRegionName()));
            result.setCity(trimToNull(payload.getCity()));
            result.setDistrict(trimToNull(payload.getDistrict()));
            result.setLatitude(payload.getLat());
            result.setLongitude(payload.getLon());
            result.setLocationText(joinLocationText(payload.getCountry(), payload.getRegionName(), payload.getCity(), payload.getDistrict()));
            result.setSource(LOCATION_SOURCE);

            locationCache.put(clientIp, result);
            return result;
        } catch (Exception ex) {
            log.debug("Failed to resolve silent IP location for {}", clientIp, ex);
            return null;
        }
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
        String text = trimToNull(value);
        if (text != null) {
            parts.add(text);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IpApiLocationPayload {
        private String status;
        private String message;
        private String country;
        private String countryCode;
        private String regionName;
        private String city;
        private String district;
        private Double lat;
        private Double lon;
        private String query;
    }
}
