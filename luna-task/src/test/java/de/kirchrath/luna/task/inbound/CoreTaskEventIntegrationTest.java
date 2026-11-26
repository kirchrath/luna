package de.kirchrath.luna.task.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import de.kirchrath.luna.task.config.TestcontainersConfig;
import de.kirchrath.luna.task.data.entity.Task;
import de.kirchrath.luna.task.data.entity.TaskPriority;
import de.kirchrath.luna.task.data.entity.TaskSource;
import de.kirchrath.luna.task.data.entity.TaskStatus;
import de.kirchrath.luna.task.data.repository.CoreTaskProjectionRepository;
import de.kirchrath.luna.task.data.repository.TaskRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"app.kafka.retry.retries=1", "app.kafka.retry.delay=100ms"})
@Import(TestcontainersConfig.class)
class CoreTaskEventIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String CORE_TASK_TOPIC = "luna_core_task";
    private static final String TASK_TOPIC = "luna_task_task";
    private static final String DLT = "luna_task_dlt";

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CoreTaskProjectionRepository projectionRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private KafkaTemplate<UUID, String> coreProducer;
    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectionRepository.deleteAll();
        var bootstrapServers = String.join(",", kafkaConnectionDetails.getBootstrapServers());
        coreProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(bootstrapServers), new UUIDSerializer(), new StringSerializer()));
        var consumerProps = KafkaTestUtils.consumerProps(bootstrapServers, "test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(List.of(TASK_TOPIC, DLT));
        await().atMost(TIMEOUT).until(() -> {
            consumer.poll(Duration.ofMillis(100));
            return !consumer.assignment().isEmpty();
        });
    }

    @AfterEach
    void tearDown() {
        consumer.close();
        coreProducer.destroy();
    }

    @Test
    void thatANewCoreTaskShouldResultIntoAnOpenTaskAndAPublishedTaskEvent() {
        // given
        var coreTaskId = UUID.randomUUID();

        // when
        sendCoreTask(coreTaskId, 0, "Pay the electricity bill", "HIGH");

        // then
        var event = awaitRecord(TASK_TOPIC);
        var task = taskRepository.findBySourceAndSourceId(TaskSource.CORE, coreTaskId).orElseThrow();
        assertThat(task.title()).isEqualTo("Pay the electricity bill");
        assertThat(task.description()).isEqualTo("Before the end of the month");
        assertThat(task.dueDate()).isEqualTo(OffsetDateTime.parse("2026-11-30T12:00:00+01:00"));
        assertThat(task.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.status()).isEqualTo(TaskStatus.OPEN);
        assertThat(task.edited()).isFalse();
        assertThat(projectionRepository.findByCoreTaskId(coreTaskId).orElseThrow().version()).isZero();
        assertThat(event.get("id").asString()).isEqualTo(task.id().toString());
        assertThat(event.has("type")).isFalse();
        assertThat(event.get("data").get("title").asString()).isEqualTo("Pay the electricity bill");
        assertThat(event.get("data").get("source").asString()).isEqualTo("CORE");
        assertThat(event.get("data").get("sourceId").asString()).isEqualTo(coreTaskId.toString());
        assertThat(event.get("audit").get("version").asLong()).isZero();
    }

    @Test
    void thatANewerVersionOfACoreTaskShouldResultIntoAnUpdatedTask() {
        // given
        var coreTaskId = UUID.randomUUID();
        sendCoreTask(coreTaskId, 0, "Pay the electricity bill", "HIGH");
        awaitRecord(TASK_TOPIC);

        // when
        sendCoreTask(coreTaskId, 1, "Pay the gas bill", "LOW");

        // then
        var event = awaitRecord(TASK_TOPIC);
        var task = taskRepository.findBySourceAndSourceId(TaskSource.CORE, coreTaskId).orElseThrow();
        assertThat(task.title()).isEqualTo("Pay the gas bill");
        assertThat(task.priority()).isEqualTo(TaskPriority.LOW);
        assertThat(task.version()).isEqualTo(1L);
        assertThat(projectionRepository.findByCoreTaskId(coreTaskId).orElseThrow().version()).isEqualTo(1L);
        assertThat(event.get("audit").get("version").asLong()).isEqualTo(1L);
    }

    @Test
    void thatAnOlderOrEqualVersionOfACoreTaskShouldResultIntoAnUnchangedTask() {
        // given
        var coreTaskId = UUID.randomUUID();
        sendCoreTask(coreTaskId, 2, "Pay the electricity bill", "HIGH");
        awaitRecord(TASK_TOPIC);

        // when
        sendCoreTask(coreTaskId, 2, "Same version", "LOW");
        sendCoreTask(coreTaskId, 1, "Older version", "LOW");
        sendCoreTask(coreTaskId, 3, "Newer version", "LOW");

        // then
        var event = awaitRecord(TASK_TOPIC);
        assertThat(event.get("data").get("title").asString()).isEqualTo("Newer version");
        var task = taskRepository.findBySourceAndSourceId(TaskSource.CORE, coreTaskId).orElseThrow();
        assertThat(task.version()).isEqualTo(1L);
        assertThat(projectionRepository.findByCoreTaskId(coreTaskId).orElseThrow().version()).isEqualTo(3L);
    }

    @Test
    void thatANewerVersionOfAnEditedTaskShouldResultIntoTheKeptUserChanges() {
        // given
        var coreTaskId = UUID.randomUUID();
        sendCoreTask(coreTaskId, 0, "Pay the electricity bill", "HIGH");
        awaitRecord(TASK_TOPIC);
        var task = taskRepository.findBySourceAndSourceId(TaskSource.CORE, coreTaskId).orElseThrow();
        taskRepository.save(task.toBuilder().title("Edited by the user").edited(true).build());

        // when
        sendCoreTask(coreTaskId, 1, "Pay the gas bill", "LOW");

        // then
        await().atMost(TIMEOUT).until(
                () -> projectionRepository.findByCoreTaskId(coreTaskId).orElseThrow().version() == 1L);
        assertThat(taskRepository.findById(task.id())).get().extracting(Task::title).isEqualTo("Edited by the user");
        assertThat(KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1)).count()).isZero();
    }

    @Test
    void thatAnUnreadableCoreTaskShouldResultIntoTheDeadLetterTopicAfterTheRetries() {
        // given
        var coreTaskId = UUID.randomUUID();

        // when
        coreProducer.send(new ProducerRecord<>(CORE_TASK_TOPIC, coreTaskId, "no json")).join();

        // then
        var record = awaitRecords(DLT).getFirst();
        assertThat(record).isEqualTo("no json");
        assertThat(projectionRepository.findByCoreTaskId(coreTaskId)).isEmpty();
    }

    @Test
    void thatAFailingCoreTaskShouldResultIntoTheDeadLetterTopicAfterTheRetries() {
        // given
        var coreTaskId = UUID.randomUUID();

        // when
        sendCoreTask(coreTaskId, 0, null, "HIGH");

        // then
        var event = jsonMapper.readTree(awaitRecords(DLT).getFirst());
        assertThat(event.get("id").asString()).isEqualTo(coreTaskId.toString());
        assertThat(taskRepository.findBySourceAndSourceId(TaskSource.CORE, coreTaskId)).isEmpty();
        assertThat(projectionRepository.findByCoreTaskId(coreTaskId)).isEmpty();
    }

    private void sendCoreTask(UUID coreTaskId, long version, String title, String priority) {
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("title", title);
        data.put("description", "Before the end of the month");
        data.put("dueDate", "2026-11-30T12:00:00+01:00");
        data.put("priority", priority);
        var event = Map.of(
                "type", "TASK",
                "id", coreTaskId,
                "audit", Map.of(
                        "createdDate", "2026-11-26T10:00:00+01:00",
                        "lastModifiedDate", "2026-11-26T10:00:00+01:00",
                        "version", version),
                "data", data);
        coreProducer.send(CORE_TASK_TOPIC, coreTaskId, jsonMapper.writeValueAsString(event)).join();
    }

    private JsonNode awaitRecord(String topic) {
        return jsonMapper.readTree(awaitRecords(topic).getFirst());
    }

    private List<String> awaitRecords(String topic) {
        var values = new ArrayList<String>();
        await().atMost(TIMEOUT).until(() -> {
            KafkaTestUtils.getRecords(consumer, Duration.ofMillis(200)).records(topic)
                    .forEach(record -> values.add(record.value()));
            return !values.isEmpty();
        });
        return values;
    }
}
