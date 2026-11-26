package de.kirchrath.luna.core.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.kirchrath.luna.core.dto.InboundEventChangedDto;
import io.swagger.v3.core.converter.ModelConverters;

@Configuration
public class OpenApiConfig {

    /**
     * Push messages have no REST path, so springdoc would not include them. Their schemas are added to the
     * components so that the frontend wrapper generates their types as well.
     */
    @Bean
    OpenApiCustomizer pushMessageSchemas(WebSocketProperties webSocketProperties) {
        return openApi -> {
            ModelConverters.getInstance(true).readAll(InboundEventChangedDto.class)
                    .forEach(openApi.getComponents()::addSchemas);
            openApi.getComponents().getSchemas().get(InboundEventChangedDto.class.getSimpleName())
                    .description("Push message via STOMP over WebSocket (endpoint " + webSocketProperties.endpoint()
                            + ") on topic " + webSocketProperties.topics().manualInboundEvents()
                            + " when an inbound event becomes ANALYZED or FAILED.");
        };
    }
}
