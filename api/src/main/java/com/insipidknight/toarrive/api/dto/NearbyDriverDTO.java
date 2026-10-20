package com.insipidknight.toarrive.api.dto;

/**
 * 附近司机候选：distanceMeters 为司机当前位置到请求坐标的距离。
 */
public record NearbyDriverDTO(Long driverId, Double lng, Double lat, Double distanceMeters) {
}
