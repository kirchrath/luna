package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import tools.jackson.databind.json.JsonMapper;

class InboundEventApplicationServiceTest {

    private final InboundEventDomainService inboundEventDomainService = mock(InboundEventDomainService.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final InboundEventApplicationService inboundEventApplicationService =
            new InboundEventApplicationService(inboundEventDomainService, jsonMapper);

    @Test
    void thatCreatingAManualInboundEventShouldResultIntoAStoredManualEventAndItsReference() {
        // given
        var id = UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11");
        when(inboundEventDomainService.createInboundEvent(eq(EventSource.MANUAL), any())).thenReturn(id);
        var input = new ManualInputDataDto("Pay the electricity bill", LunaElementTypeDto.TASK);

        // when
        var reference = inboundEventApplicationService.createManualInboundEvent(input);

        // then
        assertThat(reference.id()).isEqualTo(id);
        var captor = ArgumentCaptor.forClass(EventData.class);
        verify(inboundEventDomainService).createInboundEvent(eq(EventSource.MANUAL), captor.capture());
        assertThat(jsonMapper.readValue(captor.getValue().json(), ManualInputDataDto.class)).isEqualTo(input);
    }
}
