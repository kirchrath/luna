package de.kirchrath.luna.core.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.converter.InboundEventConverter;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.ManualInboundEventListDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional
public class InboundEventDomainService {

    private static final String CREATED_DATE = "createdDate";

    private final InboundEventRepository inboundEventRepository;
    private final OutboundEventRepository outboundEventRepository;
    private final InboundEventConverter inboundEventConverter;
    private final JsonMapper jsonMapper;

    public InboundEventDomainService(
            InboundEventRepository inboundEventRepository,
            OutboundEventRepository outboundEventRepository,
            InboundEventConverter inboundEventConverter,
            JsonMapper jsonMapper) {
        this.inboundEventRepository = inboundEventRepository;
        this.outboundEventRepository = outboundEventRepository;
        this.inboundEventConverter = inboundEventConverter;
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

    @Transactional(readOnly = true)
    public ManualInboundEventListDto listManualInboundEvents(Pageable pageable) {
        var inboundEvents = inboundEventRepository.findBySource(EventSource.MANUAL, PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, CREATED_DATE)));
        var outboundEvents = outboundEventRepository
                .findByInboundEventIdIn(inboundEvents.map(InboundEvent::id).getContent())
                .stream()
                .collect(Collectors.groupingBy(OutboundEvent::inboundEventId));
        var items = inboundEvents
                .map(inboundEvent -> inboundEventConverter.toManualListItemDto(
                        inboundEvent,
                        jsonMapper.readValue(inboundEvent.data().json(), ManualInputDataDto.class),
                        outboundEvents.getOrDefault(inboundEvent.id(), List.<OutboundEvent>of())))
                .getContent();
        return new ManualInboundEventListDto(items, inboundEvents.getTotalPages(), inboundEvents.getTotalElements());
    }
}
