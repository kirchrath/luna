package de.kirchrath.luna.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import de.kirchrath.luna.mail.config.TestcontainersConfig;
import de.kirchrath.luna.mail.config.TopicProperties;
import de.kirchrath.luna.mail.dto.EmailEventDto;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class GenerateOpenApiTest {

    private static final List<Path> KAFKA_API_CONSUMER_COPIES =
            List.of(Path.of("../luna-core/src/main/resources/kafka-api/luna-mail.yaml"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TopicProperties topicProperties;

    @Value("${spring.application.version}")
    private String version;

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

    @Test
    void thatGeneratingTheKafkaApiDocsShouldResultIntoAKafkaApiFileAndItsCopies() throws Exception {
        // given
        var kafkaApiFile = Path.of("kafka-api.yaml");
        var description = "Published on topic " + topicProperties.email() + ". The key of each message is the id of the event.";
        var kafkaApi = new OpenAPI(SpecVersion.V31)
                .openapi("3.1.0")
                .info(new Info()
                        .title("Luna Mail Kafka API")
                        .version(version)
                        .description("Events published by luna-mail on Kafka. The key of each message is the id of the event."))
                .components(new Components());

        // when
        ModelConverters.getInstance(true).readAll(EmailEventDto.class).forEach(kafkaApi.getComponents()::addSchemas);
        kafkaApi.getComponents().getSchemas().get(EmailEventDto.class.getSimpleName()).description(description);
        var kafkaApiContent = Yaml31.pretty(kafkaApi);
        Files.writeString(kafkaApiFile, kafkaApiContent);
        for (var copy : KAFKA_API_CONSUMER_COPIES) {
            Files.createDirectories(copy.getParent());
            Files.writeString(copy, kafkaApiContent);
        }

        // then
        assertThat(kafkaApiFile).content().contains("EmailEventDto", "EmailEventDataDto", "Published on topic " + topicProperties.email());
        assertThat(KAFKA_API_CONSUMER_COPIES).allSatisfy(copy -> assertThat(copy).hasContent(kafkaApiContent));
    }
}
