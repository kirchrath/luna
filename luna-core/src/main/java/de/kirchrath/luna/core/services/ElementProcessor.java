package de.kirchrath.luna.core.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.data.entity.EntityWithId;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.ElementEventDto;
import de.kirchrath.luna.core.dto.QualificationResultDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Transactional(propagation = Propagation.MANDATORY)
public abstract class ElementProcessor<D, E extends EntityWithId<E>, V extends ElementEventDto> {

    private final LunaElementType type;
    private final String topic;
    private final Resource prompt;
    private final ParameterizedTypeReference<QualificationResultDto<D>> resultType;
    private final ElementQualificationClient elementQualificationClient;
    private final ElementEventPublisher elementEventPublisher;
    private final Validator validator;

    protected ElementProcessor(
            LunaElementType type,
            String topic,
            Resource prompt,
            ParameterizedTypeReference<QualificationResultDto<D>> resultType,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator) {
        this.type = type;
        this.topic = topic;
        this.prompt = prompt;
        this.resultType = resultType;
        this.elementQualificationClient = elementQualificationClient;
        this.elementEventPublisher = elementEventPublisher;
        this.validator = validator;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public LunaElementType type() {
        return type;
    }

    public List<D> qualify(String text, OffsetDateTime referenceTime) {
        var result = elementQualificationClient.qualify(prompt, text, referenceTime, resultType);
        if (result == null || result.items() == null || result.items().isEmpty()) {
            throw new IllegalStateException("No " + type + " data could be extracted");
        }
        result.items().forEach(this::validate);
        return result.items();
    }

    public void storeAndPublish(OutboundEvent outboundEvent, D data) {
        var element = save(toEntity(outboundEvent.id(), data));
        elementEventPublisher.publish(topic, toEventDto(outboundEvent, element));
    }

    protected abstract E toEntity(UUID outboundEventId, D data);

    protected abstract E save(E element);

    protected abstract V toEventDto(OutboundEvent outboundEvent, E element);

    private void validate(D data) {
        var violations = validator.validate(data);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
