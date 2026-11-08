package com.insipidknight.toarrive.common.client;

import com.insipidknight.toarrive.common.auth.RequestSignatures;
import com.insipidknight.toarrive.common.auth.SigningHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 给服务间 HTTP 调用附加内部签名，供下游 {@code /internal/**} 入口校验。
 */
@RequiredArgsConstructor
public class InternalServiceAuthInterceptor implements ClientHttpRequestInterceptor {

    private final String serviceName;
    private final String secret;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        long timestamp = System.currentTimeMillis();
        String path = request.getURI().getPath();
        String signature = RequestSignatures.sign(secret, "internal-v1", serviceName,
                Long.toString(timestamp), request.getMethod().name(), path);
        if (signature == null) {
            throw new IllegalStateException("app.security.internal-auth.secret 未配置，拒绝发起内部调用");
        }

        request.getHeaders().set(SigningHeaders.INTERNAL_SERVICE, serviceName);
        request.getHeaders().set(SigningHeaders.INTERNAL_TIMESTAMP, Long.toString(timestamp));
        request.getHeaders().set(SigningHeaders.INTERNAL_SIGNATURE, signature);
        return execution.execute(request, body);
    }
}
