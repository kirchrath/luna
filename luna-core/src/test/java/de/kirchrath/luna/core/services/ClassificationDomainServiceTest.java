package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Limit;

import de.kirchrath.luna.core.config.ClassificationProperties;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;

class ClassificationDomainServiceTest {

    private static final UUID INBOUND_EVENT_ID = UUID.fromString("3f2b9c1d-7e6a-4b5c-9d8e-1a2b3c4d5e6f");

    private final InboundEventRepository inboundEventRepository = mock(InboundEventRepository.class);
    private final OutboundEventRepository outboundEventRepository = mock(OutboundEventRepository.class);
    private final ClassificationDomainService classificationDomainService = new ClassificationDomainService(
            inboundEventRepository, outboundEventRepository, new ClassificationProperties(Duration.ofSeconds(30), 10, 3));

    @Test
    void thatFindingEventsToClassifyShouldResultIntoTheOldestReceivedEventsOfOneBatch() {
        // given
        var inboundEvent = inboundEvent(0);
        when(inboundEventRepository.findByStatusOrderByCreatedDateAsc(InboundEventStatus.RECEIVED, Limit.of(10)))
                .thenReturn(List.of(inboundEvent));

        // when
        var found = classificationDomainService.findInboundEventsToClassify();

        // then
        assertThat(found).containsExactly(inboundEvent);
    }

    @Test
    void thatCompletingASuccessfulClassificationShouldResultIntoOutboundEventsAndAnAnalyzedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(0);
        var outcome = new ClassificationOutcome(Set.of(LunaElementType.APPOINTMENT), null);

        // when
        classificationDomainService.completeClassification(inboundEvent, outcome);

        // then
        var captor = ArgumentCaptor.forClass(OutboundEvent.class);
        verify(outboundEventRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new OutboundEvent(
                null, INBOUND_EVENT_ID, LunaElementType.APPOINTMENT, OutboundEventStatus.CLASSIFIED, null, null, null));
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatCompletingAClassificationWithoutTypesShouldResultIntoNoOutboundEventAndAnAnalyzedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(0);
        var outcome = new ClassificationOutcome(Set.of(), null);

        // when
        classificationDomainService.completeClassification(inboundEvent, outcome);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatCompletingAFailedClassificationShouldResultIntoAnIncreasedAttemptAndAReceivedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(0);
        var outcome = new ClassificationOutcome(Set.of(), new IllegalStateException("provider unavailable"));

        // when
        classificationDomainService.completeClassification(inboundEvent, outcome);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withAttempts(1).withStatus(InboundEventStatus.RECEIVED));
    }

    @Test
    void thatCompletingAFailedClassificationAtTheLastAttemptShouldResultIntoAFailedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(2);
        var outcome = new ClassificationOutcome(Set.of(), new IllegalStateException("provider unavailable"));

        // when
        classificationDomainService.completeClassification(inboundEvent, outcome);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository, times(1))
                .save(inboundEvent.withAttempts(3).withStatus(InboundEventStatus.FAILED));
    }

    private InboundEvent inboundEvent(int attempts) {
        return new InboundEvent(INBOUND_EVENT_ID, EventSource.MANUAL, new EventData("{}"),
                InboundEventStatus.RECEIVED, attempts, null, null, 0L);
    }
}
