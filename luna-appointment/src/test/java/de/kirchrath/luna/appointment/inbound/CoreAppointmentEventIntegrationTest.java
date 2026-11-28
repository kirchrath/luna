package de.kirchrath.luna.appointment.inbound;

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

import de.kirchrath.luna.appointment.config.TestcontainersConfig;
import de.kirchrath.luna.appointment.data.entity.Appointment;
import de.kirchrath.luna.appointment.data.entity.AppointmentSource;
import de.kirchrath.luna.appointment.data.entity.AppointmentStatus;
import de.kirchrath.luna.appointment.data.entity.LifeArea;
import de.kirchrath.luna.appointment.data.repository.CoreAppointmentProjectionRepository;
import de.kirchrath.luna.appointment.data.repository.AppointmentRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"app.kafka.retry.retries=1", "app.kafka.retry.delay=100ms"})
@Import(TestcontainersConfig.class)
class CoreAppointmentEventIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final OffsetDateTime START = OffsetDateTime.parse("2026-12-01T10:00:00+01:00");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-12-01T11:00:00+01:00");
    private static final String CORE_APPOINTMENT_TOPIC = "luna_core_appointment";
    private static final String APPOINTMENT_TOPIC = "luna_appointment_appointment";
    private static final String DLT = "luna_appointment_dlt";

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CoreAppointmentProjectionRepository projectionRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private KafkaTemplate<UUID, String> coreProducer;
    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        projectionRepository.deleteAll();
        var bootstrapServers = String.join(",", kafkaConnectionDetails.getBootstrapServers());
        coreProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(bootstrapServers), new UUIDSerializer(), new StringSerializer()));
        var consumerProps = KafkaTestUtils.consumerProps(bootstrapServers, "test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(List.of(APPOINTMENT_TOPIC, DLT));
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
    void thatANewCoreAppointmentShouldResultIntoAScheduledAppointmentAndAPublishedAppointmentEvent() {
        // given
        var coreAppointmentId = UUID.randomUUID();

        // when
        sendCoreAppointment(coreAppointmentId, 0, "Dentist", "Practice at the market");

        // then
        var event = awaitRecord(APPOINTMENT_TOPIC);
        var appointment = appointmentRepository.findBySourceAndSourceId(AppointmentSource.CORE, coreAppointmentId).orElseThrow();
        assertThat(appointment.title()).isEqualTo("Dentist");
        assertThat(appointment.location()).isEqualTo("Practice at the market");
        assertThat(appointment.start()).isEqualTo(START);
        assertThat(appointment.end()).isEqualTo(END);
        assertThat(appointment.allDay()).isFalse();
        assertThat(appointment.effectiveEnd()).isEqualTo(END);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointment.edited()).isFalse();
        assertThat(appointment.area()).isEqualTo(LifeArea.WORK);
        assertThat(projectionRepository.findByCoreAppointmentId(coreAppointmentId).orElseThrow().version()).isZero();
        assertThat(event.get("id").asString()).isEqualTo(appointment.id().toString());
        assertThat(event.has("type")).isFalse();
        assertThat(event.get("data").get("title").asString()).isEqualTo("Dentist");
        assertThat(event.get("data").get("source").asString()).isEqualTo("CORE");
        assertThat(event.get("data").get("sourceId").asString()).isEqualTo(coreAppointmentId.toString());
        assertThat(event.get("data").get("area").asString()).isEqualTo("WORK");
        assertThat(event.get("audit").get("version").asLong()).isZero();
    }

    @Test
    void thatANewerVersionOfACoreAppointmentShouldResultIntoAnUpdatedAppointment() {
        // given
        var coreAppointmentId = UUID.randomUUID();
        sendCoreAppointment(coreAppointmentId, 0, "Dentist", "Practice at the market");
        awaitRecord(APPOINTMENT_TOPIC);

        // when
        sendCoreAppointment(coreAppointmentId, 1, "Dentist Dr. Weber", "New practice");

        // then
        var event = awaitRecord(APPOINTMENT_TOPIC);
        var appointment = appointmentRepository.findBySourceAndSourceId(AppointmentSource.CORE, coreAppointmentId).orElseThrow();
        assertThat(appointment.title()).isEqualTo("Dentist Dr. Weber");
        assertThat(appointment.location()).isEqualTo("New practice");
        assertThat(appointment.version()).isEqualTo(1L);
        assertThat(projectionRepository.findByCoreAppointmentId(coreAppointmentId).orElseThrow().version()).isEqualTo(1L);
        assertThat(event.get("audit").get("version").asLong()).isEqualTo(1L);
    }

    @Test
    void thatAnOlderOrEqualVersionOfACoreAppointmentShouldResultIntoAnUnchangedAppointment() {
        // given
        var coreAppointmentId = UUID.randomUUID();
        sendCoreAppointment(coreAppointmentId, 2, "Dentist", "Practice at the market");
        awaitRecord(APPOINTMENT_TOPIC);

        // when
        sendCoreAppointment(coreAppointmentId, 2, "Same version", "New practice");
        sendCoreAppointment(coreAppointmentId, 1, "Older version", "New practice");
        sendCoreAppointment(coreAppointmentId, 3, "Newer version", "New practice");

        // then
        var event = awaitRecord(APPOINTMENT_TOPIC);
        assertThat(event.get("data").get("title").asString()).isEqualTo("Newer version");
        var appointment = appointmentRepository.findBySourceAndSourceId(AppointmentSource.CORE, coreAppointmentId).orElseThrow();
        assertThat(appointment.version()).isEqualTo(1L);
        assertThat(projectionRepository.findByCoreAppointmentId(coreAppointmentId).orElseThrow().version()).isEqualTo(3L);
    }

    @Test
    void thatANewerVersionOfAnEditedAppointmentShouldResultIntoTheKeptUserChanges() {
        // given
        var coreAppointmentId = UUID.randomUUID();
        sendCoreAppointment(coreAppointmentId, 0, "Dentist", "Practice at the market");
        awaitRecord(APPOINTMENT_TOPIC);
        var appointment = appointmentRepository.findBySourceAndSourceId(AppointmentSource.CORE, coreAppointmentId).orElseThrow();
        appointmentRepository.save(appointment.toBuilder().title("Edited by the user").edited(true).build());

        // when
        sendCoreAppointment(coreAppointmentId, 1, "Dentist Dr. Weber", "New practice");

        // then
        await().atMost(TIMEOUT).until(
                () -> projectionRepository.findByCoreAppointmentId(coreAppointmentId).orElseThrow().version() == 1L);
        assertThat(appointmentRepository.findById(appointment.id())).get().extracting(Appointment::title).isEqualTo("Edited by the user");
        assertThat(KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1)).count()).isZero();
    }

    @Test
    void thatAnUnreadableCoreAppointmentShouldResultIntoTheDeadLetterTopicAfterTheRetries() {
        // given
        var coreAppointmentId = UUID.randomUUID();

        // when
        coreProducer.send(new ProducerRecord<>(CORE_APPOINTMENT_TOPIC, coreAppointmentId, "no json")).join();

        // then
        var record = awaitRecords(DLT).getFirst();
        assertThat(record).isEqualTo("no json");
        assertThat(projectionRepository.findByCoreAppointmentId(coreAppointmentId)).isEmpty();
    }

    @Test
    void thatAFailingCoreAppointmentShouldResultIntoTheDeadLetterTopicAfterTheRetries() {
        // given
        var coreAppointmentId = UUID.randomUUID();

        // when
        sendCoreAppointment(coreAppointmentId, 0, null, "Practice at the market");

        // then
        var event = jsonMapper.readTree(awaitRecords(DLT).getFirst());
        assertThat(event.get("id").asString()).isEqualTo(coreAppointmentId.toString());
        assertThat(appointmentRepository.findBySourceAndSourceId(AppointmentSource.CORE, coreAppointmentId)).isEmpty();
        assertThat(projectionRepository.findByCoreAppointmentId(coreAppointmentId)).isEmpty();
    }

    private void sendCoreAppointment(UUID coreAppointmentId, long version, String title, String location) {
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("title", title);
        data.put("start", START.toString());
        data.put("end", END.toString());
        data.put("allDay", false);
        data.put("location", location);
        var event = Map.of(
                "type", "APPOINTMENT",
                "id", coreAppointmentId,
                "area", "WORK",
                "audit", Map.of(
                        "createdDate", "2026-11-26T10:00:00+01:00",
                        "lastModifiedDate", "2026-11-26T10:00:00+01:00",
                        "version", version),
                "data", data);
        coreProducer.send(CORE_APPOINTMENT_TOPIC, coreAppointmentId, jsonMapper.writeValueAsString(event)).join();
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
