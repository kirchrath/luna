package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.core.data.entity.AlertElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.AuditDto;
import de.kirchrath.luna.core.dto.AlertEventDataDto;
import de.kirchrath.luna.core.dto.AlertEventDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AlertElementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "version", ignore = true)
    AlertElement toEntity(UUID outboundEventId, AlertEventDataDto data);

    @Mapping(target = "id", source = "outboundEvent.id")
    @Mapping(target = "audit", source = "outboundEvent")
    @Mapping(target = "area", source = "outboundEvent.area")
    @Mapping(target = "data", source = "element")
    AlertEventDto toEventDto(OutboundEvent outboundEvent, AlertElement element);

    AlertEventDataDto toDataDto(AlertElement element);

    AuditDto toAuditDto(OutboundEvent outboundEvent);
}
