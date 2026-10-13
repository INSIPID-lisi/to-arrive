package com.insipidknight.toarrive.common.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Configuration
public class JacksonConfig {

    // 雪花主键是 19 位 Long，超出 JS 安全整数范围（2^53），全局序列化为字符串避免前端 JSON.parse 丢精度
    @Bean
    public JsonMapperBuilderCustomizer longAsStringCustomizer() {
        return builder -> builder.addModule(new SimpleModule("longAsString")
                .addSerializer(Long.class, ToStringSerializer.instance));
    }
}
