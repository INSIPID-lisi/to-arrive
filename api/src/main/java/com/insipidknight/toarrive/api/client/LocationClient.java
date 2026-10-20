package com.insipidknight.toarrive.api.client;

import com.insipidknight.toarrive.api.dto.NearbyDriverDTO;
import com.insipidknight.toarrive.common.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange("/internal/location")
public interface LocationClient {

    @DeleteMapping("/driver/{driverId}")
    void removeDriver(@PathVariable Long driverId);

    @GetMapping("/drivers/nearby")
    Result<List<NearbyDriverDTO>> nearbyDrivers(@RequestParam Double lng,
                                                @RequestParam Double lat,
                                                @RequestParam Double radiusKm,
                                                @RequestParam Integer limit);
}
