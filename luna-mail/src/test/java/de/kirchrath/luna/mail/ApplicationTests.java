package de.kirchrath.luna.mail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import de.kirchrath.luna.mail.config.TestcontainersConfig;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
