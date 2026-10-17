package com.insipidknight.toarrive.user.controller;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.user.dto.DriverApplyDTO;

import com.insipidknight.toarrive.user.service.DriverService;
import com.insipidknight.toarrive.user.vo.DriverApplyVO;
import com.insipidknight.toarrive.user.vo.DriverStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping("/apply")
    public Result<DriverApplyVO> apply(@RequestBody @Valid DriverApplyDTO dto) {
        DriverApplyVO vo = driverService.apply(UserContext.getUserId(), dto);
        return Result.success(vo);
    }

    @GetMapping("/me")
    public Result<DriverStatusVO> me() {
        requireDriver();
        return Result.success(driverService.getStatus(UserContext.getUserId()));
    }

    @PostMapping("/online")
    public Result<DriverStatusVO> online() {
        requireDriver();
        return Result.success(driverService.online(UserContext.getUserId()));
    }

    @PostMapping("/offline")
    public Result<DriverStatusVO> offline() {
        requireDriver();
        return Result.success(driverService.offline(UserContext.getUserId()));
    }

    private void requireDriver() {
        if (UserContext.getRole() != UserRole.DRIVER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅司机可执行该操作");
        }
    }
}
