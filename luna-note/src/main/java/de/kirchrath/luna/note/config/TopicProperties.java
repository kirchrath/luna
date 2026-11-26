package de.kirchrath.luna.note.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param coreNote      note events of luna-core (consumed)
 * @param coreNoteRetry retry topic for failed note events of luna-core
 * @param dlt           dead letter topic of luna-note
 * @param note          notes of luna-note (published)
 */
@ConfigurationProperties("app.kafka.topics")
public record TopicProperties(String coreNote, String coreNoteRetry, String dlt, String note) {
}
