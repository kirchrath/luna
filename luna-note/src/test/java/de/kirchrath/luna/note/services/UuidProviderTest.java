package de.kirchrath.luna.note.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UuidProviderTest {

    private final UuidProvider uuidProvider = new UuidProvider();

    @Test
    void thatRequestingTwoUuidsShouldResultIntoTwoDifferentRandomUuids() {
        // given
        var first = uuidProvider.randomUuid();

        // when
        var second = uuidProvider.randomUuid();

        // then
        assertThat(first).isNotNull().isNotEqualTo(second);
        assertThat(first.version()).isEqualTo(4);
    }
}
