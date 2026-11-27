package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.core.data.entity.AppointmentElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.AuditDto;
import de.kirchrath.luna.core.dto.AppointmentEventDataDto;
import de.kirchrath.luna.core.dto.AppointmentEventDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AppointmentElementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "version", ignore = true)
    AppointmentElement toEntity(UUID outboundEventId, AppointmentEventDataDto data);

    @Mapping(target = "id", source = "outboundEvent.id")
    @Mapping(target = "audit", source = "outboundEvent")
    @Mapping(target = "area", source = "outboundEvent.area")
    @Mapping(target = "data", source = "element")
    AppointmentEventDto toEventDto(OutboundEvent outboundEvent, AppointmentElement element);

    AppointmentEventDataDto toDataDto(AppointmentElement element);

    AuditDto toAuditDto(OutboundEvent outboundEvent);
}
