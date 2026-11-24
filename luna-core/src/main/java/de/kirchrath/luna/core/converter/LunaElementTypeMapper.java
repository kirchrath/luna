package de.kirchrath.luna.core.converter;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LunaElementTypeMapper {

    LunaElementType toEntity(LunaElementTypeDto type);
}
