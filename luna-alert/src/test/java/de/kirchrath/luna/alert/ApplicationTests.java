package de.kirchrath.luna.alert;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import de.kirchrath.luna.alert.config.TestcontainersConfig;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
