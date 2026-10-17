package com.insipidknight.toarrive.common.interceptor;

import com.insipidknight.toarrive.common.auth.AuthHeaders;
import com.insipidknight.toarrive.common.context.UserContext;
import com.insipidknight.toarrive.common.enums.UserRole;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * app.auth.mode=gateway 时使用：只信任网关注入的身份头，不再解析 JWT。
 * 缺少身份头说明请求绕过了网关，直接拒绝。
 */
public class GatewayIdentityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(AuthHeaders.USER_ID);
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        UserContext.setUserId(Long.valueOf(userId));

        String role = request.getHeader(AuthHeaders.USER_ROLE);
        if (StringUtils.hasText(role)) {
            UserContext.setRole(UserRole.of(Integer.valueOf(role)));
        }

        String tokenId = request.getHeader(AuthHeaders.TOKEN_ID);
        if (StringUtils.hasText(tokenId)) {
            UserContext.setTokenId(tokenId);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
