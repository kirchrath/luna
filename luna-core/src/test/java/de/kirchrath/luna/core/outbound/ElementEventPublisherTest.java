package de.kirchrath.luna.core.outbound;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import tools.jackson.databind.json.JsonMapper;

class ElementEventPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final ElementEventPublisher elementEventPublisher =
            new ElementEventPublisher(kafkaTemplate, JsonMapper.builder().build());

    @Test
    void thatPublishingAnEventShouldResultIntoAJsonMessageWithTheKey() {
        // given
        var key = UUID.fromString("6d1f0a3e-3c4b-4f63-8a3e-1e2f3a4b5c6d");
        record Event(String title) {
        }
        when(kafkaTemplate.send("luna_core_note", key.toString(), "{\"title\":\"Minutes\"}"))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        elementEventPublisher.publish("luna_core_note", key, new Event("Minutes"));

        // then
        verify(kafkaTemplate).send("luna_core_note", key.toString(), "{\"title\":\"Minutes\"}");
    }
}
