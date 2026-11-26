package de.kirchrath.luna.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param coreAlert      alert events of luna-core (consumed)
 * @param coreAlertRetry retry topic for failed alert events of luna-core
 * @param dlt           dead letter topic of luna-alert
 * @param alert          alerts of luna-alert (published)
 */
@ConfigurationProperties("app.kafka.topics")
public record TopicProperties(String coreAlert, String coreAlertRetry, String dlt, String alert) {
}
