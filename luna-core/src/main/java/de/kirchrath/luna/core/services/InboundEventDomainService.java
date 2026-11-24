package de.kirchrath.luna.core.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional
public class InboundEventDomainService {

    private final InboundEventRepository inboundEventRepository;
    private final JsonMapper jsonMapper;

    public InboundEventDomainService(InboundEventRepository inboundEventRepository, JsonMapper jsonMapper) {
        this.inboundEventRepository = inboundEventRepository;
        this.jsonMapper = jsonMapper;
    }

    public ResourceReferenceDto createManualInboundEvent(ManualInputDataDto input) {
        var inboundEvent = new InboundEvent(
                null,
                EventSource.MANUAL,
                new EventData(jsonMapper.writeValueAsString(input)),
                InboundEventStatus.RECEIVED,
                0,
                null,
                null,
                null);
        return new ResourceReferenceDto(inboundEventRepository.save(inboundEvent).id());
    }
}
