package com.insipidknight.toarrive.common.config;

import com.insipidknight.toarrive.common.interceptor.GatewayIdentityInterceptor;
import com.insipidknight.toarrive.common.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final GatewayIdentityInterceptor gatewayIdentityInterceptor;

    @Value("${app.auth.mode:jwt}")
    private String authMode;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        HandlerInterceptor interceptor = "gateway".equalsIgnoreCase(authMode)
                ? gatewayIdentityInterceptor
                : jwtAuthInterceptor;

        registry.addInterceptor(interceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register");
    }
}
