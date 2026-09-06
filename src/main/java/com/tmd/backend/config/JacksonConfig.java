package com.tmd.backend.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer emptyStringCoercion(){
        return jsonMapperBuilder -> jsonMapperBuilder.withCoercionConfig(
            LogicalType.POJO,
            cfg -> cfg.setCoercion(
                CoercionInputShape.EmptyString, CoercionAction.AsEmpty
            )
        );
    }
}
