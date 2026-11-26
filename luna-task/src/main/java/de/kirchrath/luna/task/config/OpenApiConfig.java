package de.kirchrath.luna.task.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.kirchrath.luna.task.dto.TaskChangedDto;
import io.swagger.v3.core.converter.ModelConverters;

@Configuration
public class OpenApiConfig {

    /** Push messages have no REST path; their schemas are added so that the frontend wrapper generates their types. */
    @Bean
    OpenApiCustomizer pushMessageSchemas() {
        return openApi -> ModelConverters.getInstance(true).readAll(TaskChangedDto.class)
                .forEach(openApi.getComponents()::addSchemas);
    }
}
