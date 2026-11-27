package de.kirchrath.luna.core.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.dto.LifeAreaDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDataDto;

class LifeAreaConverterTest {

    private final LifeAreaConverter lifeAreaConverter = new LifeAreaConverter(new LifeAreaMapperImpl());

    @ParameterizedTest
    @EnumSource(LifeAreaDto.class)
    void thatConvertingADtoAreaShouldResultIntoTheEntityAreaWithTheSameName(LifeAreaDto area) {
        // given
        var expected = LifeArea.valueOf(area.name());

        // when
        var converted = lifeAreaConverter.toEntity(area);

        // then
        assertThat(converted).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmailEventDataDto.AreaEnum.class)
    void thatConvertingTheAreaOfAnEmailShouldResultIntoTheEntityAreaWithTheSameName(EmailEventDataDto.AreaEnum area) {
        // given
        var expected = LifeArea.valueOf(area.name());

        // when
        var converted = lifeAreaConverter.toEntity(area);

        // then
        assertThat(converted).isEqualTo(expected);
    }

    @Test
    void thatConvertingNoAreasShouldResultIntoAllAreas() {
        // when
        var areas = lifeAreaConverter.toAreas(List.of());

        // then
        assertThat(areas).containsExactlyInAnyOrder(LifeArea.values());
    }

    @Test
    void thatConvertingSomeAreasShouldResultIntoTheseAreas() {
        // when
        var areas = lifeAreaConverter.toAreas(List.of(LifeAreaDto.WORK, LifeAreaDto.WORK));

        // then
        assertThat(areas).containsExactly(LifeArea.WORK);
    }
}
