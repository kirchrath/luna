package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.AlertElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.AlertEventDataDto;
import de.kirchrath.luna.core.dto.AlertEventDto;

@Component
public class AlertElementConverter {

    private final AlertElementMapper alertElementMapper;

    public AlertElementConverter(AlertElementMapper alertElementMapper) {
        this.alertElementMapper = alertElementMapper;
    }

    public AlertElement toEntity(UUID outboundEventId, AlertEventDataDto data) {
        return alertElementMapper.toEntity(outboundEventId, data);
    }

    public AlertEventDto toEventDto(OutboundEvent outboundEvent, AlertElement element) {
        return alertElementMapper.toEventDto(outboundEvent, element);
    }
}
