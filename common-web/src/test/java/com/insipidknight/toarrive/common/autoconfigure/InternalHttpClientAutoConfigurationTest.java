package com.insipidknight.toarrive.common.autoconfigure;

import com.insipidknight.toarrive.common.config.HttpClientPoolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InternalHttpClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    HttpClientAutoConfiguration.class,
                    ImperativeHttpClientAutoConfiguration.class,
                    InternalHttpClientAutoConfiguration.class))
            .withPropertyValues(
                    "spring.http.clients.imperative.factory=http-components",
                    "spring.http.clients.connect-timeout=1s",
                    "spring.http.clients.read-timeout=3s");

    @Test
    void usesApacheHttpClient() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ClientHttpRequestFactory.class);
            assertThat(context.getBean(ClientHttpRequestFactory.class))
                    .isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
        });
    }

    @Test
    void bindsPoolDefaults() {
        contextRunner.run(context -> {
            HttpClientPoolProperties pool = context.getBean(HttpClientPoolProperties.class);
            assertThat(pool.maxTotal()).isEqualTo(100);
            assertThat(pool.maxPerRoute()).isEqualTo(20);
            assertThat(pool.connectionRequestTimeout()).isEqualTo(Duration.ofSeconds(1));
            assertThat(pool.responseTimeout()).isEqualTo(Duration.ofSeconds(5));
        });
    }

    @Test
    void bindsPoolOverrides() {
        contextRunner.withPropertyValues(
                        "app.http.pool.max-total=200",
                        "app.http.pool.max-per-route=50",
                        "app.http.pool.response-timeout=8s")
                .run(context -> {
                    HttpClientPoolProperties pool = context.getBean(HttpClientPoolProperties.class);
                    assertThat(pool.maxTotal()).isEqualTo(200);
                    assertThat(pool.maxPerRoute()).isEqualTo(50);
                    assertThat(pool.responseTimeout()).isEqualTo(Duration.ofSeconds(8));
                });
    }
}
