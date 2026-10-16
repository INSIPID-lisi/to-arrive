package com.insipidknight.toarrive.user.service;

import client.LocationClient;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.user.dto.DriverApplyDTO;
import com.insipidknight.toarrive.user.entity.Driver;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.toarrive.user.entity.enums.VerifyStatus;
import com.insipidknight.toarrive.user.entity.enums.WorkStatus;
import com.insipidknight.toarrive.user.mapper.AuthMapper;
import com.insipidknight.toarrive.user.mapper.DriverMapper;
import com.insipidknight.toarrive.user.vo.DriverApplicationVO;
import com.insipidknight.toarrive.user.vo.DriverApplyVO;
import com.insipidknight.toarrive.user.vo.DriverStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverMapper driverMapper;
    private final AuthMapper authMapper;
    private final LocationClient locationClient;

    public DriverApplyVO apply(Long userId, DriverApplyDTO dto) {
        if (driverMapper.selectByDriverId(userId) != null) {
            throw new BusinessException("您已提交过司机入驻申请，请勿重复提交");
        }

        Driver driver = new Driver();
        driver.setDriverId(userId);
        driver.setRealName(dto.getRealName());
        driver.setIdCardNo(dto.getIdCardNo());
        driver.setLicenseNo(dto.getLicenseNo());
        driver.setPlateNumber(dto.getPlateNumber());
        driver.setVerifyStatus(VerifyStatus.PENDING);
        driver.setWorkStatus(WorkStatus.OFFLINE);

        driverMapper.insert(driver);

        return new DriverApplyVO(driver);
    }

    public List<DriverApplicationVO> listPending(int limit) {
        return driverMapper.selectPending(limit).stream().map(DriverApplicationVO::new).toList();
    }

    public DriverStatusVO getStatus(Long userId) {
        Driver driver = driverMapper.selectByDriverId(userId);
        if (driver == null) {
            throw new BusinessException("您还不是司机，请先提交入驻申请");
        }
        return new DriverStatusVO(driver);
    }

    public DriverStatusVO online(Long userId) {
        Driver driver = requireApproved(userId);
        if (driver.getWorkStatus() != WorkStatus.ONLINE) {
            driverMapper.updateWorkStatus(userId, WorkStatus.ONLINE.getValue());
        }
        return new DriverStatusVO(driverMapper.selectByDriverId(userId));
    }

    public DriverStatusVO offline(Long userId) {
        requireApproved(userId);
        driverMapper.updateWorkStatus(userId, WorkStatus.OFFLINE.getValue());
        locationClient.removeDriver(userId);
        return new DriverStatusVO(driverMapper.selectByDriverId(userId));
    }

    /**
     * 接单前置校验：资质通过且已出车
     */
    public Driver requireOnline(Long driverId) {
        Driver driver = requireApproved(driverId);
        if (driver.getWorkStatus() != WorkStatus.ONLINE) {
            throw new BusinessException("请先出车再接单");
        }
        return driver;
    }

    private Driver requireApproved(Long userId) {
        Driver driver = driverMapper.selectByDriverId(userId);
        if (driver == null) {
            throw new BusinessException("您还不是司机，请先提交入驻申请");
        }
        if (driver.getVerifyStatus() != VerifyStatus.APPROVED) {
            throw new BusinessException("司机资质未通过审核");
        }
        return driver;
    }

    @Transactional
    public void approve(Long driverId) {
        requireApplication(driverId);
        if (driverMapper.review(driverId, VerifyStatus.APPROVED.getValue(), null) == 0) {
            throw new BusinessException("该申请已审核，请勿重复操作");
        }
        authMapper.updateRole(driverId, UserRole.DRIVER.getValue());
    }

    @Transactional
    public void reject(Long driverId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("驳回必须填写原因");
        }
        requireApplication(driverId);
        if (driverMapper.review(driverId, VerifyStatus.REJECTED.getValue(), reason) == 0) {
            throw new BusinessException("该申请已审核，请勿重复操作");
        }
    }

    private void requireApplication(Long driverId) {
        if (driverMapper.selectByDriverId(driverId) == null) {
            throw new BusinessException("入驻申请不存在");
        }
    }
}
