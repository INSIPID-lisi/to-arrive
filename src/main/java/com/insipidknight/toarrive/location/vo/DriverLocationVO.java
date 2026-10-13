package com.insipidknight.toarrive.location.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DriverLocationVO {

    private Long driverId;

    private Double lng;

    private Double lat;

    private LocalDateTime reportTime;
}
