package com.insipidknight.toarrive.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * 内部 HTTP 调用的连接池与超时参数。
 * 超时时间由 spring.http.clients.* 控制，这里只放连接池和请求级超时。
 */
@ConfigurationProperties(prefix = "app.http.pool")
public record HttpClientPoolProperties(
        @DefaultValue("100") int maxTotal,
        @DefaultValue("20") int maxPerRoute,
        @DefaultValue("1s") Duration connectionRequestTimeout,
        @DefaultValue("5s") Duration responseTimeout) {
}
