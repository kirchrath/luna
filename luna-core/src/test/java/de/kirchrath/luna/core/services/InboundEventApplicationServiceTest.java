package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import de.kirchrath.luna.core.dto.InboundEventListDto;
import de.kirchrath.luna.core.dto.LifeAreaDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDto;

class InboundEventApplicationServiceTest {

    private final InboundEventDomainService inboundEventDomainService = mock(InboundEventDomainService.class);
    private final InboundEventApplicationService inboundEventApplicationService =
            new InboundEventApplicationService(inboundEventDomainService);

    @Test
    void thatCreatingAManualInboundEventShouldResultIntoTheReferenceOfTheDomainService() {
        // given
        var input = new ManualInputDataDto("Something", LifeAreaDto.PERSONAL, null);
        var reference = new ResourceReferenceDto(UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11"));
        when(inboundEventDomainService.createManualInboundEvent(input)).thenReturn(reference);

        // when
        var created = inboundEventApplicationService.createManualInboundEvent(input);

        // then
        assertThat(created).isEqualTo(reference);
    }

    @Test
    void thatListingInboundEventsShouldResultIntoThePageOfTheDomainService() {
        // given
        var list = new InboundEventListDto(List.of(), 0, 0);
        var pageable = PageRequest.of(1, 20);
        when(inboundEventDomainService.listInboundEvents(List.of(LifeAreaDto.WORK), pageable)).thenReturn(list);

        // when
        var listed = inboundEventApplicationService.listInboundEvents(List.of(LifeAreaDto.WORK), pageable);

        // then
        assertThat(listed).isEqualTo(list);
    }

    @Test
    void thatCreatingAMailInboundEventShouldResultIntoACallOfTheDomainService() {
        // given
        var email = new EmailEventDto().id(UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11"));

        // when
        inboundEventApplicationService.createMailInboundEvent(email);

        // then
        verify(inboundEventDomainService).createMailInboundEvent(email);
    }
}
