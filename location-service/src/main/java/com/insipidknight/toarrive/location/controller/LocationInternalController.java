package com.insipidknight.toarrive.location.controller;


import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.location.service.LocationService;
import com.insipidknight.toarrive.location.vo.NearbyDriverVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/location")
@RequiredArgsConstructor
public class LocationInternalController {
    private final LocationService locationService;

    @DeleteMapping("/driver/{driverId}")
    void removeDriver(@PathVariable Long driverId){
        locationService.removeDriver(driverId);
    };

    @GetMapping("/drivers/nearby")
    Result<List<NearbyDriverVO>> nearbyDrivers(@RequestParam Double lng,
                                               @RequestParam Double lat,
                                               @RequestParam(defaultValue = "3") Double radiusKm,
                                               @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(locationService.nearby(lng, lat, radiusKm, limit));
    }
}
