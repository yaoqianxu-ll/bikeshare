package com.example.bickdemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 地理距离工具。
 * 统一用哈弗辛公式计算两个经纬度之间的球面距离，避免各业务模块重复实现。
 */
public final class GeoDistanceUtils {

    private GeoDistanceUtils() {
    }

    public static Double calculateDistanceKm(Double latitudeA,
                                             Double longitudeA,
                                             Double latitudeB,
                                             Double longitudeB) {
        if (latitudeA == null || longitudeA == null || latitudeB == null || longitudeB == null) {
            return null;
        }

        double earthRadius = 6371.0;
        double latDistance = Math.toRadians(latitudeB - latitudeA);
        double lonDistance = Math.toRadians(longitudeB - longitudeA);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitudeA))
                * Math.cos(Math.toRadians(latitudeB))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadius * c)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
