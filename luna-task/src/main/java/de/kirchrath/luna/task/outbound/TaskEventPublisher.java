package de.kirchrath.luna.task.outbound;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.task.config.TopicProperties;
import de.kirchrath.luna.task.dto.TaskEventDto;
import de.kirchrath.luna.task.services.TaskChangedEvent;
import lombok.extern.slf4j.Slf4j;

/** Publishes the full state of a task on Kafka once its change is committed. */
@Slf4j
@Component
public class TaskEventPublisher {

    private final KafkaTemplate<UUID, TaskEventDto> kafkaTemplate;
    private final TopicProperties topicProperties;

    public TaskEventPublisher(KafkaTemplate<UUID, TaskEventDto> kafkaTemplate, TopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TaskChangedEvent event) {
        var task = event.task();
        kafkaTemplate.send(topicProperties.task(), task.id(), task).join();
        log.debug("Published task {} in version {}", task.id(), task.audit().version());
    }
}
