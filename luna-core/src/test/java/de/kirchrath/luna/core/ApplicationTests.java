package de.kirchrath.luna.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestcontainersConfig;

@SpringBootTest(properties = "app.classification.interval=1h")
@Import({TestcontainersConfig.class, ChatModelTestConfig.class})
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
