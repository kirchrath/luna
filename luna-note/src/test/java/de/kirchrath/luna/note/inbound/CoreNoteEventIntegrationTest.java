package de.kirchrath.luna.note.inbound;

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

import de.kirchrath.luna.note.config.TestcontainersConfig;
import de.kirchrath.luna.note.data.entity.Note;
import de.kirchrath.luna.note.data.entity.NoteSource;
import de.kirchrath.luna.note.data.entity.NoteStatus;
import de.kirchrath.luna.note.data.repository.CoreNoteProjectionRepository;
import de.kirchrath.luna.note.data.repository.NoteRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"app.kafka.retry.retries=1", "app.kafka.retry.delay=100ms"})
@Import(TestcontainersConfig.class)
class CoreNoteEventIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String CORE_NOTE_TOPIC = "luna_core_note";
    private static final String NOTE_TOPIC = "luna_note_note";
    private static final String DLT = "luna_note_dlt";

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private CoreNoteProjectionRepository projectionRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private KafkaTemplate<UUID, String> coreProducer;
    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();
        projectionRepository.deleteAll();
        var bootstrapServers = String.join(",", kafkaConnectionDetails.getBootstrapServers());
        coreProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(bootstrapServers), new UUIDSerializer(), new StringSerializer()));
        var consumerProps = KafkaTestUtils.consumerProps(bootstrapServers, "test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(List.of(NOTE_TOPIC, DLT));
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
    void thatANewCoreNoteShouldResultIntoAnActiveNoteAndAPublishedNoteEvent() {
        // given
        var coreNoteId = UUID.randomUUID();

        // when
        sendCoreNote(coreNoteId, 0, "Owners meeting", "Roof will be renovated in May");

        // then
        var event = awaitRecord(NOTE_TOPIC);
        var note = noteRepository.findBySourceAndSourceId(NoteSource.CORE, coreNoteId).orElseThrow();
        assertThat(note.title()).isEqualTo("Owners meeting");
        assertThat(note.summary()).isEqualTo("Roof will be renovated in May");
        assertThat(note.status()).isEqualTo(NoteStatus.ACTIVE);
        assertThat(note.edited()).isFalse();
        assertThat(projectionRepository.findByCoreNoteId(coreNoteId).orElseThrow().version()).isZero();
        assertThat(event.get("id").asString()).isEqualTo(note.id().toString());
        assertThat(event.has("type")).isFalse();
        assertThat(event.get("data").get("title").asString()).isEqualTo("Owners meeting");
        assertThat(event.get("data").get("source").asString()).isEqualTo("CORE");
        assertThat(event.get("data").get("sourceId").asString()).isEqualTo(coreNoteId.toString());
        assertThat(event.get("audit").get("version").asLong()).isZero();
    }

    @Test
    void thatANewerVersionOfACoreNoteShouldResultIntoAnUpdatedNote() {
        // given
        var coreNoteId = UUID.randomUUID();
        sendCoreNote(coreNoteId, 0, "Owners meeting", "Roof will be renovated in May");
        awaitRecord(NOTE_TOPIC);

        // when
        sendCoreNote(coreNoteId, 1, "Owners meeting 2026", "Reserve increases by 20 euros");

        // then
        var event = awaitRecord(NOTE_TOPIC);
        var note = noteRepository.findBySourceAndSourceId(NoteSource.CORE, coreNoteId).orElseThrow();
        assertThat(note.title()).isEqualTo("Owners meeting 2026");
        assertThat(note.summary()).isEqualTo("Reserve increases by 20 euros");
        assertThat(note.version()).isEqualTo(1L);
        assertThat(projectionRepository.findByCoreNoteId(coreNoteId).orElseThrow().version()).isEqualTo(1L);
        assertThat(event.get("audit").get("version").asLong()).isEqualTo(1L);
    }

    @Test
    void thatAnOlderOrEqualVersionOfACoreNoteShouldResultIntoAnUnchangedNote() {
        // given
        var coreNoteId = UUID.randomUUID();
        sendCoreNote(coreNoteId, 2, "Owners meeting", "Roof will be renovated in May");
        awaitRecord(NOTE_TOPIC);

        // when
        sendCoreNote(coreNoteId, 2, "Same version", "Reserve increases by 20 euros");
        sendCoreNote(coreNoteId, 1, "Older version", "Reserve increases by 20 euros");
        sendCoreNote(coreNoteId, 3, "Newer version", "Reserve increases by 20 euros");

        // then
        var event = awaitRecord(NOTE_TOPIC);
        assertThat(event.get("data").get("title").asString()).isEqualTo("Newer version");
        var note = noteRepository.findBySourceAndSourceId(NoteSource.CORE, coreNoteId).orElseThrow();
        assertThat(note.version()).isEqualTo(1L);
        assertThat(projectionRepository.findByCoreNoteId(coreNoteId).orElseThrow().version()).isEqualTo(3L);
    }

    @Test
    void thatANewerVersionOfAnEditedNoteShouldResultIntoTheKeptUserChanges() {
        // given
        var coreNoteId = UUID.randomUUID();
        sendCoreNote(coreNoteId, 0, "Owners meeting", "Roof will be renovated in May");
        awaitRecord(NOTE_TOPIC);
        var note = noteRepository.findBySourceAndSourceId(NoteSource.CORE, coreNoteId).orElseThrow();
        noteRepository.save(note.toBuilder().title("Edited by the user").edited(true).build());

        // when
        sendCoreNote(coreNoteId, 1, "Owners meeting 2026", "Reserve increases by 20 euros");

        // then
        await().atMost(TIMEOUT).until(
                () -> projectionRepository.findByCoreNoteId(coreNoteId).orElseThrow().version() == 1L);
        assertThat(noteRepository.findById(note.id())).get().extracting(Note::title).isEqualTo("Edited by the user");
        assertThat(KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1)).count()).isZero();
    }

    @Test
    void thatAnUnreadableCoreNoteShouldResultIntoTheDeadLetterTopicAfterTheRetries() {
        // given
        var coreNoteId = UUID.randomUUID();

        // when
        coreProducer.send(new ProducerRecord<>(CORE_NOTE_TOPIC, coreNoteId, "no json")).join();

        // then
        var record = awaitRecords(DLT).getFirst();
        assertThat(record).isEqualTo("no json");
        assertThat(projectionRepository.findByCoreNoteId(coreNoteId)).isEmpty();
    }

    @Test
    void thatAFailingCoreNoteShouldResultIntoTheDeadLetterTopicAfterTheRetries() {
        // given
        var coreNoteId = UUID.randomUUID();

        // when
        sendCoreNote(coreNoteId, 0, null, "Roof will be renovated in May");

        // then
        var event = jsonMapper.readTree(awaitRecords(DLT).getFirst());
        assertThat(event.get("id").asString()).isEqualTo(coreNoteId.toString());
        assertThat(noteRepository.findBySourceAndSourceId(NoteSource.CORE, coreNoteId)).isEmpty();
        assertThat(projectionRepository.findByCoreNoteId(coreNoteId)).isEmpty();
    }

    private void sendCoreNote(UUID coreNoteId, long version, String title, String summary) {
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("title", title);
        data.put("summary", summary);
        var event = Map.of(
                "type", "NOTE",
                "id", coreNoteId,
                "audit", Map.of(
                        "createdDate", "2026-11-26T10:00:00+01:00",
                        "lastModifiedDate", "2026-11-26T10:00:00+01:00",
                        "version", version),
                "data", data);
        coreProducer.send(CORE_NOTE_TOPIC, coreNoteId, jsonMapper.writeValueAsString(event)).join();
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
