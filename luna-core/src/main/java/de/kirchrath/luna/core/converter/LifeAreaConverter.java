package de.kirchrath.luna.core.converter;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.dto.LifeAreaDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDataDto;

@Component
public class LifeAreaConverter {

    private final LifeAreaMapper lifeAreaMapper;

    public LifeAreaConverter(LifeAreaMapper lifeAreaMapper) {
        this.lifeAreaMapper = lifeAreaMapper;
    }

    public LifeArea toEntity(LifeAreaDto area) {
        return lifeAreaMapper.toEntity(area);
    }

    public LifeArea toEntity(EmailEventDataDto.AreaEnum area) {
        return lifeAreaMapper.toEntity(area);
    }
}
