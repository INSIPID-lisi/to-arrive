package com.insipidknight.toarrive.user.controller;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.user.dto.ChangePasswordDTO;
import com.insipidknight.toarrive.user.dto.LoginDTO;
import com.insipidknight.toarrive.user.dto.RegisterDTO;
import com.insipidknight.toarrive.user.service.AuthService;
import com.insipidknight.toarrive.user.vo.LoginVO;
import com.insipidknight.toarrive.user.vo.RegisterVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO loginDTO) {
        LoginVO vo = authService.login(loginDTO);
        return Result.success(vo);
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
