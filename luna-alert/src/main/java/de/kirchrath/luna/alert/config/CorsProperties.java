package de.kirchrath.luna.alert.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.cors")
public record CorsProperties(@DefaultValue List<String> allowedOrigins) {
}
