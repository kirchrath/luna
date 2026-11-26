package de.kirchrath.luna.core.converter;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.ElementReferenceDto;
import de.kirchrath.luna.core.dto.InboundEventListItemDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDataDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InboundEventMapper {

    @Mapping(target = "id", source = "inboundEvent.id")
    @Mapping(target = "source", source = "inboundEvent.source")
    @Mapping(target = "text", source = "manualInput.text")
    @Mapping(target = "typeHint", source = "manualInput.typeHint")
    @Mapping(target = "subject", source = "email.subject")
    @Mapping(target = "from", source = "email.from")
    @Mapping(target = "status", source = "inboundEvent.status")
    @Mapping(target = "createdDate", source = "inboundEvent.createdDate")
    @Mapping(target = "elements", source = "outboundEvents")
    InboundEventListItemDto toListItemDto(
            InboundEvent inboundEvent,
            ManualInputDataDto manualInput,
            EmailEventDataDto email,
            List<OutboundEvent> outboundEvents);

    ElementReferenceDto toElementReferenceDto(OutboundEvent outboundEvent);
}
