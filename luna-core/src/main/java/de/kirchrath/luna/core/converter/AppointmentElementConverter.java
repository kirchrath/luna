package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.AppointmentElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.AppointmentEventDataDto;
import de.kirchrath.luna.core.dto.AppointmentEventDto;

@Component
public class AppointmentElementConverter {

    private final AppointmentElementMapper appointmentElementMapper;

    public AppointmentElementConverter(AppointmentElementMapper appointmentElementMapper) {
        this.appointmentElementMapper = appointmentElementMapper;
    }

    public AppointmentElement toEntity(UUID outboundEventId, AppointmentEventDataDto data) {
        return appointmentElementMapper.toEntity(outboundEventId, data);
    }

    public AppointmentEventDto toEventDto(OutboundEvent outboundEvent, AppointmentElement element) {
        return appointmentElementMapper.toEventDto(outboundEvent, element);
    }
}
