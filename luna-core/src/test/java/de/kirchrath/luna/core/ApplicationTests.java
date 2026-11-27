package de.kirchrath.luna.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestcontainersConfig;

@SpringBootTest(properties = {
        "app.classification.interval=1h",
        // A provider exported in the shell (e.g. SPRING_AI_MODEL_CHAT for the stack) must not replace the mock.
        "spring.ai.model.chat=none"})
@Import({TestcontainersConfig.class, ChatModelTestConfig.class})
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
