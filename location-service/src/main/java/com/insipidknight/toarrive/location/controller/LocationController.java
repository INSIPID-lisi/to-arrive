package com.insipidknight.toarrive.location.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.insipidknight.toarrive.location.dto.LocationReportDTO;
import com.insipidknight.toarrive.location.service.LocationService;
import com.insipidknight.toarrive.location.vo.DriverLocationVO;
import com.insipidknight.toarrive.location.vo.NearbyDriverVO;
import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/report")
    @SentinelResource(value = "location:report", blockHandler = "reportBlocked")
    public Result<Void> report(@RequestBody @Valid LocationReportDTO dto) {
        if (UserContext.getRole() != UserRole.DRIVER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅司机可上报位置");
        }
        locationService.report(UserContext.getUserId(), dto.getLng(), dto.getLat());
        return Result.success(null);
    }

    public Result<Void> reportBlocked(LocationReportDTO dto, BlockException e) {
        log.warn("位置上报熔断降级: {}", e.getClass().getSimpleName());
        return Result.error(ErrorCode.SERVICE_UNAVAILABLE, "位置上报繁忙，请稍后重试");
    }

    @GetMapping("/nearby")
    public Result<List<NearbyDriverVO>> nearby(@RequestParam Double lng,
                                               @RequestParam Double lat,
                                               @RequestParam(defaultValue = "5") Double radiusKm,
                                               @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(locationService.nearby(lng, lat, radiusKm, limit));
    }

    @GetMapping("/driver/{driverId}")
    public Result<DriverLocationVO> driverLocation(@PathVariable Long driverId) {
        return Result.success(locationService.getDriverLocation(driverId));
    }
}
