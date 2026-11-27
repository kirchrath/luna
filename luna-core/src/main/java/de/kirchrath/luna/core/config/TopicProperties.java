package de.kirchrath.luna.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param task           tasks of luna-core (published), likewise {@code alert} to {@code note}
 * @param mailEmail      emails of luna-mail (consumed)
 * @param mailEmailRetry retry topic for failed emails of luna-mail
 * @param dlt            dead letter topic of luna-core
 */
@ConfigurationProperties("app.kafka.topics")
public record TopicProperties(
        String task,
        String alert,
        String appointment,
        String invoice,
        String contract,
        String note,
        String mailEmail,
        String mailEmailRetry,
        String dlt) {
}
