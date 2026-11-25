package de.kirchrath.luna.core.services;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.data.entity.EntityWithId;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Transactional(propagation = Propagation.MANDATORY)
public abstract class ElementProcessor<D, E extends EntityWithId<E>> {

    private final LunaElementType type;
    private final String topic;
    private final Resource prompt;
    private final Class<D> dataType;
    private final ElementQualificationClient elementQualificationClient;
    private final ElementEventPublisher elementEventPublisher;
    private final Validator validator;

    protected ElementProcessor(
            LunaElementType type,
            String topic,
            Resource prompt,
            Class<D> dataType,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator) {
        this.type = type;
        this.topic = topic;
        this.prompt = prompt;
        this.dataType = dataType;
        this.elementQualificationClient = elementQualificationClient;
        this.elementEventPublisher = elementEventPublisher;
        this.validator = validator;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public LunaElementType type() {
        return type;
    }

    public D qualify(String text, OffsetDateTime referenceTime) {
        var data = elementQualificationClient.qualify(prompt, text, referenceTime, dataType);
        if (data == null) {
            throw new IllegalStateException("No " + type + " data could be extracted");
        }
        var violations = validator.validate(data);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return data;
    }

    public void storeAndPublish(OutboundEvent outboundEvent, D data) {
        var element = save(toEntity(outboundEvent.id(), data));
        elementEventPublisher.publish(topic, outboundEvent.id(), toEventDto(outboundEvent, element));
    }

    protected abstract E toEntity(UUID outboundEventId, D data);

    protected abstract E save(E element);

    protected abstract Object toEventDto(OutboundEvent outboundEvent, E element);
}
