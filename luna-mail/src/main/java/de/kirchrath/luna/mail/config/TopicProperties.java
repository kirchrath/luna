package de.kirchrath.luna.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param email emails fetched by luna-mail (published)
 */
@ConfigurationProperties("app.kafka.topics")
public record TopicProperties(String email) {
}
