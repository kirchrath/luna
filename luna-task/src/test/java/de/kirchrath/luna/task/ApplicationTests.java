package de.kirchrath.luna.task;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import de.kirchrath.luna.task.config.TestcontainersConfig;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
