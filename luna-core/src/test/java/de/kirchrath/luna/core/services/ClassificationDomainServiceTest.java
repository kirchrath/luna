package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
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
    private static final OffsetDateTime RECEIVED_AT = OffsetDateTime.parse("2026-11-24T10:00:00+01:00");

    private final InboundEventRepository inboundEventRepository = mock(InboundEventRepository.class);
    private final OutboundEventRepository outboundEventRepository = mock(OutboundEventRepository.class);
    private final ElementClassificationClient elementClassificationClient = mock(ElementClassificationClient.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final List<ElementProcessor<Object, ?, ?>> processors = processors();
    private final ClassificationDomainService classificationDomainService = new ClassificationDomainService(
            inboundEventRepository,
            outboundEventRepository,
            elementClassificationClient,
            List.copyOf(processors),
            new LunaElementTypeConverter(new LunaElementTypeMapperImpl()),
            new InboundEventReader(jsonMapper, new TimeProvider()),
            new ClassificationProperties(Duration.ofSeconds(30), 10, 3),
            eventPublisher);

    @BeforeEach
    void setUp() {
        when(outboundEventRepository.save(any()))
                .thenAnswer(invocation -> invocation.<OutboundEvent>getArgument(0).withId(UUID.randomUUID()));
    }

    @Test
    void thatFindingEventsToClassifyShouldResultIntoTheIdsOfTheOldestReceivedEventsOfOneBatch() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 0);
        when(inboundEventRepository.findByStatusOrderByCreatedDateAsc(InboundEventStatus.RECEIVED, Limit.of(10)))
                .thenReturn(List.of(inboundEvent));

        // when
        var found = classificationDomainService.findInboundEventIdsToClassify();

        // then
        assertThat(found).containsExactly(INBOUND_EVENT_ID);
    }

    @Test
    void thatClassifyingAnInputShouldResultIntoQualifiedStoredAndPublishedElementsAndAnAnalyzedInboundEvent() {
        // given
        var inboundEvent = storedInboundEvent(new ManualInputDataDto("Dentist on Tuesday, buy a gift before", null), 0);
        when(elementClassificationClient.classify("Dentist on Tuesday, buy a gift before", null)).thenReturn(
                new ClassificationResultDto(List.of(LunaElementTypeDto.APPOINTMENT, LunaElementTypeDto.TASK)));
        var appointmentData = qualifies(LunaElementType.APPOINTMENT);
        var taskData = qualifies(LunaElementType.TASK);

        // when
        classificationDomainService.classify(INBOUND_EVENT_ID);

        // then
        var outboundEvents = savedOutboundEvents(2);
        assertThat(outboundEvents).extracting(OutboundEvent::type)
                .containsExactly(LunaElementType.APPOINTMENT, LunaElementType.TASK);
        assertThat(outboundEvents).allSatisfy(outboundEvent -> {
            assertThat(outboundEvent.inboundEventId()).isEqualTo(INBOUND_EVENT_ID);
            assertThat(outboundEvent.status()).isEqualTo(OutboundEventStatus.PUBLISHED);
        });
        verify(processor(LunaElementType.APPOINTMENT)).storeAndPublish(any(), eq(appointmentData));
        verify(processor(LunaElementType.TASK)).storeAndPublish(any(), eq(taskData));
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
        verify(eventPublisher).publishEvent(new InboundEventStatusChangedEvent(INBOUND_EVENT_ID));
    }

    @Test
    void thatClassifyingAnInputWithSeveralElementsOfOneTypeShouldResultIntoOneElementEach() {
        // given
        storedInboundEvent(new ManualInputDataDto("Dentist on Monday, hairdresser on Friday", null), 0);
        when(elementClassificationClient.classify("Dentist on Monday, hairdresser on Friday", null))
                .thenReturn(new ClassificationResultDto(List.of(LunaElementTypeDto.APPOINTMENT)));
        when(processor(LunaElementType.APPOINTMENT).qualify(any(), eq(RECEIVED_AT)))
                .thenReturn(List.of("dentist", "hairdresser"));

        // when
        classificationDomainService.classify(INBOUND_EVENT_ID);

        // then
        assertThat(savedOutboundEvents(2)).extracting(OutboundEvent::type)
                .containsExactly(LunaElementType.APPOINTMENT, LunaElementType.APPOINTMENT);
        verify(processor(LunaElementType.APPOINTMENT)).storeAndPublish(any(), eq("dentist"));
        verify(processor(LunaElementType.APPOINTMENT)).storeAndPublish(any(), eq("hairdresser"));
    }

    @ParameterizedTest
    @EnumSource(LunaElementTypeDto.class)
    void thatClassifyingAnInputWithTypeHintShouldResultIntoAnElementOfTheHintedType(LunaElementTypeDto typeHint) {
        // given
        storedInboundEvent(new ManualInputDataDto("Something", typeHint), 0);
        when(elementClassificationClient.classify("Something", typeHint)).thenReturn(new ClassificationResultDto(List.of()));
        var type = LunaElementType.valueOf(typeHint.name());
        var data = qualifies(type);

        // when
        classificationDomainService.classify(INBOUND_EVENT_ID);

        // then
        assertThat(savedOutboundEvents(1)).extracting(OutboundEvent::type).containsExactly(type);
        verify(processor(type)).storeAndPublish(any(), eq(data));
    }

    @Test
    void thatClassifyingWithAResultWithoutTypesShouldResultIntoNoElementAndAnAnalyzedInboundEvent() {
        // given
        var inboundEvent = storedInboundEvent(new ManualInputDataDto("Something", null), 0);
        when(elementClassificationClient.classify("Something", null)).thenReturn(new ClassificationResultDto(null));

        // when
        classificationDomainService.classify(INBOUND_EVENT_ID);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatClassifyingWithoutResultShouldResultIntoNoElementAndAnAnalyzedInboundEvent() {
        // given
        var inboundEvent = storedInboundEvent(new ManualInputDataDto("Something", null), 0);
        when(elementClassificationClient.classify("Something", null)).thenReturn(null);

        // when
        classificationDomainService.classify(INBOUND_EVENT_ID);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatAFailingQualificationShouldResultIntoNoElementAndAnIncreasedAttempt() {
        // given
        var inboundEvent = storedInboundEvent(new ManualInputDataDto("Something", LunaElementTypeDto.NOTE), 0);
        when(elementClassificationClient.classify("Something", LunaElementTypeDto.NOTE))
                .thenReturn(new ClassificationResultDto(List.of()));
        when(processor(LunaElementType.NOTE).qualify("Something", RECEIVED_AT))
                .thenThrow(new IllegalStateException("No NOTE data could be extracted"));

        // when
        classificationDomainService.classify(INBOUND_EVENT_ID);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(processor(LunaElementType.NOTE), never()).storeAndPublish(any(), any());
        verify(inboundEventRepository).save(inboundEvent.withAttempts(1).withStatus(InboundEventStatus.RECEIVED));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void thatAFailingClassificationAtTheLastAttemptShouldResultIntoAFailedInboundEvent() {
        // given
        var inboundEvent = storedInboundEvent(new ManualInputDataDto("Something", null), 2);
        when(elementClassificationClient.classify("Something", null))
                .thenThrow(new IllegalStateException("provider unavailable"));

        // when
        classificationDomainService.classify(INBOUND_EVENT_ID);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withAttempts(3).withStatus(InboundEventStatus.FAILED));
        verify(eventPublisher).publishEvent(new InboundEventStatusChangedEvent(INBOUND_EVENT_ID));
    }

    @SuppressWarnings("unchecked")
    private static List<ElementProcessor<Object, ?, ?>> processors() {
        return Arrays.stream(LunaElementType.values()).<ElementProcessor<Object, ?, ?>>map(type -> {
            ElementProcessor<Object, ?, ?> processor = mock(ElementProcessor.class);
            when(processor.type()).thenReturn(type);
            return processor;
        }).toList();
    }

    private ElementProcessor<Object, ?, ?> processor(LunaElementType type) {
        return processors.stream().filter(processor -> processor.type() == type).findFirst().orElseThrow();
    }

    private Object qualifies(LunaElementType type) {
        var data = type.name() + " data";
        when(processor(type).qualify(any(), eq(RECEIVED_AT))).thenReturn(List.of(data));
        return data;
    }

    private InboundEvent inboundEvent(ManualInputDataDto input, int attempts) {
        return new InboundEvent(INBOUND_EVENT_ID, EventSource.MANUAL, null, new EventData(jsonMapper.writeValueAsString(input)),
                InboundEventStatus.RECEIVED, attempts, RECEIVED_AT, RECEIVED_AT, 0L);
    }

    private InboundEvent storedInboundEvent(ManualInputDataDto input, int attempts) {
        var inboundEvent = inboundEvent(input, attempts);
        when(inboundEventRepository.findById(INBOUND_EVENT_ID)).thenReturn(Optional.of(inboundEvent));
        return inboundEvent;
    }

    private List<OutboundEvent> savedOutboundEvents(int count) {
        var captor = ArgumentCaptor.forClass(OutboundEvent.class);
        verify(outboundEventRepository, times(count)).save(captor.capture());
        return captor.getAllValues();
    }
}
