package de.kirchrath.luna.core.converter;

import static org.assertj.core.api.Assertions.assertThat;

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
}
