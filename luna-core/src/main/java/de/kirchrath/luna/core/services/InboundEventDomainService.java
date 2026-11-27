package de.kirchrath.luna.core.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.converter.InboundEventConverter;
import de.kirchrath.luna.core.converter.LifeAreaConverter;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.InboundEventListDto;
import de.kirchrath.luna.core.dto.LifeAreaDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.ResourceReferenceDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDto;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@Transactional
public class InboundEventDomainService {

    private final InboundEventRepository inboundEventRepository;
    private final OutboundEventRepository outboundEventRepository;
    private final InboundEventConverter inboundEventConverter;
    private final InboundEventReader inboundEventReader;
    private final LifeAreaConverter lifeAreaConverter;
    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher eventPublisher;

    public InboundEventDomainService(
            InboundEventRepository inboundEventRepository,
            OutboundEventRepository outboundEventRepository,
            InboundEventConverter inboundEventConverter,
            InboundEventReader inboundEventReader,
            LifeAreaConverter lifeAreaConverter,
            JsonMapper jsonMapper,
            ApplicationEventPublisher eventPublisher) {
        this.inboundEventRepository = inboundEventRepository;
        this.outboundEventRepository = outboundEventRepository;
        this.inboundEventConverter = inboundEventConverter;
        this.inboundEventReader = inboundEventReader;
        this.lifeAreaConverter = lifeAreaConverter;
        this.jsonMapper = jsonMapper;
        this.eventPublisher = eventPublisher;
    }

    public ResourceReferenceDto createManualInboundEvent(ManualInputDataDto input) {
        return new ResourceReferenceDto(inboundEventRepository
                .save(received(EventSource.MANUAL, null, lifeAreaConverter.toEntity(input.area()), input))
                .id());
    }

    /** Stores a fetched email once; the same email again (e.g. after a retry) is ignored. */
    public void createMailInboundEvent(EmailEventDto email) {
        if (inboundEventRepository.existsBySourceAndSourceId(EventSource.MAIL, email.getId())) {
            log.debug("Email {} is already known", email.getId());
            return;
        }
        var data = email.getData();
        var inboundEvent = inboundEventRepository.save(
                received(EventSource.MAIL, email.getId(), lifeAreaConverter.toEntity(data.getArea()), data));
        eventPublisher.publishEvent(new InboundEventStatusChangedEvent(inboundEvent.id()));
    }

    @Transactional(readOnly = true)
    /** @param area only the inbound events of this life area, all if {@code null} */
    public InboundEventListDto listInboundEvents(LifeAreaDto area, Pageable pageable) {
        var page = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.desc(InboundEvent::createdDate)));
        var inboundEvents = area == null
                ? inboundEventRepository.findAll(page)
                : inboundEventRepository.findByArea(lifeAreaConverter.toEntity(area), page);
        var outboundEvents = outboundEventRepository
                .findByInboundEventIdIn(inboundEvents.map(InboundEvent::id).getContent())
                .stream()
                .collect(Collectors.groupingBy(OutboundEvent::inboundEventId));
        var items = inboundEvents
                .map(inboundEvent -> inboundEventConverter.toListItemDto(
                        inboundEvent,
                        inboundEventReader.manualInput(inboundEvent),
                        inboundEventReader.email(inboundEvent),
                        outboundEvents.getOrDefault(inboundEvent.id(), List.<OutboundEvent>of())))
                .getContent();
        return new InboundEventListDto(items, inboundEvents.getTotalPages(), inboundEvents.getTotalElements());
    }

    private InboundEvent received(EventSource source, UUID sourceId, LifeArea area, Object data) {
        return new InboundEvent(
                null,
                source,
                sourceId,
                area,
                new EventData(jsonMapper.writeValueAsString(data)),
                InboundEventStatus.RECEIVED,
                0,
                null,
                null,
                null);
    }
}
