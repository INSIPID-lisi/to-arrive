package com.insipidknight.toarrive.common.interceptor;

import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.common.util.JwtUtil;
import com.insipidknight.toarrive.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 服务自行验签的模式（app.auth.mode=jwt），仅用于迁移期兼容。
 * 最终形态由网关验签，下游使用 {@link GatewayIdentityInterceptor}。
 */
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null || claims.get("userId") == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }

        UserContext.setUserId(((Number) claims.get("userId")).longValue());
        Object role = claims.get("role");
        UserContext.setRole(role == null ? null : UserRole.of(((Number) role).intValue()));
        Object tokenId = claims.get("jti");
        UserContext.setTokenId(tokenId == null ? null : tokenId.toString());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
