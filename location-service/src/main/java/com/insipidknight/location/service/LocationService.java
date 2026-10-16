package com.insipidknight.location.service;

import com.insipidknight.location.entity.Location;
import com.insipidknight.location.vo.DriverLocationVO;
import com.insipidknight.location.vo.NearbyDriverVO;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final String GEO_KEY = "location:driver:geo";
    private static final String REPORT_TIME_KEY = "location:driver:report-time";
    private static final Metrics DISTANCE_UNIT = Metrics.KILOMETERS;
    private static final double METERS_PER_KM = 1000d;
    private static final long REPORT_TTL_MILLIS = 5 * 60 * 1000L;

    private final StringRedisTemplate redisTemplate;

    public void report(Long driverId, Double lng, Double lat) {
        String member = String.valueOf(driverId);
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(lng, lat), member);
        redisTemplate.<String, String>opsForHash().put(REPORT_TIME_KEY, member, String.valueOf(System.currentTimeMillis()));
        log.debug("司机位置上报: driverId={}, lng={}, lat={}", driverId, lng, lat);
    }

    /**
     * 司机收车：从 GEO 索引摘除，使其不再出现在附近司机中
     */
    public void removeDriver(Long driverId) {
        String member = String.valueOf(driverId);
        redisTemplate.opsForGeo().remove(GEO_KEY, member);
        redisTemplate.<String, String>opsForHash().delete(REPORT_TIME_KEY, member);
    }

    public DriverLocationVO getDriverLocation(Long driverId) {
        String member = String.valueOf(driverId);
        List<Point> points = redisTemplate.opsForGeo().position(GEO_KEY, member);
        Point point = (points == null || points.isEmpty()) ? null : points.getFirst();
        Long reportTime = readReportTime(member);
        if (point == null || !isFresh(reportTime)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该司机位置已过期或暂无上报");
        }

        Location location = new Location(driverId, point.getX(), point.getY(), toLocalDateTime(reportTime));
        return toDriverLocationVO(location);
    }

    public List<NearbyDriverVO> nearby(Double lng, Double lat, Double radiusKm, Integer limit) {
        GeoResults<GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                GEO_KEY,
                GeoReference.fromCoordinate(lng, lat),
                GeoShape.byRadius(new Distance(radiusKm, DISTANCE_UNIT)),
                GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeCoordinates()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit));

        List<String> members = results.getContent().stream()
                .map(result -> result.getContent().getName())
                .toList();
        Map<String, Long> reportTimes = readReportTimes(members);

        return results.getContent().stream()
                .filter(result -> isFresh(reportTimes.get(result.getContent().getName())))
                .map(result -> {
                    GeoLocation<String> geoLocation = result.getContent();
                    Point point = geoLocation.getPoint();

                    NearbyDriverVO vo = new NearbyDriverVO();
                    vo.setDriverId(Long.valueOf(geoLocation.getName()));
                    vo.setLng(point.getX());
                    vo.setLat(point.getY());
                    vo.setDistanceMeters(result.getDistance().in(DISTANCE_UNIT).getValue() * METERS_PER_KM);
                    return vo;
                })
                .toList();
    }

    private Map<String, Long> readReportTimes(List<String> members) {
        if (members.isEmpty()) {
            return Map.of();
        }
        List<String> values = redisTemplate.<String, String>opsForHash().multiGet(REPORT_TIME_KEY, members);

        Map<String, Long> reportTimes = new HashMap<>(members.size());
        for (int i = 0; i < members.size(); i++) {
            String value = values.get(i);
            if (value != null) {
                reportTimes.put(members.get(i), Long.parseLong(value));
            }
        }
        return reportTimes;
    }

    private Long readReportTime(String member) {
        String value = redisTemplate.<String, String>opsForHash().get(REPORT_TIME_KEY, member);
        return value == null ? null : Long.parseLong(value);
    }

    private boolean isFresh(Long reportTimeMillis) {
        return reportTimeMillis != null && System.currentTimeMillis() - reportTimeMillis <= REPORT_TTL_MILLIS;
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private DriverLocationVO toDriverLocationVO(Location location) {
        DriverLocationVO vo = new DriverLocationVO();
        vo.setDriverId(location.getDriverId());
        vo.setLng(location.getLng());
        vo.setLat(location.getLat());
        vo.setReportTime(location.getReportTime());
        return vo;
    }
}
