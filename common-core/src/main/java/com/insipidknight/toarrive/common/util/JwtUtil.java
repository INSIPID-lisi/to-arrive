package com.insipidknight.toarrive.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类。实例由 Spring 管理：网关与 user-service 各自注入。
 */
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String mySecret;

    @Value("${app.jwt.expiration}")
    private long expirationTime;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(mySecret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT密钥初始化成功");
    }

    public String generateJwt(Map<String, Object> data) {
        log.debug("开始生成JWT令牌，数据: {}", data);

        return Jwts.builder()
                .signWith(secretKey)
                .claims(data)
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .compact();
    }

    /**
     * 解析令牌的声明，令牌缺失、过期或非法时返回 null
     */
    public Claims parseToken(String token) {
        if (token == null) {
            return null;
        }
        try {
            return parseAndValidateJwt(token);
        } catch (Exception e) {
            log.warn("解析Token失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析和验证 JWT 令牌
     *
     * @param token JWT 令牌字符串
     * @return Claims 对象，包含 JWT 中的所有声明
     */
    public Claims parseAndValidateJwt(String token) {
        log.debug("开始解析和验证JWT令牌");
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 Token 的合法性（签名、有效期等）
     */
    public boolean verifyToken(String token) {
        log.debug("开始校验Token的合法性");

        if (token == null) {
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Token签名无效：{}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Token已过期：{}", e.getMessage());
        } catch (Exception e) {
            log.error("Token格式错误/校验失败：{}", e.getMessage());
        }

        return false;
    }

    /**
     * 令牌有效期（毫秒），对应 app.jwt.expiration 配置
     */
    public long getExpiration() {
        return expirationTime;
    }
}
