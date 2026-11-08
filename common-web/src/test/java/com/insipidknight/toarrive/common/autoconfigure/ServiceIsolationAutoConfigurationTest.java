package com.insipidknight.toarrive.common.autoconfigure;

import com.insipidknight.toarrive.common.config.ServiceIsolationProperties;
import com.insipidknight.toarrive.common.filter.ServiceIsolationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceIsolationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServiceIsolationAutoConfiguration.class))
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
            .withPropertyValues(
                    "spring.application.name=order-service",
                    "app.security.gateway-signature.secret=gateway-secret",
                    "app.security.internal-auth.secret=internal-secret",
                    "app.security.internal-auth.allowed-calls.order-service[0]=/internal/user");

    @Test
    void bindsPropertiesAndRegistersBeans() {
        contextRunner.run(context -> {
            ServiceIsolationProperties properties = context.getBean(ServiceIsolationProperties.class);

            assertThat(properties.gatewaySignature().secret()).isEqualTo("gateway-secret");
            assertThat(properties.gatewaySignature().maxClockSkew()).isEqualTo(Duration.ofSeconds(60));
            assertThat(properties.internalAuth().allowedCalls().get("order-service"))
                    .containsExactly("/internal/user");

            assertThat(context).hasSingleBean(ServiceIsolationFilter.class);
            assertThat(context).hasSingleBean(RestClientCustomizer.class);
        });
    }

    @Test
    void defaultsToEmptyAllowlistWhenNotConfigured() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServiceIsolationAutoConfiguration.class))
                .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
                .run(context -> {
                    ServiceIsolationProperties properties = context.getBean(ServiceIsolationProperties.class);

                    assertThat(properties.gatewaySignature().secret()).isEmpty();
                    assertThat(properties.internalAuth().allowedCalls()).isEmpty();
                });
    }
}
