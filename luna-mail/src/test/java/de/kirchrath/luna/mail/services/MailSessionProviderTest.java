package de.kirchrath.luna.mail.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.kirchrath.luna.mail.config.MailProperties;

class MailSessionProviderTest {

    @Test
    void thatCreatingASessionShouldResultIntoASessionWithTheConfiguredProperties() {
        // given
        var mailProperties = new MailProperties(
                Duration.ofMinutes(1), 100, Map.of("mail.imaps.timeout", "30000"), null, null);
        var provider = new MailSessionProvider(mailProperties);

        // when
        var session = provider.session();

        // then
        assertThat(session.getProperty("mail.imaps.timeout")).isEqualTo("30000");
        assertThat(mailProperties.accounts()).isEmpty();
    }
}
