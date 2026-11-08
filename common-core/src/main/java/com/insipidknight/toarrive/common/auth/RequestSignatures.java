package com.insipidknight.toarrive.common.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 网关转发与服务间调用的 HMAC-SHA256 签名。
 * 参与签名的字段用 {@code \n} 连接，避免拼接歧义。
 */
public final class RequestSignatures {

    private static final String ALGORITHM = "HmacSHA256";

    private RequestSignatures() {
    }

    /**
     * 生成十六进制签名；密钥未配置时返回 {@code null}，由调用方拒绝该请求。
     */
    public static String sign(String secret, String... canonicalParts) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(String.join("\n", canonicalParts).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 签名失败", e);
        }
    }

    /**
     * 常量时间比较，避免签名被逐字节爆破。
     */
    public static boolean verify(String secret, String signature, String... canonicalParts) {
        String expected = sign(secret, canonicalParts);
        if (expected == null || signature == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
