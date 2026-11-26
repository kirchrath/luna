package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestcontainersConfig;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.entity.TaskPriority;
import de.kirchrath.luna.core.data.repository.AppointmentElementRepository;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.data.repository.TaskElementRepository;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "app.classification.interval=1h")
@Import({TestcontainersConfig.class, ChatModelTestConfig.class})
class ClassificationIntegrationTest {

    @Autowired
    private InboundEventApplicationService inboundEventApplicationService;

    @Autowired
    private ClassificationScheduler classificationScheduler;

    @Autowired
    private InboundEventRepository inboundEventRepository;

    @Autowired
    private OutboundEventRepository outboundEventRepository;

    @Autowired
    private TaskElementRepository taskElementRepository;

    @Autowired
    private AppointmentElementRepository appointmentElementRepository;

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        taskElementRepository.deleteAll();
        appointmentElementRepository.deleteAll();
        outboundEventRepository.deleteAll();
        inboundEventRepository.deleteAll();
        reset(chatModel);
        ChatModelTestConfig.stubOptions(chatModel);
    }

    @Test
    void thatRunningTheClassificationSchedulerShouldResultIntoStoredAndPublishedElements() {
        // given
        var reference = inboundEventApplicationService.createManualInboundEvent(
                new ManualInputDataDto("Dentist on Tuesday at 10, bring the bonus booklet and pay the invoice there", null));
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> answer(invocation.<Prompt>getArgument(0)));

        // when
        classificationScheduler.classifyReceivedEvents();

        // then
        var inboundEvent = inboundEventRepository.findById(reference.id()).orElseThrow();
        assertThat(inboundEvent.status()).isEqualTo(InboundEventStatus.ANALYZED);
        var outboundEvents = outboundEventRepository.findByInboundEventId(reference.id());
        assertThat(outboundEvents).extracting(OutboundEvent::type)
                .containsExactlyInAnyOrder(LunaElementType.APPOINTMENT, LunaElementType.TASK, LunaElementType.TASK);
        assertThat(outboundEvents).extracting(OutboundEvent::status).containsOnly(OutboundEventStatus.PUBLISHED);
        var taskEvents = outboundEvents.stream().filter(event -> event.type() == LunaElementType.TASK).toList();
        assertThat(taskEvents).extracting(event -> taskElementRepository.findByOutboundEventId(event.id())
                        .orElseThrow().title())
                .containsExactlyInAnyOrder("Bring the bonus booklet", "Pay the invoice");
        assertThat(taskElementRepository.findByOutboundEventId(taskEvents.getFirst().id()).orElseThrow().priority())
                .isEqualTo(TaskPriority.NORMAL);
        var appointmentEvent = outboundEvents.stream()
                .filter(event -> event.type() == LunaElementType.APPOINTMENT).findFirst().orElseThrow();
        assertThat(appointmentElementRepository.findByOutboundEventId(appointmentEvent.id())).isPresent();

        var records = consumeRecords(3, "luna_core_task", "luna_core_appointment");
        assertThat(records).containsOnlyKeys(
                taskEvents.get(0).id().toString(), taskEvents.get(1).id().toString(), appointmentEvent.id().toString());
        var appointmentMessage = jsonMapper.readTree(records.get(appointmentEvent.id().toString()));
        assertThat(appointmentMessage.get("id").asString()).isEqualTo(appointmentEvent.id().toString());
        assertThat(appointmentMessage.get("type").asString()).isEqualTo("APPOINTMENT");
        assertThat(appointmentMessage.get("data").get("title").asString()).isEqualTo("Dentist");
        assertThat(OffsetDateTime.parse(appointmentMessage.get("data").get("start").asString()))
                .isAtSameInstantAs(OffsetDateTime.parse("2026-12-01T10:00:00+01:00"));
        assertThat(appointmentMessage.get("audit").get("version").asLong()).isZero();
    }

    @Test
    void thatRunningTheClassificationSchedulerWithAFailingProviderShouldResultIntoAnotherAttemptLater() {
        // given
        var reference = inboundEventApplicationService.createManualInboundEvent(new ManualInputDataDto("Something", null));
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("provider unavailable"));

        // when
        classificationScheduler.classifyReceivedEvents();

        // then
        var inboundEvent = inboundEventRepository.findById(reference.id()).orElseThrow();
        assertThat(inboundEvent.status()).isEqualTo(InboundEventStatus.RECEIVED);
        assertThat(inboundEvent.attempts()).isEqualTo(1);
        assertThat(outboundEventRepository.findByInboundEventId(reference.id())).isEmpty();
    }

    private ChatResponse answer(Prompt prompt) {
        var contents = prompt.getContents();
        String json;
        if (contents.contains("You extract all tasks")) {
            json = "{\"items\": [{\"title\": \"Bring the bonus booklet\", \"priority\": \"NORMAL\"},"
                    + " {\"title\": \"Pay the invoice\", \"priority\": \"NORMAL\"}]}";
        } else if (contents.contains("You extract all appointments")) {
            json = "{\"items\": [{\"title\": \"Dentist\", \"start\": \"2026-12-01T10:00:00+01:00\","
                    + " \"allDay\": false}]}";
        } else {
            json = "{\"types\": [\"APPOINTMENT\", \"TASK\"]}";
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }

    private Map<String, String> consumeRecords(int count, String... topics) {
        var consumerProps = KafkaTestUtils.consumerProps(
                String.join(",", kafkaConnectionDetails.getBootstrapServers()), "integration-test");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer());
        try (var consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(List.of(topics));
            var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), count);
            var byKey = new java.util.HashMap<String, String>();
            records.forEach(record -> byKey.put(record.key(), record.value()));
            return byKey;
        }
    }
}
