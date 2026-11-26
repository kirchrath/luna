package de.kirchrath.luna.alert.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.kirchrath.luna.alert.dto.AlertChangedDto;
import io.swagger.v3.core.converter.ModelConverters;

@Configuration
public class OpenApiConfig {

    /** Push messages have no REST path; their schemas are added so that the frontend wrapper generates their types. */
    @Bean
    OpenApiCustomizer pushMessageSchemas() {
        return openApi -> ModelConverters.getInstance(true).readAll(AlertChangedDto.class)
                .forEach(openApi.getComponents()::addSchemas);
    }
}
