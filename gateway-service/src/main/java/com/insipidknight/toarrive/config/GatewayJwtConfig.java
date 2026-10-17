package com.insipidknight.toarrive.config;

import com.insipidknight.toarrive.common.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关只依赖 common-core，common-web 的自动装配不在 classpath 上，
 * 必须显式注册 JwtUtil，否则密钥不会初始化。
 */
@Configuration
public class GatewayJwtConfig {

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }
}
