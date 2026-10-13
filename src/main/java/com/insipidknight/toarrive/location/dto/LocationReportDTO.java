package com.insipidknight.toarrive.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationReportDTO {

    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180", message = "经度超出有效范围")
    @DecimalMax(value = "180", message = "经度超出有效范围")
    private Double lng;

    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90", message = "纬度超出有效范围")
    @DecimalMax(value = "90", message = "纬度超出有效范围")
    private Double lat;
}
