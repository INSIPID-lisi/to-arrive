package com.insipidknight.toarrive.user.vo;

import com.insipidknight.toarrive.user.entity.Driver;
import com.insipidknight.toarrive.user.entity.enums.VerifyStatus;
import lombok.Data;

@Data
public class DriverApplyVO {

    private Long driverId;

    private VerifyStatus verifyStatus;

    private String verifyStatusDesc;

    public DriverApplyVO(Driver driver) {
        this.driverId = driver.getDriverId();
        this.verifyStatus = driver.getVerifyStatus();
        this.verifyStatusDesc = driver.getVerifyStatus().getDesc();
    }
}
