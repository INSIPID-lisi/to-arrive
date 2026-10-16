package com.insipidknight.toarrive.user.vo;

import com.insipidknight.toarrive.user.entity.Driver;
import com.insipidknight.toarrive.user.entity.enums.VerifyStatus;
import com.insipidknight.toarrive.user.entity.enums.WorkStatus;
import lombok.Data;

@Data
public class DriverStatusVO {

    private Long driverId;

    private String plateNumber;

    private VerifyStatus verifyStatus;
    private String verifyStatusDesc;

    private WorkStatus workStatus;
    private String workStatusDesc;

    private String rejectReason;

    public DriverStatusVO(Driver driver) {
        this.driverId = driver.getDriverId();
        this.plateNumber = driver.getPlateNumber();
        this.verifyStatus = driver.getVerifyStatus();
        this.verifyStatusDesc = driver.getVerifyStatus().getDesc();
        this.workStatus = driver.getWorkStatus();
        this.workStatusDesc = driver.getWorkStatus().getDesc();
        this.rejectReason = driver.getRejectReason();
    }
}
