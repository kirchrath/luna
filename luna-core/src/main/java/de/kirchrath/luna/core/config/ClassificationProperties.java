package de.kirchrath.luna.core.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.classification")
public record ClassificationProperties(Duration interval, int batchSize, int maxAttempts) {
}
