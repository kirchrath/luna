package de.kirchrath.luna.core.services;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private final LunaElementTypeConverter lunaElementTypeConverter;
    private final JsonMapper jsonMapper;
    private final ClassificationProperties classificationProperties;

    public ClassificationDomainService(
            InboundEventRepository inboundEventRepository,
            OutboundEventRepository outboundEventRepository,
            ElementClassificationClient elementClassificationClient,
            LunaElementTypeConverter lunaElementTypeConverter,
            JsonMapper jsonMapper,
            ClassificationProperties classificationProperties) {
        this.inboundEventRepository = inboundEventRepository;
        this.outboundEventRepository = outboundEventRepository;
        this.elementClassificationClient = elementClassificationClient;
        this.lunaElementTypeConverter = lunaElementTypeConverter;
        this.jsonMapper = jsonMapper;
        this.classificationProperties = classificationProperties;
    }

    public List<InboundEvent> findInboundEventsToClassify() {
        return inboundEventRepository.findByStatusOrderByCreatedDateAsc(
                InboundEventStatus.RECEIVED, Limit.of(classificationProperties.batchSize()));
    }

    public void classify(InboundEvent inboundEvent) {
        Set<LunaElementType> types;
        try {
            types = determineTypes(jsonMapper.readValue(inboundEvent.data().json(), ManualInputDataDto.class));
        } catch (RuntimeException e) {
            recordFailedAttempt(inboundEvent, e);
            return;
        }
        types.forEach(type -> outboundEventRepository.save(
                new OutboundEvent(null, inboundEvent.id(), type, OutboundEventStatus.CLASSIFIED, null, null, null)));
        inboundEventRepository.save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
        log.debug("Classified inbound event {} as {}", inboundEvent.id(), types);
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

    private void recordFailedAttempt(InboundEvent inboundEvent, RuntimeException cause) {
        var attempts = inboundEvent.attempts() + 1;
        var maxAttempts = classificationProperties.maxAttempts();
        if (attempts >= maxAttempts) {
            log.error("Classification of inbound event {} failed finally after {} attempts",
                    inboundEvent.id(), attempts, cause);
            inboundEventRepository.save(inboundEvent.withAttempts(attempts).withStatus(InboundEventStatus.FAILED));
        } else {
            log.warn("Classification of inbound event {} failed (attempt {}/{})",
                    inboundEvent.id(), attempts, maxAttempts, cause);
            inboundEventRepository.save(inboundEvent.withAttempts(attempts).withStatus(InboundEventStatus.RECEIVED));
        }
    }
}
