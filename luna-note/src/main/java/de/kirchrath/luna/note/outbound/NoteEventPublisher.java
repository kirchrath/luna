package de.kirchrath.luna.note.outbound;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.kirchrath.luna.note.config.TopicProperties;
import de.kirchrath.luna.note.dto.NoteEventDto;
import de.kirchrath.luna.note.services.NoteChangedEvent;
import lombok.extern.slf4j.Slf4j;

/** Publishes the full state of a note on Kafka once its change is committed. */
@Slf4j
@Component
public class NoteEventPublisher {

    private final KafkaTemplate<UUID, NoteEventDto> kafkaTemplate;
    private final TopicProperties topicProperties;

    public NoteEventPublisher(KafkaTemplate<UUID, NoteEventDto> kafkaTemplate, TopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(NoteChangedEvent event) {
        var note = event.note();
        kafkaTemplate.send(topicProperties.note(), note.id(), note).join();
        log.debug("Published note {} in version {}", note.id(), note.audit().version());
    }
}
