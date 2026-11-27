package de.kirchrath.luna.mail.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import de.kirchrath.luna.mail.data.entity.LifeArea;

class MailPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(MailPropertiesConfiguration.class)
            .withPropertyValues("app.mail.accounts.private.username=me@luna.test", "app.mail.accounts.private.password=x");

    @Test
    void thatAnAccountWithAnAreaShouldResultIntoTheBoundArea() {
        // when
        contextRunner.withPropertyValues("app.mail.accounts.private.area=PERSONAL").run(context -> {
            // then
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MailProperties.class).accounts().get("private").area())
                    .isEqualTo(LifeArea.PERSONAL);
        });
    }

    @Test
    void thatAnAccountWithoutAnAreaShouldResultIntoAFailedStart() {
        // when
        contextRunner.run(context -> {
            // then
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("accounts[private].area");
        });
    }

    @Configuration
    @EnableConfigurationProperties(MailProperties.class)
    static class MailPropertiesConfiguration {
    }
}
