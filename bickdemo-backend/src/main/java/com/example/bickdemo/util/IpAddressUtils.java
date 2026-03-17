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
                "CF-Connecting-IP",
                "True-Client-IP",
                "X-Real-IP",
                "X-Client-IP",
                "Fastly-Client-IP",
                "Fly-Client-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !UNKNOWN.equalsIgnoreCase(value)) {
                return normalizeIp(extractIpFromHeaderValue(value));
            }
        }
        String forwarded = request.getHeader("Forwarded");
        if (StringUtils.hasText(forwarded) && !UNKNOWN.equalsIgnoreCase(forwarded)) {
            String resolvedIp = extractIpFromForwardedHeader(forwarded);
            if (StringUtils.hasText(resolvedIp)) {
                return normalizeIp(resolvedIp);
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

    public static boolean isLocalOrPrivateIp(String ip) {
        String normalizedIp = normalizeIp(ip);
        return "127.0.0.1".equals(normalizedIp) || isPrivateIpv4(normalizedIp) || isPrivateIpv6(normalizedIp);
    }

    private static String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "127.0.0.1";
        }
        String normalized = ip.trim().replace("\"", "");
        if (normalized.startsWith("[") && normalized.contains("]")) {
            normalized = normalized.substring(1, normalized.indexOf(']'));
        } else if (normalized.contains(":") && normalized.contains(".") && normalized.chars().filter(ch -> ch == ':').count() == 1) {
            normalized = normalized.substring(0, normalized.indexOf(':'));
        }
        if ("0:0:0:0:0:0:0:1".equals(normalized) || "::1".equals(normalized)) {
            return "127.0.0.1";
        }
        return normalized;
    }

    private static String extractIpFromHeaderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.split(",")[0].trim();
    }

    private static String extractIpFromForwardedHeader(String headerValue) {
        String[] segments = headerValue.split("[;,]");
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.regionMatches(true, 0, "for=", 0, 4)) {
                return trimmed.substring(4).trim();
            }
        }
        return null;
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

    private static boolean isPrivateIpv6(String ip) {
        String normalized = ip == null ? "" : ip.toLowerCase();
        return normalized.startsWith("fe80:")
                || normalized.startsWith("fc")
                || normalized.startsWith("fd");
    }
}
