package com.insipidknight.toarrive.common.filter;

import com.insipidknight.toarrive.common.auth.AuthHeaders;
import com.insipidknight.toarrive.common.auth.RequestSignatures;
import com.insipidknight.toarrive.common.auth.SigningHeaders;
import com.insipidknight.toarrive.common.config.ServiceIsolationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceIsolationFilterTest {

    private static final String GATEWAY_SECRET = "gateway-secret";
    private static final String INTERNAL_SECRET = "internal-secret";

    private final ServiceIsolationFilter filter = new ServiceIsolationFilter(
            new ServiceIsolationProperties(
                    new ServiceIsolationProperties.GatewaySignature(GATEWAY_SECRET, Duration.ofSeconds(60)),
                    new ServiceIsolationProperties.InternalAuth(INTERNAL_SECRET, Duration.ofSeconds(60),
                            Map.of("order-service", List.of("/internal/user", "/internal/payment")))),
            JsonMapper.builder().build());

    @Test
    void rejectsDirectApiAccessWithoutGatewaySignature() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsStaleGatewaySignature() throws Exception {
        long stale = System.currentTimeMillis() - Duration.ofMinutes(5).toMillis();
        MockHttpServletRequest request = signedGatewayRequest("GET", "/api/user/profile", "42", stale);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void allowsApiRequestSignedByGateway() throws Exception {
        MockHttpServletRequest request = signedGatewayRequest("GET", "/api/user/profile", "42",
                System.currentTimeMillis());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void allowsAllowlistedInternalCall() throws Exception {
        MockHttpServletRequest request = signedInternalRequest("order-service", "GET",
                "/internal/user/42/brief", System.currentTimeMillis());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void rejectsInternalCallFromUnlistedCaller() throws Exception {
        MockHttpServletRequest request = signedInternalRequest("payment-service", "GET",
                "/internal/user/42/brief", System.currentTimeMillis());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsInternalCallOutsideAllowlist() throws Exception {
        MockHttpServletRequest request = signedInternalRequest("order-service", "GET",
                "/internal/other/42", System.currentTimeMillis());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    private MockHttpServletRequest signedGatewayRequest(String method, String path, String userId, long timestamp) {
        String signature = RequestSignatures.sign(GATEWAY_SECRET, "gateway-v1",
                Long.toString(timestamp), method, path, userId);
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader(AuthHeaders.USER_ID, userId);
        request.addHeader(SigningHeaders.GATEWAY_TIMESTAMP, Long.toString(timestamp));
        request.addHeader(SigningHeaders.GATEWAY_SIGNATURE, signature);
        return request;
    }

    private MockHttpServletRequest signedInternalRequest(String caller, String method, String path, long timestamp) {
        String signature = RequestSignatures.sign(INTERNAL_SECRET, "internal-v1", caller,
                Long.toString(timestamp), method, path);
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader(SigningHeaders.INTERNAL_SERVICE, caller);
        request.addHeader(SigningHeaders.INTERNAL_TIMESTAMP, Long.toString(timestamp));
        request.addHeader(SigningHeaders.INTERNAL_SIGNATURE, signature);
        return request;
    }
}
