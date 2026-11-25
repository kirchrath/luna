package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.converter.AppointmentElementConverter;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.AppointmentElement;
import de.kirchrath.luna.core.data.repository.AppointmentElementRepository;
import de.kirchrath.luna.core.dto.AppointmentEventDataDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.Validator;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class AppointmentElementProcessor extends ElementProcessor<AppointmentEventDataDto, AppointmentElement> {

    private final AppointmentElementRepository appointmentElementRepository;
    private final AppointmentElementConverter appointmentElementConverter;

    public AppointmentElementProcessor(
            @Value("classpath:prompts/qualification/appointment.st") Resource prompt,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator,
            AppointmentElementRepository appointmentElementRepository,
            AppointmentElementConverter appointmentElementConverter) {
        super(LunaElementType.APPOINTMENT, "luna_core_appointment", prompt, AppointmentEventDataDto.class,
                elementQualificationClient, elementEventPublisher, validator);
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
    protected Object toEventDto(OutboundEvent outboundEvent, AppointmentElement element) {
        return appointmentElementConverter.toEventDto(outboundEvent, element);
    }
}
