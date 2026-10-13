package com.insipidknight.toarrive.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtUtil 的密钥/有效期是静态字段，由 {@code @PostConstruct init()} 从 @Value 注入。
 * 单测里用 ReflectionTestUtils 手动喂入配置再调用 init()，等价于容器启动时的行为。
 */
class JwtUtilTest {

    private static final String SECRET = "to-arrive-test-secret-0123456789-abcdefghijklmnopqrstuvwxyz";
    private static final long EXPIRATION = 60_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "mySecret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", EXPIRATION);
        jwtUtil.init();
    }

    @Test
    @DisplayName("生成后可解析，claim 内容一致")
    void generate_then_parse() {
        String token = JwtUtil.generateJwt(Map.of("userId", 123L, "role", 2));

        Claims claims = JwtUtil.parseToken(token);

        assertThat(claims).isNotNull();
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(123L);
        assertThat(((Number) claims.get("role")).intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("合法 token verifyToken 为 true")
    void verifyToken_valid() {
        String token = JwtUtil.generateJwt(Map.of("userId", 1L));

        assertThat(JwtUtil.verifyToken(token)).isTrue();
    }

    @Test
    @DisplayName("签名不匹配时 verifyToken 为 false、parseToken 为 null")
    void wrongSignature() {
        String token = JwtUtil.generateJwt(Map.of("userId", 1L));

        // 换一把密钥重新初始化，等价于用别的密钥签发的 token
        ReflectionTestUtils.setField(jwtUtil, "mySecret", SECRET.replace('a', 'b'));
        jwtUtil.init();

        assertThat(JwtUtil.verifyToken(token)).isFalse();
        assertThat(JwtUtil.parseToken(token)).isNull();
    }

    @Test
    @DisplayName("null 与非法串 parseToken 返回 null")
    void parseToken_invalid() {
        assertThat(JwtUtil.parseToken(null)).isNull();
        assertThat(JwtUtil.parseToken("not-a-jwt")).isNull();
    }

    @Test
    @DisplayName("已过期 token：parseToken 为 null、verifyToken 为 false")
    void expiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", -1000L);
        jwtUtil.init();
        String expired = JwtUtil.generateJwt(Map.of("userId", 1L));

        assertThat(JwtUtil.parseToken(expired)).isNull();
        assertThat(JwtUtil.verifyToken(expired)).isFalse();
    }

    @Test
    @DisplayName("加入黑名单后失效")
    void invalidatedToken() {
        String token = JwtUtil.generateJwt(Map.of("userId", 1L));
        assertThat(JwtUtil.parseToken(token)).isNotNull();

        JwtUtil.invalidateToken(token);

        assertThat(JwtUtil.parseToken(token)).isNull();
        assertThat(JwtUtil.verifyToken(token)).isFalse();
    }

    @Test
    @DisplayName("getExpiration 返回配置的有效期")
    void getExpiration() {
        assertThat(JwtUtil.getExpiration()).isEqualTo(EXPIRATION);
    }
}
