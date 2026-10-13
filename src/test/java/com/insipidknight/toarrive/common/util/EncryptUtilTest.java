package com.insipidknight.toarrive.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptUtilTest {

    @Test
    @DisplayName("加密后与明文不同，且能校验通过")
    void encrypt_then_verify() {
        String encoded = EncryptUtil.encrypt("secret123");

        assertThat(encoded).isNotEqualTo("secret123");
        assertThat(EncryptUtil.verify("secret123", encoded)).isTrue();
    }

    @Test
    @DisplayName("错误密码校验不通过")
    void verify_wrongPassword() {
        String encoded = EncryptUtil.encrypt("secret123");

        assertThat(EncryptUtil.verify("secret124", encoded)).isFalse();
    }

    @Test
    @DisplayName("同一明文两次加密结果不同（加盐），但都可校验")
    void encrypt_isSalted() {
        String first = EncryptUtil.encrypt("secret123");
        String second = EncryptUtil.encrypt("secret123");

        assertThat(first).isNotEqualTo(second);
        assertThat(EncryptUtil.verify("secret123", first)).isTrue();
        assertThat(EncryptUtil.verify("secret123", second)).isTrue();
    }
}
