package de.kirchrath.luna.task.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param coreTask      task events of luna-core (consumed)
 * @param coreTaskRetry retry topic for failed task events of luna-core
 * @param dlt           dead letter topic of luna-task
 * @param task          tasks of luna-task (published)
 */
@ConfigurationProperties("app.kafka.topics")
public record TopicProperties(String coreTask, String coreTaskRetry, String dlt, String task) {
}
