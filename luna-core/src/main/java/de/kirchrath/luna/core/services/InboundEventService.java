package de.kirchrath.luna.core.services;

import org.springframework.stereotype.Service;

import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;
import tools.jackson.databind.json.JsonMapper;

@Service
public class InboundEventService {

    private final InboundEventRepository inboundEventRepository;
    private final UuidProvider uuidProvider;
    private final JsonMapper jsonMapper;

    public InboundEventService(
            InboundEventRepository inboundEventRepository, UuidProvider uuidProvider, JsonMapper jsonMapper) {
        this.inboundEventRepository = inboundEventRepository;
        this.uuidProvider = uuidProvider;
        this.jsonMapper = jsonMapper;
    }

    public ResourceReferenceDto createManualInboundEvent(ManualInputDataDto input) {
        var inboundEvent = new InboundEvent(
                uuidProvider.randomUuid(),
                EventSource.MANUAL,
                new EventData(jsonMapper.writeValueAsString(input)),
                null,
                null,
                null);
        return new ResourceReferenceDto(inboundEventRepository.save(inboundEvent).id());
    }
}
