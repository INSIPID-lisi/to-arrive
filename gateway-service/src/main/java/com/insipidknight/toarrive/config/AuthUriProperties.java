package com.insipidknight.toarrive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.uri")
@Data
public class AuthUriProperties {

    /**
     * 免认证路径（PathPattern 语法）。PathPattern 不是可配置类型，
     * 由 AuthGlobalFilter 在启动时统一编译。
     */
    private List<String> whiteList = new ArrayList<>();
}
