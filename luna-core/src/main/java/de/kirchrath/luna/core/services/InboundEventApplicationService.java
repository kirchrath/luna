package de.kirchrath.luna.core.services;

import org.springframework.stereotype.Service;

import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;

@Service
public class InboundEventApplicationService {

    private final InboundEventDomainService inboundEventDomainService;

    public InboundEventApplicationService(InboundEventDomainService inboundEventDomainService) {
        this.inboundEventDomainService = inboundEventDomainService;
    }

    public ResourceReferenceDto createManualInboundEvent(ManualInputDataDto input) {
        return inboundEventDomainService.createManualInboundEvent(input);
    }
}
