package com.example.bickdemo.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class IpAddressUtils {

    private static final String UNKNOWN = "unknown";

    private IpAddressUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !UNKNOWN.equalsIgnoreCase(value)) {
                return normalizeIp(value.split(",")[0].trim());
            }
        }
        return normalizeIp(request.getRemoteAddr());
    }

    public static String resolveAddress(String ip) {
        String normalizedIp = normalizeIp(ip);
        if ("127.0.0.1".equals(normalizedIp)) {
            return "本机地址";
        }
        if (isPrivateIpv4(normalizedIp)) {
            return "内网地址";
        }
        return "外网地址";
    }

    private static String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "127.0.0.1";
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private static boolean isPrivateIpv4(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first == 10
                    || (first == 192 && second == 168)
                    || (first == 172 && second >= 16 && second <= 31);
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
