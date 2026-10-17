package com.insipidknight.toarrive.user.controller.admin;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;

import com.insipidknight.toarrive.user.entity.enums.UserStatus;
import com.insipidknight.toarrive.user.service.AdminUserService;
import com.insipidknight.toarrive.user.vo.AdminUserVO;
import com.insipidknight.toarrive.user.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Result<PageVO<AdminUserVO>> list(@RequestParam(required = false) UserRole role,
                                            @RequestParam(required = false) UserStatus status,
                                            @RequestParam(required = false) String phone,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "20") Integer size) {
        requireAdmin();
        return Result.success(adminUserService.listUsers(role, status, phone, page, size));
    }

    @PostMapping("/{userId}/disable")
    public Result<Void> disable(@PathVariable Long userId) {
        requireAdmin();
        adminUserService.disable(userId);
        return Result.success(null);
    }

    @PostMapping("/{userId}/enable")
    public Result<Void> enable(@PathVariable Long userId) {
        requireAdmin();
        adminUserService.enable(userId);
        return Result.success(null);
    }

    private void requireAdmin() {
        if (UserContext.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行该操作");
        }
    }
}
