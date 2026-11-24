package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;

class InboundEventApplicationServiceTest {

    private final InboundEventDomainService inboundEventDomainService = mock(InboundEventDomainService.class);
    private final InboundEventApplicationService inboundEventApplicationService =
            new InboundEventApplicationService(inboundEventDomainService);

    @Test
    void thatCreatingAManualInboundEventShouldResultIntoTheReferenceOfTheDomainService() {
        // given
        var input = new ManualInputDataDto("Something", null);
        var reference = new ResourceReferenceDto(UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11"));
        when(inboundEventDomainService.createManualInboundEvent(input)).thenReturn(reference);

        // when
        var created = inboundEventApplicationService.createManualInboundEvent(input);

        // then
        assertThat(created).isEqualTo(reference);
    }
}
