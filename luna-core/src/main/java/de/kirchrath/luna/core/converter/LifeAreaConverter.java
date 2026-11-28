package de.kirchrath.luna.core.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** The given areas, or all areas without any. */
    public Set<LifeArea> toAreas(List<LifeAreaDto> areas) {
        return areas.isEmpty()
                ? Set.of(LifeArea.values())
                : areas.stream().map(lifeAreaMapper::toEntity).collect(Collectors.toUnmodifiableSet());
    }

    public LifeArea toEntity(EmailEventDataDto.AreaEnum area) {
        return lifeAreaMapper.toEntity(area);
    }
}
