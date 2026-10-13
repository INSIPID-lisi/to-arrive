package com.insipidknight.toarrive.location.vo;

import lombok.Data;

@Data
public class NearbyDriverVO {

    private Long driverId;

    private Double lng;

    private Double lat;

    private Double distanceMeters;
}
