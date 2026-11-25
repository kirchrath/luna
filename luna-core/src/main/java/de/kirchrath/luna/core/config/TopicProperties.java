package de.kirchrath.luna.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.kafka.topics")
public record TopicProperties(
        String task, String alert, String appointment, String invoice, String contract, String note) {
}
