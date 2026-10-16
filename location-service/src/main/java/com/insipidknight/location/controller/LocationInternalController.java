package com.insipidknight.location.controller;


import com.insipidknight.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/location")
@RequiredArgsConstructor
public class LocationInternalController {
    private final LocationService locationService;

    @DeleteMapping("/driver/{driverId}")
    void removeDriver(@PathVariable Long driverId){
        locationService.removeDriver(driverId);
    };
}
