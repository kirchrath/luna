package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import de.kirchrath.luna.core.converter.InboundEventConverter;
import de.kirchrath.luna.core.converter.InboundEventMapperImpl;
import de.kirchrath.luna.core.converter.LifeAreaConverter;
import de.kirchrath.luna.core.converter.LifeAreaMapperImpl;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.ElementReferenceDto;
import de.kirchrath.luna.core.dto.InboundEventSourceDto;
import de.kirchrath.luna.core.dto.InboundEventStatusDto;
import de.kirchrath.luna.core.dto.LifeAreaDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDataDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDto;
import tools.jackson.databind.json.JsonMapper;

class InboundEventDomainServiceTest {

    private static final UUID ID = UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11");
    private static final UUID EMAIL_ID = UUID.fromString("5e6f7a8b-9c0d-4e1f-a2b3-c4d5e6f7a8b9");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-25T10:00:00+01:00");

    private final InboundEventRepository inboundEventRepository = mock(InboundEventRepository.class);
    private final OutboundEventRepository outboundEventRepository = mock(OutboundEventRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final InboundEventDomainService inboundEventDomainService = new InboundEventDomainService(
            inboundEventRepository,
            outboundEventRepository,
            new InboundEventConverter(new InboundEventMapperImpl()),
            new InboundEventReader(jsonMapper, new TimeProvider()),
            new LifeAreaConverter(new LifeAreaMapperImpl()),
            jsonMapper,
            eventPublisher);

    @Test
    void thatCreatingAManualInboundEventShouldResultIntoAStoredReceivedEventAndItsReference() {
        // given
        when(inboundEventRepository.save(any()))
                .thenAnswer(invocation -> invocation.<InboundEvent>getArgument(0).withId(ID));
        var input = new ManualInputDataDto("Pay the electricity bill", LifeAreaDto.PERSONAL, LunaElementTypeDto.TASK);

        // when
        var reference = inboundEventDomainService.createManualInboundEvent(input);

        // then
        assertThat(reference.id()).isEqualTo(ID);
        var captor = ArgumentCaptor.forClass(InboundEvent.class);
        verify(inboundEventRepository).save(captor.capture());
        var stored = captor.getValue();
        assertThat(stored.id()).isNull();
        assertThat(stored.source()).isEqualTo(EventSource.MANUAL);
        assertThat(stored.sourceId()).isNull();
        assertThat(stored.area()).isEqualTo(LifeArea.PERSONAL);
        assertThat(jsonMapper.readValue(stored.data().json(), ManualInputDataDto.class)).isEqualTo(input);
        assertThat(stored.status()).isEqualTo(InboundEventStatus.RECEIVED);
        assertThat(stored.attempts()).isZero();
        assertThat(stored.createdDate()).isNull();
        assertThat(stored.lastModifiedDate()).isNull();
        assertThat(stored.version()).isNull();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void thatCreatingAMailInboundEventShouldResultIntoAStoredReceivedEventAndASignal() {
        // given
        when(inboundEventRepository.save(any()))
                .thenAnswer(invocation -> invocation.<InboundEvent>getArgument(0).withId(ID));
        var email = new EmailEventDto().id(EMAIL_ID).data(email("Invoice 4711"));

        // when
        inboundEventDomainService.createMailInboundEvent(email);

        // then
        var captor = ArgumentCaptor.forClass(InboundEvent.class);
        verify(inboundEventRepository).save(captor.capture());
        var stored = captor.getValue();
        assertThat(stored.source()).isEqualTo(EventSource.MAIL);
        assertThat(stored.sourceId()).isEqualTo(EMAIL_ID);
        assertThat(stored.area()).isEqualTo(LifeArea.WORK);
        assertThat(jsonMapper.readValue(stored.data().json(), EmailEventDataDto.class).getSubject())
                .isEqualTo("Invoice 4711");
        assertThat(stored.status()).isEqualTo(InboundEventStatus.RECEIVED);
        verify(eventPublisher).publishEvent(new InboundEventStatusChangedEvent(ID));
    }

    @Test
    void thatCreatingAKnownMailInboundEventAgainShouldResultIntoNoSecondEvent() {
        // given
        when(inboundEventRepository.existsBySourceAndSourceId(EventSource.MAIL, EMAIL_ID)).thenReturn(true);

        // when
        inboundEventDomainService.createMailInboundEvent(new EmailEventDto().id(EMAIL_ID).data(email("Again")));

        // then
        verify(inboundEventRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void thatListingInboundEventsShouldResultIntoThePageNewestFirstWithTheElementsOfEachEvent() {
        // given
        var receivedId = UUID.fromString("9a4c2e1b-8d7f-4e3a-b2c1-0f9e8d7c6b5a");
        var taskId = UUID.fromString("6d1f0a3e-3c4b-4f63-8a3e-1e2f3a4b5c6d");
        var analyzed = new InboundEvent(ID, EventSource.MANUAL, null, LifeArea.PERSONAL, new EventData(jsonMapper
                        .writeValueAsString(new ManualInputDataDto("Pay the invoice", LifeAreaDto.PERSONAL, LunaElementTypeDto.TASK))),
                InboundEventStatus.ANALYZED, 1, NOW, NOW, 1L);
        var received = new InboundEvent(receivedId, EventSource.MAIL, EMAIL_ID, LifeArea.WORK,
                new EventData(jsonMapper.writeValueAsString(email("Invoice 4711"))),
                InboundEventStatus.RECEIVED, 0, NOW, NOW, 0L);
        var pageRequest = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdDate"));
        when(inboundEventRepository.findAll(pageRequest))
                .thenReturn(new PageImpl<>(List.of(received, analyzed), pageRequest, 5));
        when(outboundEventRepository.findByInboundEventIdIn(List.of(receivedId, ID))).thenReturn(List.of(
                new OutboundEvent(
                        taskId, ID, LunaElementType.TASK, LifeArea.PERSONAL, OutboundEventStatus.PUBLISHED, NOW, NOW, 0L)));

        // when
        var list = inboundEventDomainService.listInboundEvents(null, PageRequest.of(1, 2, Sort.by("text")));

        // then
        assertThat(list.totalCount()).isEqualTo(5);
        assertThat(list.totalPages()).isEqualTo(3);
        assertThat(list.items()).hasSize(2);
        var first = list.items().getFirst();
        assertThat(first.id()).isEqualTo(receivedId);
        assertThat(first.source()).isEqualTo(InboundEventSourceDto.MAIL);
        assertThat(first.area()).isEqualTo(LifeAreaDto.WORK);
        assertThat(first.subject()).isEqualTo("Invoice 4711");
        assertThat(first.from()).isEqualTo("Power company <billing@power.test>");
        assertThat(first.text()).isNull();
        assertThat(first.status()).isEqualTo(InboundEventStatusDto.RECEIVED);
        assertThat(first.elements()).isEmpty();
        var second = list.items().get(1);
        assertThat(second.id()).isEqualTo(ID);
        assertThat(second.source()).isEqualTo(InboundEventSourceDto.MANUAL);
        assertThat(second.text()).isEqualTo("Pay the invoice");
        assertThat(second.typeHint()).isEqualTo(LunaElementTypeDto.TASK);
        assertThat(second.subject()).isNull();
        assertThat(second.status()).isEqualTo(InboundEventStatusDto.ANALYZED);
        assertThat(second.elements()).containsExactly(new ElementReferenceDto(taskId, LunaElementTypeDto.TASK));
    }

    @Test
    void thatListingTheInboundEventsOfAnAreaShouldResultIntoThePageOfThisArea() {
        // given
        var work = new InboundEvent(ID, EventSource.MAIL, EMAIL_ID, LifeArea.WORK,
                new EventData(jsonMapper.writeValueAsString(email("Invoice 4711"))),
                InboundEventStatus.RECEIVED, 0, NOW, NOW, 0L);
        var pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdDate"));
        when(inboundEventRepository.findByArea(LifeArea.WORK, pageRequest))
                .thenReturn(new PageImpl<>(List.of(work), pageRequest, 1));

        // when
        var list = inboundEventDomainService.listInboundEvents(LifeAreaDto.WORK, PageRequest.of(0, 20));

        // then
        assertThat(list.totalCount()).isEqualTo(1);
        assertThat(list.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(ID);
            assertThat(item.area()).isEqualTo(LifeAreaDto.WORK);
        });
        verify(inboundEventRepository, never()).findAll(any(Pageable.class));
    }

    private EmailEventDataDto email(String subject) {
        return new EmailEventDataDto()
                .account("private")
                .address("luna@luna.test")
                .area(EmailEventDataDto.AreaEnum.WORK)
                .from("Power company <billing@power.test>")
                .to(List.of("luna@luna.test"))
                .subject(subject)
                .receivedDate(NOW)
                .text("Please pay")
                .textTruncated(false)
                .attachments(List.of());
    }
}
