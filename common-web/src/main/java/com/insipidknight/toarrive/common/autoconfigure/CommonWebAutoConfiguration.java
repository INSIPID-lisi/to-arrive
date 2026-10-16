package com.insipidknight.toarrive.common.autoconfigure;

import com.insipidknight.toarrive.common.config.WebMvcConfig;
import com.insipidknight.toarrive.common.exception.GlobalExceptionHandler;
import com.insipidknight.toarrive.common.interceptor.JwtAuthInterceptor;
import com.insipidknight.toarrive.common.util.JwtUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@AutoConfiguration
@EnableScheduling
public class CommonWebAutoConfiguration {

    // JwtUtil 在 common-core 的 brother 包里，服务组件扫描扫不到，必须在这里显式注册，
    // 否则 @PostConstruct 不执行、签名密钥为 null，登录/鉴权会在运行时失败
    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

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
