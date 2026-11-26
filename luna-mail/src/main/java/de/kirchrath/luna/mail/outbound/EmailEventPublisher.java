package de.kirchrath.luna.mail.outbound;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.mail.config.TopicProperties;
import de.kirchrath.luna.mail.dto.EmailEventDto;
import de.kirchrath.luna.mail.services.EmailSavedEvent;
import lombok.extern.slf4j.Slf4j;

/** Publishes a fetched email on Kafka once it is committed. */
@Slf4j
@Component
public class EmailEventPublisher {

    private final KafkaTemplate<UUID, EmailEventDto> kafkaTemplate;
    private final TopicProperties topicProperties;

    public EmailEventPublisher(KafkaTemplate<UUID, EmailEventDto> kafkaTemplate, TopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(EmailSavedEvent event) {
        var email = event.email();
        kafkaTemplate.send(topicProperties.email(), email.id(), email).join();
        log.debug("Published email {} of account {}", email.id(), email.data().account());
    }
}
