package de.kirchrath.luna.task.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties({WebSocketProperties.class, CorsProperties.class})
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties webSocketProperties;
    private final CorsProperties corsProperties;

    public WebSocketConfig(WebSocketProperties webSocketProperties, CorsProperties corsProperties) {
        this.webSocketProperties = webSocketProperties;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(webSocketProperties.endpoint())
                .setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(webSocketProperties.brokerPrefix());
    }
}
