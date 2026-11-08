package com.insipidknight.toarrive.user.vo;

import com.insipidknight.toarrive.user.entity.Driver;
import com.insipidknight.toarrive.user.entity.enums.VerifyStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DriverApplicationVO {

    private Long driverId;

    private String realName;

    private String idCardNo;

    private String licenseNo;

    private String plateNumber;

    private VerifyStatus verifyStatus;

    private String verifyStatusDesc;

    private LocalDateTime createTime;

    public DriverApplicationVO(Driver driver) {
        this.driverId = driver.getDriverId();
        this.realName = driver.getRealName();
        this.idCardNo = driver.getIdCardNo();
        this.licenseNo = driver.getLicenseNo();
        this.plateNumber = driver.getPlateNumber();
        this.verifyStatus = driver.getVerifyStatus();
        this.verifyStatusDesc = driver.getVerifyStatus().getDesc();
        this.createTime = driver.getCreateTime();
    }
}
