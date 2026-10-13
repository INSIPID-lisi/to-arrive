package com.insipidknight.toarrive.user.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.location.service.LocationService;
import com.insipidknight.toarrive.user.entity.Driver;
import com.insipidknight.toarrive.user.entity.enums.VerifyStatus;
import com.insipidknight.toarrive.user.entity.enums.WorkStatus;
import com.insipidknight.toarrive.user.mapper.AuthMapper;
import com.insipidknight.toarrive.user.mapper.DriverMapper;
import com.insipidknight.toarrive.user.vo.DriverStatusVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    private static final Long DRIVER_ID = 1L;

    @Mock
    private DriverMapper driverMapper;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private DriverService driverService;

    // ---------- online ----------

    @Test
    @DisplayName("出车：尚无入驻记录 → 报错，不改工作状态")
    void online_notADriver() {
        when(driverMapper.selectByDriverId(DRIVER_ID)).thenReturn(null);

        assertThatThrownBy(() -> driverService.online(DRIVER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("您还不是司机，请先提交入驻申请");

        verify(driverMapper, never()).updateWorkStatus(DRIVER_ID, WorkStatus.ONLINE.getValue());
        verifyNoInteractions(locationService);
    }

    @Test
    @DisplayName("出车：资质未通过审核 → 报错，不改工作状态")
    void online_notApproved() {
        when(driverMapper.selectByDriverId(DRIVER_ID))
                .thenReturn(driver(VerifyStatus.PENDING, WorkStatus.OFFLINE));

        assertThatThrownBy(() -> driverService.online(DRIVER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("司机资质未通过审核");

        verify(driverMapper, never()).updateWorkStatus(DRIVER_ID, WorkStatus.ONLINE.getValue());
    }

    @Test
    @DisplayName("出车成功：已通过且收车 → 工作状态置出车")
    void online_success() {
        when(driverMapper.selectByDriverId(DRIVER_ID))
                .thenReturn(driver(VerifyStatus.APPROVED, WorkStatus.OFFLINE),
                        driver(VerifyStatus.APPROVED, WorkStatus.ONLINE));

        DriverStatusVO vo = driverService.online(DRIVER_ID);

        assertThat(vo.getWorkStatus()).isEqualTo(WorkStatus.ONLINE);
        verify(driverMapper).updateWorkStatus(DRIVER_ID, WorkStatus.ONLINE.getValue());
        verifyNoInteractions(locationService);
    }

    @Test
    @DisplayName("出车幂等：已出车 → 不重复写库")
    void online_alreadyOnline() {
        when(driverMapper.selectByDriverId(DRIVER_ID))
                .thenReturn(driver(VerifyStatus.APPROVED, WorkStatus.ONLINE));

        DriverStatusVO vo = driverService.online(DRIVER_ID);

        assertThat(vo.getWorkStatus()).isEqualTo(WorkStatus.ONLINE);
        verify(driverMapper, never()).updateWorkStatus(DRIVER_ID, WorkStatus.ONLINE.getValue());
    }

    // ---------- offline ----------

    @Test
    @DisplayName("收车成功：工作状态置收车，并从 GEO 索引摘除")
    void offline_success() {
        when(driverMapper.selectByDriverId(DRIVER_ID))
                .thenReturn(driver(VerifyStatus.APPROVED, WorkStatus.ONLINE),
                        driver(VerifyStatus.APPROVED, WorkStatus.OFFLINE));

        DriverStatusVO vo = driverService.offline(DRIVER_ID);

        assertThat(vo.getWorkStatus()).isEqualTo(WorkStatus.OFFLINE);
        verify(driverMapper).updateWorkStatus(DRIVER_ID, WorkStatus.OFFLINE.getValue());
        verify(locationService).removeDriver(DRIVER_ID);
    }

    // ---------- requireOnline ----------

    @Test
    @DisplayName("接单前置校验：已通过但收车 → 报「请先出车」")
    void requireOnline_offline() {
        when(driverMapper.selectByDriverId(DRIVER_ID))
                .thenReturn(driver(VerifyStatus.APPROVED, WorkStatus.OFFLINE));

        assertThatThrownBy(() -> driverService.requireOnline(DRIVER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先出车再接单");
    }

    @Test
    @DisplayName("接单前置校验：已通过且出车 → 放行")
    void requireOnline_ok() {
        when(driverMapper.selectByDriverId(DRIVER_ID))
                .thenReturn(driver(VerifyStatus.APPROVED, WorkStatus.ONLINE));

        assertThat(driverService.requireOnline(DRIVER_ID).getWorkStatus()).isEqualTo(WorkStatus.ONLINE);
    }

    // ---------- helpers ----------

    private static Driver driver(VerifyStatus verifyStatus, WorkStatus workStatus) {
        Driver driver = new Driver();
        driver.setDriverId(DRIVER_ID);
        driver.setPlateNumber("京A12345");
        driver.setVerifyStatus(verifyStatus);
        driver.setWorkStatus(workStatus);
        return driver;
    }
}
