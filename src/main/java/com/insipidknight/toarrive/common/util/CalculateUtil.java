package com.insipidknight.toarrive.common.util;

import java.math.BigDecimal;

public class CalculateUtil {

    private static final double EARTH_RADIUS = 6371.0; // 地球平均半径，单位：公里

    /**
     * 根据经纬度计算两点间距离（单位：公里）
     */
    public static BigDecimal calculateDistance(double startLng, double startLat,
                                               double endLng, double endLat) {
        double lat1 = Math.toRadians(startLat);
        double lat2 = Math.toRadians(endLat);
        double deltaLat = Math.toRadians(endLat - startLat);
        double deltaLng = Math.toRadians(endLng - startLng);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return BigDecimal.valueOf(EARTH_RADIUS * c);
    }

}
