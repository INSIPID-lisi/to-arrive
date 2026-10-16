package com.insipidknight.locationservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Location {

    private Long driverId;

    private Double lng;

    private Double lat;

    private LocalDateTime reportTime;
}
