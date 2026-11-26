package de.kirchrath.luna.appointment.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TimeProviderTest {

    private final TimeProvider timeProvider = new TimeProvider();

    @Test
    void thatRequestingTheCurrentTimeShouldResultIntoNowWithTheOffsetOfTheServerTimeZone() {
        // given
        var before = OffsetDateTime.now();

        // when
        var now = timeProvider.now();

        // then
        assertThat(now).isAfterOrEqualTo(before).isBeforeOrEqualTo(OffsetDateTime.now());
        assertThat(now.getOffset()).isEqualTo(ZoneId.systemDefault().getRules().getOffset(now.toInstant()));
    }

    @Test
    void thatConvertingAnInstantShouldResultIntoTheSameInstantWithTheOffsetOfTheServerTimeZone() {
        // given
        var instant = Instant.parse("2026-07-01T10:15:30Z");

        // when
        var converted = timeProvider.atServerOffset(instant);

        // then
        assertThat(converted.toInstant()).isEqualTo(instant);
        assertThat(converted.getOffset()).isEqualTo(ZoneId.systemDefault().getRules().getOffset(instant));
    }
}
