package de.kirchrath.luna.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestcontainersConfig;
import de.kirchrath.luna.core.config.TopicProperties;
import de.kirchrath.luna.core.dto.AlertEventDto;
import de.kirchrath.luna.core.dto.AppointmentEventDto;
import de.kirchrath.luna.core.dto.ContractEventDto;
import de.kirchrath.luna.core.dto.ElementEventDto;
import de.kirchrath.luna.core.dto.InvoiceEventDto;
import de.kirchrath.luna.core.dto.NoteEventDto;
import de.kirchrath.luna.core.dto.TaskEventDto;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;

@SpringBootTest(properties = "app.classification.interval=1h")
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, ChatModelTestConfig.class})
class GenerateOpenApiTest {

    /** Services that consume events of luna-core and generate their Java types from a copy of kafka-api.yaml. */
    private static final List<Path> KAFKA_API_CONSUMER_COPIES =
            List.of(
                    Path.of("../luna-task/src/main/resources/kafka-api/luna-core.yaml"),
                    Path.of("../luna-alert/src/main/resources/kafka-api/luna-core.yaml"),
                    Path.of("../luna-note/src/main/resources/kafka-api/luna-core.yaml"));

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
    void thatGeneratingTheKafkaApiDocsShouldResultIntoAKafkaApiFile() throws Exception {
        // given
        var kafkaApiFile = Path.of("kafka-api.yaml");
        var topics = new LinkedHashMap<Class<?>, String>();
        topics.put(TaskEventDto.class, topicProperties.task());
        topics.put(AlertEventDto.class, topicProperties.alert());
        topics.put(AppointmentEventDto.class, topicProperties.appointment());
        topics.put(InvoiceEventDto.class, topicProperties.invoice());
        topics.put(ContractEventDto.class, topicProperties.contract());
        topics.put(NoteEventDto.class, topicProperties.note());
        var kafkaApi = new OpenAPI(SpecVersion.V31)
                .openapi("3.1.0")
                .info(new Info()
                        .title("Luna Core Kafka API")
                        .version(version)
                        .description("Events published by luna-core on Kafka. The key of each message is the id of the event."))
                .components(new Components());

        // when
        ModelConverters.getInstance(true).readAll(ElementEventDto.class).forEach(kafkaApi.getComponents()::addSchemas);
        topics.forEach((type, topic) -> {
            ModelConverters.getInstance(true).readAll(type).forEach(kafkaApi.getComponents()::addSchemas);
            kafkaApi.getComponents().getSchemas().get(type.getSimpleName())
                    .description("Published on topic " + topic + ". The key of each message is the id of the event.");
        });
        var kafkaApiContent = Yaml31.pretty(kafkaApi);
        Files.writeString(kafkaApiFile, kafkaApiContent);
        for (var copy : KAFKA_API_CONSUMER_COPIES) {
            Files.createDirectories(copy.getParent());
            Files.writeString(copy, kafkaApiContent);
        }

        // then
        assertThat(kafkaApiFile).content()
                .contains("ElementEventDto", "discriminator", "TaskEventDto", "NoteEventDto", "Published on topic " + topicProperties.task());
        assertThat(KAFKA_API_CONSUMER_COPIES).allSatisfy(copy -> assertThat(copy).hasContent(kafkaApiContent));
    }
}
