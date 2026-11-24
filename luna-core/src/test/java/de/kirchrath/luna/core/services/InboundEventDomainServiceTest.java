package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import tools.jackson.databind.json.JsonMapper;

class InboundEventDomainServiceTest {

    private final InboundEventRepository inboundEventRepository = mock(InboundEventRepository.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final InboundEventDomainService inboundEventDomainService =
            new InboundEventDomainService(inboundEventRepository, jsonMapper);

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
}
