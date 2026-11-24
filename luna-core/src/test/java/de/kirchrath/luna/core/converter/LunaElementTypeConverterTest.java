package de.kirchrath.luna.core.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;

class LunaElementTypeConverterTest {

    private final LunaElementTypeConverter lunaElementTypeConverter =
            new LunaElementTypeConverter(new LunaElementTypeMapperImpl());

    @ParameterizedTest
    @EnumSource(LunaElementTypeDto.class)
    void thatConvertingADtoTypeShouldResultIntoTheEntityTypeWithTheSameName(LunaElementTypeDto type) {
        // given
        var expected = LunaElementType.valueOf(type.name());

        // when
        var converted = lunaElementTypeConverter.toEntity(type);

        // then
        assertThat(converted).isEqualTo(expected);
    }

    @Test
    void thatConvertingNullShouldResultIntoNull() {
        // given
        LunaElementTypeDto type = null;

        // when
        var converted = lunaElementTypeConverter.toEntity(type);

        // then
        assertThat(converted).isNull();
    }
}
