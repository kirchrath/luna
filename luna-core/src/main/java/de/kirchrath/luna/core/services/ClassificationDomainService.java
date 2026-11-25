package de.kirchrath.luna.core.services;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.config.ClassificationProperties;
import de.kirchrath.luna.core.converter.LunaElementTypeConverter;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.outbound.ElementClassificationClient;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@Transactional
public class ClassificationDomainService {

    private final InboundEventRepository inboundEventRepository;
    private final OutboundEventRepository outboundEventRepository;
    private final ElementClassificationClient elementClassificationClient;
    private final Map<LunaElementType, ElementProcessor<?, ?, ?>> elementProcessors;
    private final LunaElementTypeConverter lunaElementTypeConverter;
    private final JsonMapper jsonMapper;
    private final ClassificationProperties classificationProperties;
    private final ApplicationEventPublisher eventPublisher;

    public ClassificationDomainService(
            InboundEventRepository inboundEventRepository,
            OutboundEventRepository outboundEventRepository,
            ElementClassificationClient elementClassificationClient,
            List<ElementProcessor<?, ?, ?>> elementProcessors,
            LunaElementTypeConverter lunaElementTypeConverter,
            JsonMapper jsonMapper,
            ClassificationProperties classificationProperties,
            ApplicationEventPublisher eventPublisher) {
        this.inboundEventRepository = inboundEventRepository;
        this.outboundEventRepository = outboundEventRepository;
        this.elementClassificationClient = elementClassificationClient;
        this.elementProcessors = new EnumMap<>(LunaElementType.class);
        elementProcessors.forEach(processor -> this.elementProcessors.put(processor.type(), processor));
        this.lunaElementTypeConverter = lunaElementTypeConverter;
        this.jsonMapper = jsonMapper;
        this.classificationProperties = classificationProperties;
        this.eventPublisher = eventPublisher;
    }

    public List<UUID> findInboundEventIdsToClassify() {
        return inboundEventRepository.findByStatusOrderByCreatedDateAsc(
                        InboundEventStatus.RECEIVED, Limit.of(classificationProperties.batchSize()))
                .stream()
                .map(InboundEvent::id)
                .toList();
    }

    public void classify(UUID inboundEventId) {
        var inboundEvent = inboundEventRepository.findById(inboundEventId).orElseThrow();
        List<QualifiedElement<?>> elements;
        try {
            var input = jsonMapper.readValue(inboundEvent.data().json(), ManualInputDataDto.class);
            elements = determineTypes(input).stream()
                    .flatMap(type -> qualify(elementProcessors.get(type), input.text(), inboundEvent.createdDate()).stream())
                    .toList();
        } catch (RuntimeException e) {
            recordFailedAttempt(inboundEvent, e);
            return;
        }
        elements.forEach(element -> element.storeAndPublish(outboundEventRepository.save(new OutboundEvent(
                null, inboundEvent.id(), element.type(), OutboundEventStatus.PUBLISHED, null, null, null))));
        inboundEventRepository.save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
        eventPublisher.publishEvent(new InboundEventStatusChangedEvent(inboundEvent.id()));
        log.debug("Published {} element(s) for inbound event {}", elements.size(), inboundEvent.id());
    }

    private Set<LunaElementType> determineTypes(ManualInputDataDto input) {
        var result = elementClassificationClient.classify(input.text(), input.typeHint());
        var types = new LinkedHashSet<LunaElementType>();
        if (input.typeHint() != null) {
            types.add(lunaElementTypeConverter.toEntity(input.typeHint()));
        }
        if (result != null && result.types() != null) {
            result.types().forEach(type -> types.add(lunaElementTypeConverter.toEntity(type)));
        }
        return types;
    }

    private <D> List<QualifiedElement<?>> qualify(
            ElementProcessor<D, ?, ?> processor, String text, OffsetDateTime referenceTime) {
        return processor.qualify(text, referenceTime).stream()
                .<QualifiedElement<?>>map(data -> new QualifiedElement<>(processor, data))
                .toList();
    }

    private void recordFailedAttempt(InboundEvent inboundEvent, RuntimeException cause) {
        var attempts = inboundEvent.attempts() + 1;
        var maxAttempts = classificationProperties.maxAttempts();
        if (attempts >= maxAttempts) {
            log.error("Classification of inbound event {} failed finally after {} attempts",
                    inboundEvent.id(), attempts, cause);
            inboundEventRepository.save(inboundEvent.withAttempts(attempts).withStatus(InboundEventStatus.FAILED));
            eventPublisher.publishEvent(new InboundEventStatusChangedEvent(inboundEvent.id()));
        } else {
            log.warn("Classification of inbound event {} failed (attempt {}/{})",
                    inboundEvent.id(), attempts, maxAttempts, cause);
            inboundEventRepository.save(inboundEvent.withAttempts(attempts).withStatus(InboundEventStatus.RECEIVED));
        }
    }

    private record QualifiedElement<D>(ElementProcessor<D, ?, ?> processor, D data) {

        LunaElementType type() {
            return processor.type();
        }

        void storeAndPublish(OutboundEvent outboundEvent) {
            processor.storeAndPublish(outboundEvent, data);
        }
    }
}
