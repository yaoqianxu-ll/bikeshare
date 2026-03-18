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
 * 采用两步走策略：
 * 1. 先根据 IP 地址获取经纬度坐标
 * 2. 再根据经纬度坐标反查详细的国家、省、市、区信息
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
     * 第一步 API：根据 IP 获取经纬度
     * 使用 ip-api.com 获取 IP 对应的经纬度（免费、无需 key）
     */
    private static final String IP_TO_COORD_URL_TEMPLATE =
            "http://ip-api.com/json/%s?fields=status,message,lat,lon,query&lang=zh-CN";

    /**
     * 第二步 API：根据经纬度反查地区信息
     * 使用高德地图逆地理编码 API（需要申请 key）
     * 文档：https://lbs.amap.com/api/webservice/guide/api/georegeo#regeo
     *
     * 备用方案：使用 OpenStreetMap Nominatim（免费、无需 key，但限流）
     */
    private static final String COORD_TO_ADDRESS_URL_TEMPLATE =
            "https://restapi.amap.com/v3/geocode/regeo?location=%s&key=%s&extensions=all&radius=1000";
    private static final String COORD_TO_ADDRESS_URL_TEMPLATE_OSM =
            "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%s&lon=%s&accept-language=zh-CN";

    private final ObjectMapper objectMapper;

    @Value("${gaode.map.api.key:}")
    private String gaodeApiKey;

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
            // 第一步：根据 IP 获取经纬度
            IpCoordinateResponse coordResponse = fetchCoordinateFromIp(clientIp);
            if (coordResponse == null) {
                log.debug("Failed to fetch coordinate from IP {}", clientIp);
                return null;
            }

            Double latitude = coordResponse.getLat();
            Double longitude = coordResponse.getLon();

            if (latitude == null || longitude == null) {
                log.debug("Coordinate is null for IP {}", clientIp);
                return null;
            }

            // 第二步：根据经纬度反查地区信息
            ClientLocationResponse result = fetchLocationFromCoordinate(latitude, longitude, clientIp);
            if (result != null) {
                result.setIp(StringUtils.hasText(coordResponse.getQuery()) ? coordResponse.getQuery() : clientIp);
                result.setLatitude(latitude);
                result.setLongitude(longitude);
                result.setSource(LOCATION_SOURCE);

                locationCache.put(clientIp, result);
            }

            return result;
        } catch (Exception ex) {
            log.debug("Failed to resolve silent IP location for {}", clientIp, ex);
            return null;
        }
    }

    /**
     * 第一步：根据 IP 地址获取经纬度坐标
     */
    private IpCoordinateResponse fetchCoordinateFromIp(String ip) {
        try {
            String encodedIp = URLEncoder.encode(ip, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(IP_TO_COORD_URL_TEMPLATE.formatted(encodedIp)))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("IP coordinate lookup failed with status {} for ip {}", response.statusCode(), ip);
                return null;
            }

            IpCoordinateResponse payload = objectMapper.readValue(response.body(), IpCoordinateResponse.class);
            if (payload == null || !"success".equalsIgnoreCase(payload.getStatus())) {
                log.debug("IP coordinate lookup returned failure for ip {}: {}", ip, payload == null ? "empty" : payload.getMessage());
                return null;
            }

            return payload;
        } catch (Exception e) {
            log.debug("Exception while fetching coordinate from IP {}: {}", ip, e.getMessage());
            return null;
        }
    }

    /**
     * 第二步：根据经纬度坐标反查地区信息
     * 优先使用高德地图 API，如果未配置 key 或失败则使用 OSM Nominatim 备用
     */
    private ClientLocationResponse fetchLocationFromCoordinate(Double latitude, Double longitude, String ip) {
        // 优先尝试高德地图 API
        if (StringUtils.hasText(gaodeApiKey)) {
            try {
                ClientLocationResponse result = fetchLocationFromGaode(latitude, longitude);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                log.debug("Gaode API failed for IP {} coord {},{}: {}", ip, latitude, longitude, e.getMessage());
            }
        }

        // 备用方案：使用 OSM Nominatim
        try {
            ClientLocationResponse result = fetchLocationFromOsm(latitude, longitude);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            log.debug("OSM Nominatim failed for IP {} coord {},{}: {}", ip, latitude, longitude, e.getMessage());
        }

        // 都失败了，返回一个最小化的结果
        ClientLocationResponse fallback = new ClientLocationResponse();
        fallback.setLocationText("未知地区");
        return fallback;
    }

    /**
     * 使用高德地图逆地理编码 API 反查地区
     */
    private ClientLocationResponse fetchLocationFromGaode(Double latitude, Double longitude) throws Exception {
        String locationParam = URLEncoder.encode(longitude + "," + latitude, StandardCharsets.UTF_8.name());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(COORD_TO_ADDRESS_URL_TEMPLATE.formatted(locationParam, gaodeApiKey)))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }

        GaodeReGeoResponse payload = objectMapper.readValue(response.body(), GaodeReGeoResponse.class);
        if (payload == null || !"1".equals(payload.getStatus())) {
            return null;
        }

        if (payload.getRegeocode() == null || payload.getRegeocode().getAddressComponent() == null) {
            return null;
        }

        GaodeReGeoResponse.AddressComponent addr = payload.getRegeocode().getAddressComponent();
        ClientLocationResponse result = new ClientLocationResponse();
        result.setCountry(cleanLocationPart(addr.getCountry()));
        result.setProvince(cleanLocationPart(addr.getProvince()));
        result.setCity(cleanLocationPart(addr.getCity()));
        result.setDistrict(cleanLocationPart(addr.getDistrict()));
        result.setLocationText(joinLocationText(result.getCountry(), result.getProvince(), result.getCity(), result.getDistrict()));

        return result;
    }

    /**
     * 使用 OpenStreetMap Nominatim 反查地区（备用方案）
     */
    private ClientLocationResponse fetchLocationFromOsm(Double latitude, Double longitude) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(COORD_TO_ADDRESS_URL_TEMPLATE_OSM.formatted(latitude, longitude)))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .header("User-Agent", "BikeShare-App/1.0")  // Nominatim 要求 User-Agent
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }

        OsmReGeoResponse payload = objectMapper.readValue(response.body(), OsmReGeoResponse.class);
        if (payload == null || payload.getAddress() == null) {
            return null;
        }

        OsmAddress addr = payload.getAddress();
        ClientLocationResponse result = new ClientLocationResponse();
        result.setCountry(cleanLocationPart(addr.getCountry()));
        result.setProvince(cleanLocationPart(addr.getState()));
        result.setCity(cleanLocationPart(addr.getCity() != null ? addr.getCity() : addr.getTown() != null ? addr.getTown() : addr.getVillage()));
        result.setDistrict(cleanLocationPart(addr.getCounty() != null ? addr.getCounty() : addr.getSuburb()));
        result.setLocationText(joinLocationText(result.getCountry(), result.getProvince(), result.getCity(), result.getDistrict()));

        return result;
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
     * 第一步 API 响应：IP 到经纬度
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IpCoordinateResponse {
        private String status;
        private String message;
        private Double lat;
        private Double lon;
        private String query;
    }

    /**
     * 第二步 API 响应：高德地图逆地理编码
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GaodeReGeoResponse {
        private String status;
        private String infocode;
        private Regeocode regeocode;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Regeocode {
            private AddressComponent addressComponent;
            private String formattedAddress;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class AddressComponent {
            private String country;
            private String province;
            private String city;
            private String district;
        }
    }

    /**
     * 第二步 API 响应：OSM Nominatim 逆地理编码
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OsmReGeoResponse {
        private OsmAddress address;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OsmAddress {
        private String country;
        private String state;
        private String city;
        private String town;
        private String village;
        private String county;
        private String suburb;
    }
}
