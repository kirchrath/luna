package de.kirchrath.luna.note;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import de.kirchrath.luna.note.config.TestcontainersConfig;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
