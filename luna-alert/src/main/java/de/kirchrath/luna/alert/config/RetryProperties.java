package de.kirchrath.luna.alert.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param retries number of retries of a failed event before it goes to the dead letter topic
 * @param delay   delay between two retries
 */
@ConfigurationProperties("app.kafka.retry")
public record RetryProperties(int retries, Duration delay) {
}
