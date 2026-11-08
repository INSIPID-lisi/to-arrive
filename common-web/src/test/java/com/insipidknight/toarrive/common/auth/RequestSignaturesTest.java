package com.insipidknight.toarrive.common.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestSignaturesTest {

    private static final String SECRET = "test-secret";

    @Test
    void acceptsMatchingSignature() {
        String signature = RequestSignatures.sign(SECRET, "gateway-v1", "1000", "GET", "/api/user", "42");

        assertThat(RequestSignatures.verify(SECRET, signature,
                "gateway-v1", "1000", "GET", "/api/user", "42")).isTrue();
    }

    @Test
    void rejectsTamperedPart() {
        String signature = RequestSignatures.sign(SECRET, "gateway-v1", "1000", "GET", "/api/user", "42");

        assertThat(RequestSignatures.verify(SECRET, signature,
                "gateway-v1", "1000", "GET", "/api/payment", "42")).isFalse();
        assertThat(RequestSignatures.verify(SECRET, signature,
                "gateway-v1", "1000", "GET", "/api/user", "43")).isFalse();
    }

    @Test
    void rejectsDifferentSecret() {
        String signature = RequestSignatures.sign(SECRET, "gateway-v1", "1000", "GET", "/api/user", "");

        assertThat(RequestSignatures.verify("other-secret", signature,
                "gateway-v1", "1000", "GET", "/api/user", "")).isFalse();
    }

    @Test
    void missingSecretYieldsNoSignature() {
        assertThat(RequestSignatures.sign("", "gateway-v1")).isNull();
        assertThat(RequestSignatures.verify("", "anything", "gateway-v1")).isFalse();
    }
}
