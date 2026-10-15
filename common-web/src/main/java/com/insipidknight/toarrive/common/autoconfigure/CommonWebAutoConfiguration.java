package com.insipidknight.toarrive.common.autoconfigure;

import com.insipidknight.toarrive.common.config.WebMvcConfig;
import com.insipidknight.toarrive.common.exception.GlobalExceptionHandler;
import com.insipidknight.toarrive.common.interceptor.JwtAuthInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@AutoConfiguration
@EnableScheduling
public class CommonWebAutoConfiguration {

    @Bean
    public JwtAuthInterceptor jwtAuthInterceptor() {
        return new JwtAuthInterceptor();
    }

    @Bean
    public WebMvcConfig webMvcConfig(JwtAuthInterceptor jwtAuthInterceptor) {
        return new WebMvcConfig(jwtAuthInterceptor);
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    // 雪花主键是 19 位 Long，超出 JS 安全整数范围（2^53），全局序列化为字符串避免前端 JSON.parse 丢精度
    @Bean
    public JsonMapperBuilderCustomizer longAsStringCustomizer() {
        return builder -> builder.addModule(new SimpleModule("longAsString")
                .addSerializer(Long.class, ToStringSerializer.instance));
    }
}
