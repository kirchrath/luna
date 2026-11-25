package de.kirchrath.luna.core.outbound;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.dto.ElementEventDto;

@Component
public class ElementEventPublisher {

    private final KafkaTemplate<UUID, ElementEventDto> kafkaTemplate;

    public ElementEventPublisher(KafkaTemplate<UUID, ElementEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, ElementEventDto event) {
        kafkaTemplate.send(topic, event.id(), event).join();
    }
}
