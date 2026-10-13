package com.insipidknight.toarrive.user.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.common.util.EncryptUtil;
import com.insipidknight.toarrive.common.util.JwtUtil;
import com.insipidknight.toarrive.user.dto.LoginDTO;
import com.insipidknight.toarrive.user.dto.RegisterDTO;
import com.insipidknight.toarrive.user.entity.User;
import com.insipidknight.toarrive.user.entity.enums.UserRole;
import com.insipidknight.toarrive.user.entity.enums.UserStatus;
import com.insipidknight.toarrive.user.mapper.AuthMapper;
import com.insipidknight.toarrive.user.vo.LoginVO;
import com.insipidknight.toarrive.user.vo.RegisterVO;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PHONE = "13800138000";
    private static final String RAW_PASSWORD = "pwd123456";
    private static final String SECRET = "to-arrive-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz";

    @Mock
    private AuthMapper authMapper;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void initJwt() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "mySecret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 60_000L);
        jwtUtil.init();
    }

    // ---------- register ----------

    @Test
    @DisplayName("注册：两次密码不一致 → 直接报错，不落库")
    void register_passwordMismatch() {
        RegisterDTO dto = registerDto(PHONE, "abc123", "abc124");

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("两次输入的密码不一致");

        verify(authMapper, never()).selectByPhone(any());
        verify(authMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册：手机号已存在 → 报错，不落库")
    void register_phoneExists() {
        RegisterDTO dto = registerDto(PHONE, RAW_PASSWORD, RAW_PASSWORD);
        when(authMapper.selectByPhone(PHONE)).thenReturn(new User());

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该手机号已被注册");

        verify(authMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册成功：密码加密落库，按 uid 建默认资料")
    void register_success() {
        RegisterDTO dto = registerDto(PHONE, RAW_PASSWORD, RAW_PASSWORD);
        when(authMapper.selectByPhone(PHONE)).thenReturn(null);
        when(authMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setUserId(1001L);
            return 1;
        });

        RegisterVO vo = authService.register(dto);

        assertThat(vo.getUserId()).isEqualTo(1001L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPhone()).isEqualTo(PHONE);
        assertThat(saved.getUid()).isNotBlank();
        assertThat(saved.getPassword()).isNotEqualTo(RAW_PASSWORD);
        assertThat(EncryptUtil.verify(RAW_PASSWORD, saved.getPassword())).isTrue();

        verify(userProfileService).createDefault(1001L, saved.getUid());
    }

    // ---------- login ----------

    @Test
    @DisplayName("登录：手机号不存在 → 统一报「手机号或密码错误」")
    void login_userNotFound() {
        LoginDTO dto = loginDto(PHONE, RAW_PASSWORD);

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("手机号或密码错误");
    }

    @Test
    @DisplayName("登录：密码错误 → 统一报「手机号或密码错误」")
    void login_wrongPassword() {
        LoginDTO dto = loginDto(PHONE, "wrong-password");
        when(authMapper.selectByPhone(PHONE))
                .thenReturn(user(EncryptUtil.encrypt(RAW_PASSWORD), UserStatus.NORMAL, UserRole.PASSENGER));

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("手机号或密码错误");
    }

    @Test
    @DisplayName("登录：账号被禁用 → ACCOUNT_LOCKED")
    void login_disabled() {
        LoginDTO dto = loginDto(PHONE, RAW_PASSWORD);
        when(authMapper.selectByPhone(PHONE))
                .thenReturn(user(EncryptUtil.encrypt(RAW_PASSWORD), UserStatus.DISABLED, UserRole.PASSENGER));

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ACCOUNT_LOCKED.getCode())
                .hasMessage("账号已被禁用");
    }

    @Test
    @DisplayName("登录：账号已注销 → 报错")
    void login_cancelled() {
        LoginDTO dto = loginDto(PHONE, RAW_PASSWORD);
        when(authMapper.selectByPhone(PHONE))
                .thenReturn(user(EncryptUtil.encrypt(RAW_PASSWORD), UserStatus.CANCELLED, UserRole.PASSENGER));

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已注销");
    }

    @Test
    @DisplayName("登录成功：token 携带库中的 userId / uid / role，并返回用户简要信息")
    void login_success_tokenCarriesDbClaims() {
        LoginDTO dto = loginDto(PHONE, RAW_PASSWORD);
        User stored = user(EncryptUtil.encrypt(RAW_PASSWORD), UserStatus.NORMAL, UserRole.DRIVER);
        stored.setUserId(7L);
        stored.setUid("U00000000007");
        when(authMapper.selectByPhone(PHONE)).thenReturn(stored);

        LoginVO vo = authService.login(dto);

        assertThat(vo.getAccessToken()).isNotBlank();
        assertThat(vo.getExpiresIn()).isEqualTo(JwtUtil.getExpiration());

        Claims claims = JwtUtil.parseToken(vo.getAccessToken());
        assertThat(claims).isNotNull();
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(7L);
        assertThat(claims.get("uid")).isEqualTo("U00000000007");
        assertThat(((Number) claims.get("role")).intValue()).isEqualTo(UserRole.DRIVER.getValue());

        verify(userProfileService).buildBrief(stored);
    }

    @Test
    @DisplayName("角色升级后重新登录：新 token 带新角色（方案 a：靠重登刷 token）")
    void login_afterRoleUpgrade_newTokenHasNewRole() {
        LoginDTO dto = loginDto(PHONE, RAW_PASSWORD);
        User stored = user(EncryptUtil.encrypt(RAW_PASSWORD), UserStatus.NORMAL, UserRole.PASSENGER);
        when(authMapper.selectByPhone(PHONE)).thenReturn(stored);

        Claims before = JwtUtil.parseToken(authService.login(dto).getAccessToken());
        assertThat(((Number) before.get("role")).intValue()).isEqualTo(UserRole.PASSENGER.getValue());

        // 模拟管理员审核通过：库里角色改为司机，司机重新登录
        stored.setRole(UserRole.DRIVER);

        Claims after = JwtUtil.parseToken(authService.login(dto).getAccessToken());
        assertThat(((Number) after.get("role")).intValue()).isEqualTo(UserRole.DRIVER.getValue());
    }

    // ---------- cancel ----------

    @Test
    @DisplayName("注销：用户不存在 → 报错")
    void cancel_userNotFound() {
        when(authMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> authService.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号不存在或已注销");
    }

    @Test
    @DisplayName("注销成功：状态置 CANCELLED，逻辑删除 user，删除 profile")
    void cancel_success() {
        User stored = user("x", UserStatus.NORMAL, UserRole.PASSENGER);
        stored.setUserId(1L);
        when(authMapper.selectById(1L)).thenReturn(stored);

        authService.cancel(1L);

        assertThat(stored.getStatus()).isEqualTo(UserStatus.CANCELLED);
        verify(authMapper).updateById(stored);
        verify(authMapper).deleteById(1L);
        verify(userProfileService).removeByUserId(1L);
    }

    // ---------- changePassword ----------

    @Test
    @DisplayName("改密码：账号不存在 → 报错，不更新")
    void changePassword_userNotFound() {
        when(authMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> authService.changePassword(1L, RAW_PASSWORD, "newpwd123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号不存在");

        verify(authMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("改密码：原密码错误 → 报错，不更新")
    void changePassword_wrongOldPassword() {
        User stored = user(EncryptUtil.encrypt(RAW_PASSWORD), UserStatus.NORMAL, UserRole.PASSENGER);
        stored.setUserId(1L);
        when(authMapper.selectById(1L)).thenReturn(stored);

        assertThatThrownBy(() -> authService.changePassword(1L, "wrong-old", "newpwd123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("原密码错误");

        verify(authMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("改密码成功：密文更新为新密码，新密码可校验、旧密码失效")
    void changePassword_success() {
        User stored = user(EncryptUtil.encrypt(RAW_PASSWORD), UserStatus.NORMAL, UserRole.PASSENGER);
        stored.setUserId(1L);
        when(authMapper.selectById(1L)).thenReturn(stored);

        authService.changePassword(1L, RAW_PASSWORD, "newpwd123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).updateById(captor.capture());
        String encoded = captor.getValue().getPassword();
        assertThat(encoded).isNotEqualTo("newpwd123");
        assertThat(EncryptUtil.verify("newpwd123", encoded)).isTrue();
        assertThat(EncryptUtil.verify(RAW_PASSWORD, encoded)).isFalse();
    }

    // ---------- helpers ----------

    private static RegisterDTO registerDto(String phone, String password, String confirmPassword) {
        RegisterDTO dto = new RegisterDTO();
        dto.setPhone(phone);
        dto.setPassword(password);
        dto.setConfirmPassword(confirmPassword);
        return dto;
    }

    private static LoginDTO loginDto(String phone, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setPhone(phone);
        dto.setPassword(password);
        return dto;
    }

    private static User user(String encodedPassword, UserStatus status, UserRole role) {
        User user = new User();
        user.setPhone(PHONE);
        user.setPassword(encodedPassword);
        user.setStatus(status);
        user.setRole(role);
        user.setUid("U00000000001");
        return user;
    }
}
