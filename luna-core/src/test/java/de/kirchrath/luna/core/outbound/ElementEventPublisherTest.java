package de.kirchrath.luna.core.outbound;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import de.kirchrath.luna.core.dto.AuditDto;
import de.kirchrath.luna.core.dto.ElementEventDto;
import de.kirchrath.luna.core.dto.NoteEventDataDto;
import de.kirchrath.luna.core.dto.NoteEventDto;

class ElementEventPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<UUID, ElementEventDto> kafkaTemplate = mock(KafkaTemplate.class);
    private final ElementEventPublisher elementEventPublisher = new ElementEventPublisher(kafkaTemplate);

    @Test
    @SuppressWarnings("unchecked")
    void thatPublishingAnEventShouldResultIntoAMessageKeyedByTheEventId() {
        // given
        var id = UUID.fromString("6d1f0a3e-3c4b-4f63-8a3e-1e2f3a4b5c6d");
        var now = OffsetDateTime.parse("2026-11-24T10:00:00+01:00");
        var event = new NoteEventDto(id, new AuditDto(now, now, 0L), new NoteEventDataDto("Minutes", "Budget approved"));
        when(kafkaTemplate.send("luna_core_note", id, event))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        elementEventPublisher.publish("luna_core_note", event);

        // then
        verify(kafkaTemplate).send("luna_core_note", id, event);
    }
}
