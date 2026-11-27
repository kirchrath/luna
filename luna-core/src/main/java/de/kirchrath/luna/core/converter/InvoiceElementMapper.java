package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.core.data.entity.InvoiceElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.AuditDto;
import de.kirchrath.luna.core.dto.InvoiceEventDataDto;
import de.kirchrath.luna.core.dto.InvoiceEventDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvoiceElementMapper {

    @Mapping(target = "id", ignore = true)
    // EntityWithId#withId looks like a fluent setter to MapStruct; the id is set by EntityIdCallback.
    @Mapping(target = "withId", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "version", ignore = true)
    InvoiceElement toEntity(UUID outboundEventId, InvoiceEventDataDto data);

    @Mapping(target = "id", source = "outboundEvent.id")
    @Mapping(target = "audit", source = "outboundEvent")
    @Mapping(target = "area", source = "outboundEvent.area")
    @Mapping(target = "data", source = "element")
    InvoiceEventDto toEventDto(OutboundEvent outboundEvent, InvoiceElement element);

    InvoiceEventDataDto toDataDto(InvoiceElement element);

    AuditDto toAuditDto(OutboundEvent outboundEvent);
}
