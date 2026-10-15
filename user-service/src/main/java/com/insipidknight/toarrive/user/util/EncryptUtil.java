package com.insipidknight.toarrive.user.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EncryptUtil {
    private static final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public static String encrypt(String rawPassword) {
        log.debug("开始进行Argon2加密");
        return encoder.encode(rawPassword);
    }

    public static boolean verify(String rawPassword, String encodedPassword) {
        log.debug("开始验证密码");
        return encoder.matches(rawPassword, encodedPassword);
    }
}
