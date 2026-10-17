package com.insipidknight.toarrive.user.controller.admin;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;

import com.insipidknight.toarrive.user.service.DriverService;
import com.insipidknight.toarrive.user.vo.DriverApplicationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/driver")
@RequiredArgsConstructor
public class AdminDriverController {

    private final DriverService driverService;

    @GetMapping("/pending")
    public Result<List<DriverApplicationVO>> pending(@RequestParam(defaultValue = "20") Integer limit) {
        requireAdmin();
        return Result.success(driverService.listPending(limit));
    }

    @PostMapping("/{driverId}/approve")
    public Result<Void> approve(@PathVariable Long driverId) {
        requireAdmin();
        driverService.approve(driverId);
        return Result.success(null);
    }

    @PostMapping("/{driverId}/reject")
    public Result<Void> reject(@PathVariable Long driverId,
                               @RequestParam(required = false) String reason) {
        requireAdmin();
        driverService.reject(driverId, reason);
        return Result.success(null);
    }

    private void requireAdmin() {
        if (UserContext.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行该操作");
        }
    }
}
