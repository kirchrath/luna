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
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Limit;

import de.kirchrath.luna.core.config.ClassificationProperties;
import de.kirchrath.luna.core.converter.LunaElementTypeConverter;
import de.kirchrath.luna.core.converter.LunaElementTypeMapperImpl;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.ClassificationResultDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.outbound.ElementClassificationClient;
import tools.jackson.databind.json.JsonMapper;

class ClassificationDomainServiceTest {

    private static final UUID INBOUND_EVENT_ID = UUID.fromString("3f2b9c1d-7e6a-4b5c-9d8e-1a2b3c4d5e6f");

    private final InboundEventRepository inboundEventRepository = mock(InboundEventRepository.class);
    private final OutboundEventRepository outboundEventRepository = mock(OutboundEventRepository.class);
    private final ElementClassificationClient elementClassificationClient = mock(ElementClassificationClient.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ClassificationDomainService classificationDomainService = new ClassificationDomainService(
            inboundEventRepository,
            outboundEventRepository,
            elementClassificationClient,
            new LunaElementTypeConverter(new LunaElementTypeMapperImpl()),
            jsonMapper,
            new ClassificationProperties(Duration.ofSeconds(30), 10, 3));

    @Test
    void thatFindingEventsToClassifyShouldResultIntoTheOldestReceivedEventsOfOneBatch() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 0);
        when(inboundEventRepository.findByStatusOrderByCreatedDateAsc(InboundEventStatus.RECEIVED, Limit.of(10)))
                .thenReturn(List.of(inboundEvent));

        // when
        var found = classificationDomainService.findInboundEventsToClassify();

        // then
        assertThat(found).containsExactly(inboundEvent);
    }

    @Test
    void thatClassifyingAnInputShouldResultIntoOneOutboundEventPerTypeAndAnAnalyzedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Dentist on Tuesday, buy a gift before", null), 0);
        when(elementClassificationClient.classify("Dentist on Tuesday, buy a gift before", null)).thenReturn(
                new ClassificationResultDto(List.of(LunaElementTypeDto.APPOINTMENT, LunaElementTypeDto.TASK)));

        // when
        classificationDomainService.classify(inboundEvent);

        // then
        assertThat(savedOutboundEventTypes(2)).containsExactly(LunaElementType.APPOINTMENT, LunaElementType.TASK);
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatClassifyingAnInputWithTypeHintShouldResultIntoTheHintedTypeAndFurtherDeterminedTypes() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Bill", LunaElementTypeDto.INVOICE), 0);
        when(elementClassificationClient.classify("Bill", LunaElementTypeDto.INVOICE)).thenReturn(
                new ClassificationResultDto(List.of(LunaElementTypeDto.INVOICE, LunaElementTypeDto.CONTRACT)));

        // when
        classificationDomainService.classify(inboundEvent);

        // then
        assertThat(savedOutboundEventTypes(2)).containsExactly(LunaElementType.INVOICE, LunaElementType.CONTRACT);
    }

    @ParameterizedTest
    @EnumSource(LunaElementTypeDto.class)
    void thatClassifyingAnInputWithTypeHintShouldResultIntoTheMatchingEntityType(LunaElementTypeDto typeHint) {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", typeHint), 0);
        when(elementClassificationClient.classify("Something", typeHint)).thenReturn(new ClassificationResultDto(List.of()));

        // when
        classificationDomainService.classify(inboundEvent);

        // then
        assertThat(savedOutboundEventTypes(1)).containsExactly(LunaElementType.valueOf(typeHint.name()));
    }

    @Test
    void thatClassifyingWithAResultWithoutTypesShouldResultIntoNoOutboundEventAndAnAnalyzedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 0);
        when(elementClassificationClient.classify("Something", null)).thenReturn(new ClassificationResultDto(null));

        // when
        classificationDomainService.classify(inboundEvent);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatClassifyingWithoutResultShouldResultIntoNoOutboundEventAndAnAnalyzedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 0);
        when(elementClassificationClient.classify("Something", null)).thenReturn(null);

        // when
        classificationDomainService.classify(inboundEvent);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatAFailingClassificationShouldResultIntoAnIncreasedAttemptAndAReceivedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 0);
        when(elementClassificationClient.classify("Something", null))
                .thenThrow(new IllegalStateException("provider unavailable"));

        // when
        classificationDomainService.classify(inboundEvent);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withAttempts(1).withStatus(InboundEventStatus.RECEIVED));
    }

    @Test
    void thatAFailingClassificationAtTheLastAttemptShouldResultIntoAFailedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 2);
        when(elementClassificationClient.classify("Something", null))
                .thenThrow(new IllegalStateException("provider unavailable"));

        // when
        classificationDomainService.classify(inboundEvent);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withAttempts(3).withStatus(InboundEventStatus.FAILED));
    }

    private InboundEvent inboundEvent(ManualInputDataDto input, int attempts) {
        return new InboundEvent(INBOUND_EVENT_ID, EventSource.MANUAL, new EventData(jsonMapper.writeValueAsString(input)),
                InboundEventStatus.RECEIVED, attempts, null, null, 0L);
    }

    private List<LunaElementType> savedOutboundEventTypes(int count) {
        var captor = ArgumentCaptor.forClass(OutboundEvent.class);
        verify(outboundEventRepository, times(count)).save(captor.capture());
        captor.getAllValues().forEach(outboundEvent -> {
            assertThat(outboundEvent.id()).isNull();
            assertThat(outboundEvent.inboundEventId()).isEqualTo(INBOUND_EVENT_ID);
            assertThat(outboundEvent.status()).isEqualTo(OutboundEventStatus.CLASSIFIED);
        });
        return captor.getAllValues().stream().map(OutboundEvent::type).toList();
    }
}
