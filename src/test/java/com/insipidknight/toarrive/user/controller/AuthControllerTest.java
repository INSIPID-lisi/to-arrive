package com.insipidknight.toarrive.user.controller;

import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.common.exception.GlobalExceptionHandler;
import com.insipidknight.toarrive.user.service.AuthService;
import com.insipidknight.toarrive.user.vo.LoginVO;
import com.insipidknight.toarrive.user.vo.RegisterVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 只起 Controller 层的 standalone MockMvc，不加载 Spring 上下文（无需 MySQL/Redis）。
 * 注：这里没有注册 JwtAuthInterceptor，所以只覆盖 请求校验 / Result 包装 / 异常映射。
 */
class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("注册成功：Result 包装 code=200，返回 userId")
    void register_success() throws Exception {
        RegisterVO vo = new RegisterVO();
        vo.setUserId(1001L);
        when(authService.register(any())).thenReturn(vo);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800138000","password":"abc123","confirmPassword":"abc123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("1001"));
    }

    @Test
    @DisplayName("注册参数非法：校验在进 service 前拦截，返回 400 + 字段提示")
    void register_invalidPhone_blockedBeforeService() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"123","password":"abc123","confirmPassword":"abc123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("手机号格式错误")));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("登录成功：返回 token 与用户信息")
    void login_success() throws Exception {
        LoginVO vo = new LoginVO();
        vo.setAccessToken("token-value");
        vo.setExpiresIn(43200000L);
        when(authService.login(any())).thenReturn(vo);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800138000","password":"abc123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("token-value"));
    }

    @Test
    @DisplayName("业务异常：映射为错误码 + message")
    void login_businessError() throws Exception {
        when(authService.login(any())).thenThrow(new BusinessException("手机号或密码错误"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800138000","password":"abc123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("手机号或密码错误"));
    }

    @Test
    @DisplayName("注销：从 UserContext 取 userId 透传给 service")
    void cancel_ok() throws Exception {
        UserContext.setUserId(9L);

        mockMvc.perform(post("/api/auth/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).cancel(9L);
    }
}
