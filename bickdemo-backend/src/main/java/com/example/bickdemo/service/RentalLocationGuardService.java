package com.example.bickdemo.service;

import com.example.bickdemo.dto.ClientLocationResponse;
import com.example.bickdemo.util.GeoDistanceUtils;
import com.example.bickdemo.util.IpAddressUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 租车位置守卫。
 * 平台租赁和个人出租申请都会复用这里的 10 公里范围校验，确保前后端口径一致。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RentalLocationGuardService {

    @Value("${app.rental.range-check.max-distance-km:10}")
    private double maxRentalDistanceKm;

    @Value("${app.rental.range-check.require-public-ip:false}")
    private boolean requirePublicIp;

    private final ClientLocationService clientLocationService;

    public double ensureWithinRentalRange(HttpServletRequest request,
                                          String targetName,
                                          String targetLocation,
                                          Double targetLatitude,
                                          Double targetLongitude) {
        String targetText = resolveTargetText(targetName, targetLocation);
        String clientIp = IpAddressUtils.resolveClientIp(request);
        if (targetLatitude == null || targetLongitude == null) {
            throw new RuntimeException(targetText + "尚未配置标准经纬度，暂时不能租用");
        }

        ClientLocationResponse currentLocation = clientLocationService.resolveClientLocation(request);
        if (currentLocation == null || currentLocation.getLatitude() == null || currentLocation.getLongitude() == null) {
            if (!requirePublicIp && IpAddressUtils.isLocalOrPrivateIp(clientIp)) {
                log.debug("Skip rental distance check for local/private ip {}", clientIp);
                return -1D;
            }
            throw new RuntimeException("暂时无法确认当前位置，系统仅支持租用 " + formatDistance(maxRentalDistanceKm) + " 公里范围内的车辆，请使用可识别公网 IP 的网络后重试");
        }

        Double distanceKm = GeoDistanceUtils.calculateDistanceKm(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                targetLatitude,
                targetLongitude
        );
        if (distanceKm == null) {
            throw new RuntimeException("暂时无法计算你与" + targetText + "的距离，请稍后再试");
        }
        if (distanceKm > maxRentalDistanceKm) {
            throw new RuntimeException(targetText + "距离你当前位置约 " + distanceKm + " 公里，已超出 " + formatDistance(maxRentalDistanceKm) + " 公里租用范围");
        }
        return distanceKm;
    }

    private String resolveTargetText(String targetName, String targetLocation) {
        if (StringUtils.hasText(targetName)) {
            return "“" + targetName.trim() + "”";
        }
        if (StringUtils.hasText(targetLocation)) {
            return "位于 " + targetLocation.trim() + " 的车辆";
        }
        return "该车辆";
    }

    private String formatDistance(double distanceKm) {
        long integerDistance = (long) distanceKm;
        return distanceKm == integerDistance ? String.valueOf(integerDistance) : String.valueOf(distanceKm);
    }
}
