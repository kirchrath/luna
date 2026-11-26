package de.kirchrath.luna.appointment.outbound;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.appointment.config.TopicProperties;
import de.kirchrath.luna.appointment.dto.AppointmentEventDto;
import de.kirchrath.luna.appointment.services.AppointmentChangedEvent;
import lombok.extern.slf4j.Slf4j;

/** Publishes the full state of a appointment on Kafka once its change is committed. */
@Slf4j
@Component
public class AppointmentEventPublisher {

    private final KafkaTemplate<UUID, AppointmentEventDto> kafkaTemplate;
    private final TopicProperties topicProperties;

    public AppointmentEventPublisher(KafkaTemplate<UUID, AppointmentEventDto> kafkaTemplate, TopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AppointmentChangedEvent event) {
        var appointment = event.appointment();
        kafkaTemplate.send(topicProperties.appointment(), appointment.id(), appointment).join();
        log.debug("Published appointment {} in version {}", appointment.id(), appointment.audit().version());
    }
}
