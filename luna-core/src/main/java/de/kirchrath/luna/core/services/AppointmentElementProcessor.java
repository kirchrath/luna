package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.config.PromptProperties;
import de.kirchrath.luna.core.config.TopicProperties;
import de.kirchrath.luna.core.converter.AppointmentElementConverter;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.AppointmentElement;
import de.kirchrath.luna.core.data.repository.AppointmentElementRepository;
import de.kirchrath.luna.core.dto.AppointmentEventDataDto;
import de.kirchrath.luna.core.dto.AppointmentEventDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.Validator;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class AppointmentElementProcessor extends ElementProcessor<AppointmentEventDataDto, AppointmentElement, AppointmentEventDto> {

    private final AppointmentElementRepository appointmentElementRepository;
    private final AppointmentElementConverter appointmentElementConverter;

    public AppointmentElementProcessor(
            TopicProperties topicProperties,
            PromptProperties promptProperties,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator,
            AppointmentElementRepository appointmentElementRepository,
            AppointmentElementConverter appointmentElementConverter) {
        super(LunaElementType.APPOINTMENT, topicProperties.appointment(), promptProperties.appointment(), new ParameterizedTypeReference<>() {
        }, elementQualificationClient, elementEventPublisher, validator);
        this.appointmentElementRepository = appointmentElementRepository;
        this.appointmentElementConverter = appointmentElementConverter;
    }

    @Override
    protected AppointmentElement toEntity(UUID outboundEventId, AppointmentEventDataDto data) {
        return appointmentElementConverter.toEntity(outboundEventId, data);
    }

    @Override
    protected AppointmentElement save(AppointmentElement element) {
        return appointmentElementRepository.save(element);
    }

    @Override
    protected AppointmentEventDto toEventDto(OutboundEvent outboundEvent, AppointmentElement element) {
        return appointmentElementConverter.toEventDto(outboundEvent, element);
    }
}
