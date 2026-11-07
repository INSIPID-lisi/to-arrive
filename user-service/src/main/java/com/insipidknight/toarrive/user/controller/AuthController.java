package com.insipidknight.toarrive.user.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.user.dto.ChangePasswordDTO;
import com.insipidknight.toarrive.user.dto.LoginDTO;
import com.insipidknight.toarrive.user.dto.RegisterDTO;
import com.insipidknight.toarrive.user.service.AuthService;
import com.insipidknight.toarrive.user.vo.LoginVO;
import com.insipidknight.toarrive.user.vo.RegisterVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<RegisterVO> register(@RequestBody @Valid RegisterDTO registerDTO) {
        RegisterVO vo = authService.register(registerDTO);
        return Result.success(vo);
    }

    @PostMapping("/login")
    @SentinelResource(value = "auth:login", blockHandler = "loginBlocked")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO loginDTO) {
        LoginVO vo = authService.login(loginDTO);
        return Result.success(vo);
    }

    public Result<LoginVO> loginBlocked(LoginDTO loginDTO, BlockException e) {
        log.warn("登录熔断降级: {}", e.getClass().getSimpleName());
        return Result.error(ErrorCode.SERVICE_UNAVAILABLE, "登录请求繁忙，请稍后重试");
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(UserContext.getTokenId());
        return Result.success(null);
    }

    @PostMapping("/cancel")
    public Result<Void> cancel() {
        authService.cancel(UserContext.getUserId());
        return Result.success(null);
    }

    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordDTO changePasswordDTO) {
        authService.changePassword(UserContext.getUserId(),
                changePasswordDTO.getOldPassword(), changePasswordDTO.getNewPassword());
        return Result.success(null);
    }


}
