package de.kirchrath.luna.core.services;

import org.springframework.stereotype.Service;

import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;
import tools.jackson.databind.json.JsonMapper;

@Service
public class InboundEventApplicationService {

    private final InboundEventDomainService inboundEventDomainService;
    private final JsonMapper jsonMapper;

    public InboundEventApplicationService(InboundEventDomainService inboundEventDomainService, JsonMapper jsonMapper) {
        this.inboundEventDomainService = inboundEventDomainService;
        this.jsonMapper = jsonMapper;
    }

    public ResourceReferenceDto createManualInboundEvent(ManualInputDataDto input) {
        var data = new EventData(jsonMapper.writeValueAsString(input));
        return new ResourceReferenceDto(inboundEventDomainService.createInboundEvent(EventSource.MANUAL, data));
    }
}
