package com.insipidknight.toarrive.user.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;

import com.insipidknight.toarrive.common.util.GenerateUtil;
import com.insipidknight.toarrive.common.util.JwtUtil;
import com.insipidknight.toarrive.user.dto.LoginDTO;
import com.insipidknight.toarrive.user.dto.RegisterDTO;
import com.insipidknight.toarrive.user.entity.User;
import com.insipidknight.toarrive.user.entity.enums.UserStatus;
import com.insipidknight.toarrive.user.mapper.AuthMapper;
import com.insipidknight.toarrive.user.util.EncryptUtil;
import com.insipidknight.toarrive.user.vo.LoginVO;
import com.insipidknight.toarrive.user.vo.RegisterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthMapper authMapper;
    private final UserProfileService userProfileService;
    private final JwtUtil jwtUtil;
    private final TokenSessionService tokenSessionService;

    @Transactional
    public RegisterVO register(RegisterDTO registerDTO) {

        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        User existUser = authMapper.selectByPhone(registerDTO.getPhone());
        if (existUser != null) {
            throw new BusinessException("该手机号已被注册");
        }

        User user = new User();
        user.setPhone(registerDTO.getPhone());
        user.setPassword(EncryptUtil.encrypt(registerDTO.getPassword()));
        user.setUid(GenerateUtil.generateUid());

        authMapper.insert(user);

        userProfileService.createDefault(user.getUserId(), user.getUid());

        log.info("用户注册成功, userId={}, phone={}", user.getUserId(), user.getPhone());

        RegisterVO vo = new RegisterVO();
        vo.setUserId(user.getUserId());
        return vo;
    }

    public LoginVO login(LoginDTO loginDTO) {

        User user = authMapper.selectByPhone(loginDTO.getPhone());
        if (user == null || !EncryptUtil.verify(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "账号已被禁用");
        }
        if (user.getStatus() == UserStatus.CANCELLED) {
            throw new BusinessException("账号已注销");
        }

        String tokenId = UUID.randomUUID().toString();
        long tokenVersion = tokenSessionService.currentVersion(user.getUserId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("uid", user.getUid());
        claims.put("role", user.getRole().getValue());
        claims.put("jti", tokenId);
        claims.put("ver", tokenVersion);
        String accessToken = jwtUtil.generateJwt(claims);

        tokenSessionService.saveSession(user.getUserId(), tokenId, jwtUtil.getExpiration());

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setExpiresIn(jwtUtil.getExpiration());

        vo.setUser(userProfileService.buildBrief(user));

        log.info("用户登录成功, userId={}, phone={}", user.getUserId(), user.getPhone());

        return vo;
    }

    public void logout(String tokenId) {
        tokenSessionService.removeSession(tokenId);
        log.info("用户登出, tokenId={}", tokenId);
    }

    @Transactional
    public void cancel(Long userId) {

        User user = authMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("账号不存在或已注销");
        }

        user.setStatus(UserStatus.CANCELLED);
        authMapper.updateById(user);

        authMapper.deleteById(userId);
        userProfileService.removeByUserId(userId);
        tokenSessionService.revokeAll(userId);

        log.info("用户注销成功, userId={}, phone={}", user.getUserId(), user.getPhone());
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {

        User user = authMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (!EncryptUtil.verify(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(EncryptUtil.encrypt(newPassword));
        authMapper.updateById(user);
        tokenSessionService.revokeAll(userId);

        log.info("用户修改密码成功, userId={}", userId);
    }


}
