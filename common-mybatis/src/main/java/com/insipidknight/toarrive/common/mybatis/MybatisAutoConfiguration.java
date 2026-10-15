package com.insipidknight.toarrive.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class MybatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public TimeFillObjectHandler timeFillObjectHandler() {
        return new TimeFillObjectHandler();
    }
}
