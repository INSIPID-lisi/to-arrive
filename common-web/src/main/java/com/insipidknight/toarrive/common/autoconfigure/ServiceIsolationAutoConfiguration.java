package com.insipidknight.toarrive.common.autoconfigure;

import com.insipidknight.toarrive.common.client.InternalServiceAuthInterceptor;
import com.insipidknight.toarrive.common.config.ServiceIsolationProperties;
import com.insipidknight.toarrive.common.filter.ServiceIsolationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * 下游服务入口隔离与内部调用签名装配。
 */
@AutoConfiguration
@EnableConfigurationProperties(ServiceIsolationProperties.class)
public class ServiceIsolationAutoConfiguration {

    @Bean
    ServiceIsolationFilter serviceIsolationFilter(ServiceIsolationProperties properties,
                                                  ObjectMapper objectMapper) {
        return new ServiceIsolationFilter(properties, objectMapper);
    }

    @Bean
    RestClientCustomizer internalServiceAuthCustomizer(ServiceIsolationProperties properties,
                                                       @Value("${spring.application.name:}") String serviceName) {
        return builder -> builder.requestInterceptor(
                new InternalServiceAuthInterceptor(serviceName, properties.internalAuth().secret()));
    }
}
