package de.kirchrath.luna.core.converter;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.ElementReferenceDto;
import de.kirchrath.luna.core.dto.ManualInboundEventListItemDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InboundEventMapper {

    @Mapping(target = "id", source = "inboundEvent.id")
    @Mapping(target = "text", source = "input.text")
    @Mapping(target = "typeHint", source = "input.typeHint")
    @Mapping(target = "status", source = "inboundEvent.status")
    @Mapping(target = "createdDate", source = "inboundEvent.createdDate")
    @Mapping(target = "elements", source = "outboundEvents")
    ManualInboundEventListItemDto toManualListItemDto(
            InboundEvent inboundEvent, ManualInputDataDto input, List<OutboundEvent> outboundEvents);

    ElementReferenceDto toElementReferenceDto(OutboundEvent outboundEvent);
}
