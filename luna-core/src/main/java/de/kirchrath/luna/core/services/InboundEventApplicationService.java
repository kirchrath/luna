package de.kirchrath.luna.core.services;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import de.kirchrath.luna.core.dto.InboundEventListDto;
import de.kirchrath.luna.core.dto.LifeAreaDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDto;

@Service
public class InboundEventApplicationService {

    private final InboundEventDomainService inboundEventDomainService;

    public InboundEventApplicationService(InboundEventDomainService inboundEventDomainService) {
        this.inboundEventDomainService = inboundEventDomainService;
    }

    public ResourceReferenceDto createManualInboundEvent(ManualInputDataDto input) {
        return inboundEventDomainService.createManualInboundEvent(input);
    }

    public void createMailInboundEvent(EmailEventDto email) {
        inboundEventDomainService.createMailInboundEvent(email);
    }

    public InboundEventListDto listInboundEvents(List<LifeAreaDto> areas, Pageable pageable) {
        return inboundEventDomainService.listInboundEvents(areas, pageable);
    }
}
