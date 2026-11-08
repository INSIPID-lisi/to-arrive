package com.insipidknight.toarrive.common.autoconfigure;

import com.insipidknight.toarrive.common.config.HttpClientPoolProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * 把 Spring Boot 自动装配的 RestClient 切换成 Apache HttpClient5，并注入连接池参数。
 * 切换本身由 spring.http.clients.imperative.factory=http-components 完成。
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.apache.hc.client5.http.impl.classic.HttpClients")
@EnableConfigurationProperties(HttpClientPoolProperties.class)
public class InternalHttpClientAutoConfiguration {

    @Bean
    ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder> apacheConnectionPoolCustomizer(
            HttpClientPoolProperties pool) {
        return builder -> builder
                .withConnectionManagerCustomizer(manager -> manager
                        .setMaxConnTotal(pool.maxTotal())
                        .setMaxConnPerRoute(pool.maxPerRoute()))
                .withDefaultRequestConfigCustomizer(request -> request
                        .setConnectionRequestTimeout(pool.connectionRequestTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .setResponseTimeout(pool.responseTimeout().toMillis(), TimeUnit.MILLISECONDS));
    }
}
