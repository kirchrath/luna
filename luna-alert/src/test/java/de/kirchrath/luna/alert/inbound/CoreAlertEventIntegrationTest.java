package de.kirchrath.luna.alert.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
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

import de.kirchrath.luna.alert.config.TestcontainersConfig;
import de.kirchrath.luna.alert.data.entity.AlertSeverity;
import de.kirchrath.luna.alert.data.entity.AlertSource;
import de.kirchrath.luna.alert.data.entity.AlertStatus;
import de.kirchrath.luna.alert.data.repository.CoreAlertProjectionRepository;
import de.kirchrath.luna.alert.data.repository.AlertRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"app.kafka.retry.retries=1", "app.kafka.retry.delay=100ms"})
@Import(TestcontainersConfig.class)
class CoreAlertEventIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String CORE_ALERT_TOPIC = "luna_core_alert";
    private static final String ALERT_TOPIC = "luna_alert_alert";
    private static final String DLT = "luna_alert_dlt";

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CoreAlertProjectionRepository projectionRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private KafkaTemplate<UUID, String> coreProducer;
    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        projectionRepository.deleteAll();
        var bootstrapServers = String.join(",", kafkaConnectionDetails.getBootstrapServers());
        coreProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(bootstrapServers), new UUIDSerializer(), new StringSerializer()));
        var consumerProps = KafkaTestUtils.consumerProps(bootstrapServers, "test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(List.of(ALERT_TOPIC, DLT));
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
    void thatANewCoreAlertShouldResultIntoAnOpenAlertAndAPublishedAlertEvent() {
        // given
        var coreAlertId = UUID.randomUUID();

        // when
        sendCoreAlert(coreAlertId, 0, "NAS disk almost full", "CRITICAL");

        // then
        var event = awaitRecord(ALERT_TOPIC);
        var alert = alertRepository.findBySourceAndSourceId(AlertSource.CORE, coreAlertId).orElseThrow();
        assertThat(alert.title()).isEqualTo("NAS disk almost full");
        assertThat(alert.message()).isEqualTo("Volume 1 is 94 % full");
        assertThat(alert.affectedSystem()).isEqualTo("nas01");
        assertThat(alert.severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alert.severityRank()).isZero();
        assertThat(alert.status()).isEqualTo(AlertStatus.OPEN);
        assertThat(projectionRepository.findByCoreAlertId(coreAlertId).orElseThrow().version()).isZero();
        assertThat(event.get("id").asString()).isEqualTo(alert.id().toString());
        assertThat(event.has("type")).isFalse();
        assertThat(event.get("data").get("title").asString()).isEqualTo("NAS disk almost full");
        assertThat(event.get("data").get("source").asString()).isEqualTo("CORE");
        assertThat(event.get("data").get("sourceId").asString()).isEqualTo(coreAlertId.toString());
        assertThat(event.get("audit").get("version").asLong()).isZero();
    }

    @Test
    void thatANewerVersionOfACoreAlertShouldResultIntoAnUpdatedAlert() {
        // given
        var coreAlertId = UUID.randomUUID();
        sendCoreAlert(coreAlertId, 0, "NAS disk almost full", "CRITICAL");
        awaitRecord(ALERT_TOPIC);

        // when
        sendCoreAlert(coreAlertId, 1, "NAS disk full", "INFO");

        // then
        var event = awaitRecord(ALERT_TOPIC);
        var alert = alertRepository.findBySourceAndSourceId(AlertSource.CORE, coreAlertId).orElseThrow();
        assertThat(alert.title()).isEqualTo("NAS disk full");
        assertThat(alert.severity()).isEqualTo(AlertSeverity.INFO);
        assertThat(alert.severityRank()).isEqualTo(2);
        assertThat(alert.version()).isEqualTo(1L);
        assertThat(projectionRepository.findByCoreAlertId(coreAlertId).orElseThrow().version()).isEqualTo(1L);
        assertThat(event.get("audit").get("version").asLong()).isEqualTo(1L);
    }

    @Test
    void thatAnOlderOrEqualVersionOfACoreAlertShouldResultIntoAnUnchangedAlert() {
        // given
        var coreAlertId = UUID.randomUUID();
        sendCoreAlert(coreAlertId, 2, "NAS disk almost full", "CRITICAL");
        awaitRecord(ALERT_TOPIC);

        // when
        sendCoreAlert(coreAlertId, 2, "Same version", "INFO");
        sendCoreAlert(coreAlertId, 1, "Older version", "INFO");
        sendCoreAlert(coreAlertId, 3, "Newer version", "INFO");

        // then
        var event = awaitRecord(ALERT_TOPIC);
        assertThat(event.get("data").get("title").asString()).isEqualTo("Newer version");
        var alert = alertRepository.findBySourceAndSourceId(AlertSource.CORE, coreAlertId).orElseThrow();
        assertThat(alert.version()).isEqualTo(1L);
        assertThat(projectionRepository.findByCoreAlertId(coreAlertId).orElseThrow().version()).isEqualTo(3L);
    }

    @Test
    void thatANewerVersionOfAnAcknowledgedAlertShouldResultIntoNewContentWithTheKeptStatus() {
        // given
        var coreAlertId = UUID.randomUUID();
        sendCoreAlert(coreAlertId, 0, "NAS disk almost full", "CRITICAL");
        awaitRecord(ALERT_TOPIC);
        var alert = alertRepository.findBySourceAndSourceId(AlertSource.CORE, coreAlertId).orElseThrow();
        alertRepository.save(alert.toBuilder().status(AlertStatus.ACKNOWLEDGED).build());

        // when
        sendCoreAlert(coreAlertId, 1, "NAS disk full", "INFO");

        // then
        var event = awaitRecord(ALERT_TOPIC);
        var updated = alertRepository.findById(alert.id()).orElseThrow();
        assertThat(updated.title()).isEqualTo("NAS disk full");
        assertThat(updated.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(event.get("data").get("status").asString()).isEqualTo("ACKNOWLEDGED");
    }

    @Test
    void thatAnUnreadableCoreAlertShouldResultIntoTheDeadLetterTopicAfterTheRetries() {
        // given
        var coreAlertId = UUID.randomUUID();

        // when
        coreProducer.send(new ProducerRecord<>(CORE_ALERT_TOPIC, coreAlertId, "no json")).join();

        // then
        var record = awaitRecords(DLT).getFirst();
        assertThat(record).isEqualTo("no json");
        assertThat(projectionRepository.findByCoreAlertId(coreAlertId)).isEmpty();
    }

    @Test
    void thatAFailingCoreAlertShouldResultIntoTheDeadLetterTopicAfterTheRetries() {
        // given
        var coreAlertId = UUID.randomUUID();

        // when
        sendCoreAlert(coreAlertId, 0, null, "CRITICAL");

        // then
        var event = jsonMapper.readTree(awaitRecords(DLT).getFirst());
        assertThat(event.get("id").asString()).isEqualTo(coreAlertId.toString());
        assertThat(alertRepository.findBySourceAndSourceId(AlertSource.CORE, coreAlertId)).isEmpty();
        assertThat(projectionRepository.findByCoreAlertId(coreAlertId)).isEmpty();
    }

    private void sendCoreAlert(UUID coreAlertId, long version, String title, String severity) {
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("title", title);
        data.put("message", "Volume 1 is 94 % full");
        data.put("severity", severity);
        data.put("affectedSystem", "nas01");
        var event = Map.of(
                "type", "ALERT",
                "id", coreAlertId,
                "audit", Map.of(
                        "createdDate", "2026-11-26T10:00:00+01:00",
                        "lastModifiedDate", "2026-11-26T10:00:00+01:00",
                        "version", version),
                "data", data);
        coreProducer.send(CORE_ALERT_TOPIC, coreAlertId, jsonMapper.writeValueAsString(event)).join();
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
