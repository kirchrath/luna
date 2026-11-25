package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import de.kirchrath.luna.core.converter.InboundEventConverter;
import de.kirchrath.luna.core.converter.InboundEventMapperImpl;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.ElementReferenceDto;
import de.kirchrath.luna.core.dto.InboundEventStatusDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import tools.jackson.databind.json.JsonMapper;

class InboundEventDomainServiceTest {

    private final InboundEventRepository inboundEventRepository = mock(InboundEventRepository.class);
    private final OutboundEventRepository outboundEventRepository = mock(OutboundEventRepository.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final InboundEventDomainService inboundEventDomainService = new InboundEventDomainService(
            inboundEventRepository,
            outboundEventRepository,
            new InboundEventConverter(new InboundEventMapperImpl()),
            jsonMapper);

    @Test
    void thatCreatingAManualInboundEventShouldResultIntoAStoredReceivedEventAndItsReference() {
        // given
        var id = UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11");
        when(inboundEventRepository.save(any()))
                .thenAnswer(invocation -> invocation.<InboundEvent>getArgument(0).withId(id));
        var input = new ManualInputDataDto("Pay the electricity bill", LunaElementTypeDto.TASK);

        // when
        var reference = inboundEventDomainService.createManualInboundEvent(input);

        // then
        assertThat(reference.id()).isEqualTo(id);
        var captor = ArgumentCaptor.forClass(InboundEvent.class);
        verify(inboundEventRepository).save(captor.capture());
        var stored = captor.getValue();
        assertThat(stored.id()).isNull();
        assertThat(stored.source()).isEqualTo(EventSource.MANUAL);
        assertThat(jsonMapper.readValue(stored.data().json(), ManualInputDataDto.class)).isEqualTo(input);
        assertThat(stored.status()).isEqualTo(InboundEventStatus.RECEIVED);
        assertThat(stored.attempts()).isZero();
        assertThat(stored.createdDate()).isNull();
        assertThat(stored.lastModifiedDate()).isNull();
        assertThat(stored.version()).isNull();
    }

    @Test
    void thatListingManualInboundEventsShouldResultIntoThePageNewestFirstWithTheElementsOfEachEvent() {
        // given
        var now = OffsetDateTime.parse("2026-11-25T10:00:00+01:00");
        var analyzedId = UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11");
        var receivedId = UUID.fromString("9a4c2e1b-8d7f-4e3a-b2c1-0f9e8d7c6b5a");
        var taskId = UUID.fromString("6d1f0a3e-3c4b-4f63-8a3e-1e2f3a4b5c6d");
        var analyzed = new InboundEvent(analyzedId, EventSource.MANUAL,
                new EventData(jsonMapper.writeValueAsString(new ManualInputDataDto("Pay the invoice", LunaElementTypeDto.TASK))),
                InboundEventStatus.ANALYZED, 1, now, now, 1L);
        var received = new InboundEvent(receivedId, EventSource.MANUAL,
                new EventData(jsonMapper.writeValueAsString(new ManualInputDataDto("Milk is empty", null))),
                InboundEventStatus.RECEIVED, 0, now, now, 0L);
        var pageRequest = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdDate"));
        when(inboundEventRepository.findBySource(EventSource.MANUAL, pageRequest))
                .thenReturn(new PageImpl<>(List.of(received, analyzed), pageRequest, 5));
        when(outboundEventRepository.findByInboundEventIdIn(List.of(receivedId, analyzedId))).thenReturn(List.of(
                new OutboundEvent(taskId, analyzedId, LunaElementType.TASK, OutboundEventStatus.PUBLISHED, now, now, 0L)));

        // when
        var list = inboundEventDomainService.listManualInboundEvents(PageRequest.of(1, 2, Sort.by("text")));

        // then
        assertThat(list.totalCount()).isEqualTo(5);
        assertThat(list.totalPages()).isEqualTo(3);
        assertThat(list.items()).hasSize(2);
        var first = list.items().getFirst();
        assertThat(first.id()).isEqualTo(receivedId);
        assertThat(first.text()).isEqualTo("Milk is empty");
        assertThat(first.status()).isEqualTo(InboundEventStatusDto.RECEIVED);
        assertThat(first.elements()).isEmpty();
        var second = list.items().get(1);
        assertThat(second.id()).isEqualTo(analyzedId);
        assertThat(second.typeHint()).isEqualTo(LunaElementTypeDto.TASK);
        assertThat(second.status()).isEqualTo(InboundEventStatusDto.ANALYZED);
        assertThat(second.elements()).containsExactly(new ElementReferenceDto(taskId, LunaElementTypeDto.TASK));
    }
}
