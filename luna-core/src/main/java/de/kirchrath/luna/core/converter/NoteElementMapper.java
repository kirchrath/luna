package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.core.data.entity.NoteElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.AuditDto;
import de.kirchrath.luna.core.dto.NoteEventDataDto;
import de.kirchrath.luna.core.dto.NoteEventDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteElementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "version", ignore = true)
    NoteElement toEntity(UUID outboundEventId, NoteEventDataDto data);

    @Mapping(target = "id", source = "outboundEvent.id")
    @Mapping(target = "audit", source = "outboundEvent")
    @Mapping(target = "data", source = "element")
    NoteEventDto toEventDto(OutboundEvent outboundEvent, NoteElement element);

    NoteEventDataDto toDataDto(NoteElement element);

    AuditDto toAuditDto(OutboundEvent outboundEvent);
}
