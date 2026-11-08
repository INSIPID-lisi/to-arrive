package com.insipidknight.toarrive.common.client;

import com.insipidknight.toarrive.common.auth.RequestSignatures;
import com.insipidknight.toarrive.common.auth.SigningHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalServiceAuthInterceptorTest {

    private static final String URL = "http://user-service/internal/user/42/brief";

    @Test
    void addsVerifiableSignatureHeaders() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create(URL));

        new InternalServiceAuthInterceptor("order-service", "internal-secret")
                .intercept(request, new byte[0],
                        (req, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        String service = request.getHeaders().getFirst(SigningHeaders.INTERNAL_SERVICE);
        String timestamp = request.getHeaders().getFirst(SigningHeaders.INTERNAL_TIMESTAMP);
        String signature = request.getHeaders().getFirst(SigningHeaders.INTERNAL_SIGNATURE);

        assertThat(service).isEqualTo("order-service");
        assertThat(RequestSignatures.verify("internal-secret", signature,
                "internal-v1", service, timestamp, "GET", "/internal/user/42/brief")).isTrue();
    }

    @Test
    void failsWhenSecretMissing() {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create(URL));

        assertThatThrownBy(() -> new InternalServiceAuthInterceptor("order-service", "")
                .intercept(request, new byte[0],
                        (req, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK)))
                .isInstanceOf(IllegalStateException.class);
    }
}
