package com.insipidknight.toarrive.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT工具类
 * 提供JWT令牌的生成和验证功能
 */
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String mySecret;

    @Value("${app.jwt.expiration}")
    private long expirationTime;

    private static SecretKey secretKey;

    /**
     * -- GETTER --
     *  获取令牌有效期（毫秒），对应 app.jwt.expiration 配置
     */
    @Getter
    private static long expiration;

    private static final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(mySecret.getBytes());
        expiration = expirationTime;
        log.info("JWT密钥初始化成功");
    }

    public static String generateJwt(Map<String, Object> data) {
        log.debug("开始生成JWT令牌，数据: {}", data);

        return Jwts.builder()
                .signWith(secretKey)
                .claims(data)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .compact();
    }

    /**
     * 解析令牌的声明，令牌缺失、过期或非法时返回 null
     */
    public static Claims parseToken(String token) {
        if (token == null || blacklistedTokens.contains(token)) {
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
     * 解析和验证JWT令牌
     *
     * @param token JWT令牌字符串
     * @return Claims对象，包含JWT中的所有声明
     */
    public static Claims parseAndValidateJwt(String token) {

        log.debug("开始解析和验证JWT令牌");
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    /**
     * 校验Token的合法性（签名、有效期等）
     * @param token 待校验的JWT Token
     * @return 校验结果
     */
    public static boolean verifyToken(String token) {
        log.debug("开始校验Token的合法性");

        if (token == null || blacklistedTokens.contains(token)) {
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
     * 将Token加入黑名单（登出）
     */
    public static void invalidateToken(String token) {
        if (token != null) {
            blacklistedTokens.add(token);
            log.info("Token已加入黑名单");
        }
    }

}

  