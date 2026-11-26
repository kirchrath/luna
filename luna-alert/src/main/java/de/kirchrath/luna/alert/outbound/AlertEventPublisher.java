package de.kirchrath.luna.alert.outbound;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.alert.config.TopicProperties;
import de.kirchrath.luna.alert.dto.AlertEventDto;
import de.kirchrath.luna.alert.services.AlertChangedEvent;
import lombok.extern.slf4j.Slf4j;

/** Publishes the full state of a alert on Kafka once its change is committed. */
@Slf4j
@Component
public class AlertEventPublisher {

    private final KafkaTemplate<UUID, AlertEventDto> kafkaTemplate;
    private final TopicProperties topicProperties;

    public AlertEventPublisher(KafkaTemplate<UUID, AlertEventDto> kafkaTemplate, TopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AlertChangedEvent event) {
        var alert = event.alert();
        kafkaTemplate.send(topicProperties.alert(), alert.id(), alert).join();
        log.debug("Published alert {} in version {}", alert.id(), alert.audit().version());
    }
}
