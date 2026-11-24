package de.kirchrath.luna.core.converter;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;

@Component
public class LunaElementTypeConverter {

    private final LunaElementTypeMapper lunaElementTypeMapper;

    public LunaElementTypeConverter(LunaElementTypeMapper lunaElementTypeMapper) {
        this.lunaElementTypeMapper = lunaElementTypeMapper;
    }

    public LunaElementType toEntity(LunaElementTypeDto type) {
        return lunaElementTypeMapper.toEntity(type);
    }
}
