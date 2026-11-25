package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.core.data.entity.TaskElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.AuditDto;
import de.kirchrath.luna.core.dto.TaskEventDataDto;
import de.kirchrath.luna.core.dto.TaskEventDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskElementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "version", ignore = true)
    TaskElement toEntity(UUID outboundEventId, TaskEventDataDto data);

    @Mapping(target = "id", source = "outboundEvent.id")
    @Mapping(target = "audit", source = "outboundEvent")
    @Mapping(target = "data", source = "element")
    TaskEventDto toEventDto(OutboundEvent outboundEvent, TaskElement element);

    TaskEventDataDto toDataDto(TaskElement element);

    AuditDto toAuditDto(OutboundEvent outboundEvent);
}
