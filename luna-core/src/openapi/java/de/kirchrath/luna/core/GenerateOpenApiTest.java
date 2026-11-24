package de.kirchrath.luna.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestcontainersConfig;

@SpringBootTest(properties = "app.classification.interval=1h")
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, ChatModelTestConfig.class})
class GenerateOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void thatGeneratingTheApiDocsShouldResultIntoAnOpenApiFile() throws Exception {
        // given
        var openApiFile = Path.of("openapi.yaml");

        // when
        var apiDocs = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Files.writeString(openApiFile, apiDocs);

        // then
        assertThat(openApiFile).hasContent(apiDocs);
    }
}
