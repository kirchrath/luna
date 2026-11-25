package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.converter.AlertElementConverter;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.AlertElement;
import de.kirchrath.luna.core.data.repository.AlertElementRepository;
import de.kirchrath.luna.core.dto.AlertEventDataDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.Validator;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class AlertElementProcessor extends ElementProcessor<AlertEventDataDto, AlertElement> {

    private final AlertElementRepository alertElementRepository;
    private final AlertElementConverter alertElementConverter;

    public AlertElementProcessor(
            @Value("classpath:prompts/qualification/alert.st") Resource prompt,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator,
            AlertElementRepository alertElementRepository,
            AlertElementConverter alertElementConverter) {
        super(LunaElementType.ALERT, "luna_core_alert", prompt, AlertEventDataDto.class,
                elementQualificationClient, elementEventPublisher, validator);
        this.alertElementRepository = alertElementRepository;
        this.alertElementConverter = alertElementConverter;
    }

    @Override
    protected AlertElement toEntity(UUID outboundEventId, AlertEventDataDto data) {
        return alertElementConverter.toEntity(outboundEventId, data);
    }

    @Override
    protected AlertElement save(AlertElement element) {
        return alertElementRepository.save(element);
    }

    @Override
    protected Object toEventDto(OutboundEvent outboundEvent, AlertElement element) {
        return alertElementConverter.toEventDto(outboundEvent, element);
    }
}
