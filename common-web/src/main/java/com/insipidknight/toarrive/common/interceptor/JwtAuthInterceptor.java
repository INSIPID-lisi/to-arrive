package com.insipidknight.toarrive.common.interceptor;

import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.common.util.JwtUtil;
import com.insipidknight.toarrive.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        Claims claims = JwtUtil.parseToken(token);
        if (claims == null || claims.get("userId") == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }

        UserContext.setUserId(((Number) claims.get("userId")).longValue());
        Object role = claims.get("role");
        UserContext.setRole(role == null ? null : UserRole.of(((Number) role).intValue()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
