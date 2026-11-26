package de.kirchrath.luna.mail.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A real IMAP and SMTP server. Account {@code test} uses the default server settings, account {@code other} its own
 * and account {@code broken} a wrong password.
 */
@TestConfiguration(proxyBeanMethods = false)
public class GreenMailConfig {

    public static final int IMAPS_PORT = 3993;
    public static final int SMTP_PORT = 3025;
    public static final String TEST_ADDRESS = "luna@luna.test";
    public static final String OTHER_ADDRESS = "other@luna.test";
    public static final String PASSWORD = "secret";

    @Bean
    GenericContainer<?> greenMailContainer() {
        return new GenericContainer<>(DockerImageName.parse("greenmail/standalone:2.1.15"))
                .withEnv("GREENMAIL_OPTS", "-Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0"
                        + " -Dgreenmail.users=luna:" + PASSWORD + "@luna.test,other:" + PASSWORD + "@luna.test"
                        + " -Dgreenmail.users.login=email")
                .withExposedPorts(IMAPS_PORT, SMTP_PORT);
    }

    @Bean
    DynamicPropertyRegistrar greenMailProperties(GenericContainer<?> greenMailContainer) {
        return registry -> {
            registry.add("app.mail.session[mail.imaps.ssl.trust]", () -> "*");
            registry.add("app.mail.session[mail.imaps.ssl.checkserveridentity]", () -> "false");
            registry.add("app.mail.defaults.host", greenMailContainer::getHost);
            registry.add("app.mail.defaults.port", () -> greenMailContainer.getMappedPort(IMAPS_PORT));
            registry.add("app.mail.accounts.test.username", () -> TEST_ADDRESS);
            registry.add("app.mail.accounts.test.password", () -> PASSWORD);
            registry.add("app.mail.accounts.other.username", () -> OTHER_ADDRESS);
            registry.add("app.mail.accounts.other.password", () -> PASSWORD);
            registry.add("app.mail.accounts.other.host", greenMailContainer::getHost);
            registry.add("app.mail.accounts.other.port", () -> greenMailContainer.getMappedPort(IMAPS_PORT));
            registry.add("app.mail.accounts.other.folder", () -> "INBOX");
            registry.add("app.mail.accounts.broken.username", () -> TEST_ADDRESS);
            registry.add("app.mail.accounts.broken.password", () -> "wrong");
        };
    }
}
