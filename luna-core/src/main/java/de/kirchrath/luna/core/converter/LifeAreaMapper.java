package de.kirchrath.luna.core.converter;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.dto.LifeAreaDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDataDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LifeAreaMapper {

    LifeArea toEntity(LifeAreaDto area);

    LifeArea toEntity(EmailEventDataDto.AreaEnum area);
}
