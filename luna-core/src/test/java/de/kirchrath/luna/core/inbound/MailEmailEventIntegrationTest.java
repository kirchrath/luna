package de.kirchrath.luna.core.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestcontainersConfig;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.mail.EmailEventDataDto;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
        "app.classification.interval=1h",
        "app.kafka.retry.retries=1",
        "app.kafka.retry.delay=100ms",
        // A provider exported in the shell (e.g. SPRING_AI_MODEL_CHAT for the stack) must not replace the mock.
        "spring.ai.model.chat=none"})
@Import({TestcontainersConfig.class, ChatModelTestConfig.class})
class MailEmailEventIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String EMAIL_TOPIC = "luna_mail_email";
    private static final String DLT = "luna_core_dlt";

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Autowired
    private InboundEventRepository inboundEventRepository;

    @Autowired
    private OutboundEventRepository outboundEventRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private KafkaTemplate<UUID, String> mailProducer;
    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        outboundEventRepository.deleteAll();
        inboundEventRepository.deleteAll();
        var bootstrapServers = String.join(",", kafkaConnectionDetails.getBootstrapServers());
        mailProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(bootstrapServers), new UUIDSerializer(), new StringSerializer()));
        var consumerProps = KafkaTestUtils.consumerProps(bootstrapServers, "test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(List.of(DLT));
        await().atMost(TIMEOUT).until(() -> {
            consumer.poll(Duration.ofMillis(100));
            return !consumer.assignment().isEmpty();
        });
    }

    @AfterEach
    void tearDown() {
        consumer.close();
        mailProducer.destroy();
    }

    @Test
    void thatAnEmailShouldResultIntoAReceivedMailInboundEvent() {
        // given
        var emailId = UUID.randomUUID();

        // when
        sendEmail(emailId, "Invoice 4711");

        // then
        await().atMost(TIMEOUT).until(() -> inboundEventRepository.existsBySourceAndSourceId(EventSource.MAIL, emailId));
        var inboundEvent = inboundEventRepository.findAll().iterator().next();
        assertThat(inboundEvent.status()).isEqualTo(InboundEventStatus.RECEIVED);
        assertThat(jsonMapper.readValue(inboundEvent.data().json(), EmailEventDataDto.class).getSubject())
                .isEqualTo("Invoice 4711");
    }

    @Test
    void thatTheSameEmailTwiceShouldResultIntoOneInboundEvent() {
        // given
        var emailId = UUID.randomUUID();
        sendEmail(emailId, "Invoice 4711");
        await().atMost(TIMEOUT).until(() -> inboundEventRepository.existsBySourceAndSourceId(EventSource.MAIL, emailId));

        // when
        sendEmail(emailId, "Invoice 4711");
        sendEmail(UUID.randomUUID(), "Marker");

        // then
        await().atMost(TIMEOUT).until(() -> inboundEventRepository.count() == 2);
        assertThat(inboundEventRepository.count()).isEqualTo(2);
    }

    @Test
    void thatAnUnreadableEventShouldResultIntoTheDeadLetterTopic() {
        // given
        var key = UUID.randomUUID();

        // when
        mailProducer.send(EMAIL_TOPIC, key, "not json").join();

        // then
        var values = new ArrayList<String>();
        await().atMost(TIMEOUT).until(() -> {
            KafkaTestUtils.getRecords(consumer, Duration.ofMillis(200)).records(DLT)
                    .forEach(record -> values.add(record.value()));
            return !values.isEmpty();
        });
        assertThat(values).containsExactly("not json");
        assertThat(inboundEventRepository.count()).isZero();
    }

    private void sendEmail(UUID id, String subject) {
        var event = Map.of(
                "id", id,
                "audit", Map.of(
                        "createdDate", "2026-11-26T08:00:00+01:00",
                        "lastModifiedDate", "2026-11-26T08:00:00+01:00",
                        "version", 0),
                "data", Map.of(
                        "account", "private",
                        "address", "luna@luna.test",
                        "from", "Power company <billing@power.test>",
                        "to", List.of("luna@luna.test"),
                        "subject", subject,
                        "receivedDate", "2026-11-26T07:59:00+01:00",
                        "text", "Please pay by Friday.",
                        "textTruncated", false,
                        "attachments", List.of()));
        mailProducer.send(EMAIL_TOPIC, id, jsonMapper.writeValueAsString(event)).join();
    }
}
