package de.kirchrath.luna.mail.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.containers.GenericContainer;

import de.kirchrath.luna.mail.config.GreenMailConfig;
import de.kirchrath.luna.mail.config.TestcontainersConfig;
import de.kirchrath.luna.mail.data.entity.MailboxStatus;
import de.kirchrath.luna.mail.data.repository.EmailRepository;
import de.kirchrath.luna.mail.data.repository.MailboxRepository;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "app.mail.poll-interval=1h")
@Import({TestcontainersConfig.class, GreenMailConfig.class})
class MailPollingIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String EMAIL_TOPIC = "luna_mail_email";

    @Autowired
    private MailApplicationService mailApplicationService;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Autowired
    private GenericContainer<?> greenMailContainer;

    @Autowired
    private JsonMapper jsonMapper;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        emailRepository.deleteAll();
        mailboxRepository.deleteAll();
        var bootstrapServers = String.join(",", kafkaConnectionDetails.getBootstrapServers());
        var consumerProps = KafkaTestUtils.consumerProps(bootstrapServers, "test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(List.of(EMAIL_TOPIC));
        await().atMost(TIMEOUT).until(() -> {
            consumer.poll(Duration.ofMillis(100));
            return !consumer.assignment().isEmpty();
        });
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void thatListingTheAccountsShouldResultIntoAllConfiguredAccountsByName() {
        // when
        var names = mailApplicationService.findAccountNames();

        // then
        assertThat(names).containsExactly("broken", "other", "test");
    }

    @Test
    void thatTheFirstPollShouldResultIntoARememberedStateWithoutOlderEmails() throws Exception {
        // given
        send(GreenMailConfig.TEST_ADDRESS, "Older email", plainText("Sent before Luna knew the account"));

        // when
        mailApplicationService.poll("test");

        // then
        var mailbox = mailboxRepository.findByAccount("test").orElseThrow();
        assertThat(mailbox.status()).isEqualTo(MailboxStatus.OK);
        assertThat(mailbox.address()).isEqualTo(GreenMailConfig.TEST_ADDRESS);
        assertThat(mailbox.folder()).isEqualTo("INBOX");
        assertThat(mailbox.uidValidity()).isNotNull();
        assertThat(mailbox.lastUid()).isPositive();
        assertThat(mailbox.lastPolledDate()).isNotNull();
        assertThat(emailRepository.findByMailboxId(mailbox.id())).isEmpty();
    }

    @Test
    void thatANewEmailShouldResultIntoASavedAndPublishedEmail() throws Exception {
        // given
        mailApplicationService.poll("other");
        var content = new MimeMultipart();
        var alternative = new MimeMultipart("alternative");
        alternative.addBodyPart(html("<p>Please pay <b>the invoice</b> by Friday.</p>"));
        var body = new MimeBodyPart();
        body.setContent(alternative);
        content.addBodyPart(body);
        var attachment = new MimeBodyPart();
        attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(new byte[] {1, 2, 3}, "application/pdf")));
        attachment.setFileName("invoice.pdf");
        content.addBodyPart(attachment);

        // when
        send(GreenMailConfig.OTHER_ADDRESS, "Invoice 4711", content);
        mailApplicationService.poll("other");

        // then
        var event = awaitRecord();
        var mailbox = mailboxRepository.findByAccount("other").orElseThrow();
        var email = emailRepository.findByMailboxId(mailbox.id()).getFirst();
        assertThat(email.subject()).isEqualTo("Invoice 4711");
        assertThat(email.sender()).isEqualTo("Sender <sender@luna.test>");
        assertThat(email.recipients()).containsExactly(GreenMailConfig.OTHER_ADDRESS);
        assertThat(email.text()).isEqualTo("Please pay the invoice by Friday.");
        assertThat(email.attachments()).singleElement().satisfies(saved -> {
            assertThat(saved.name()).isEqualTo("invoice.pdf");
            assertThat(saved.contentType()).isEqualTo("application/pdf");
        });
        assertThat(mailbox.lastUid()).isEqualTo(email.uid());
        assertThat(event.get("id").asString()).isEqualTo(email.id().toString());
        assertThat(event.get("data").get("account").asString()).isEqualTo("other");
        assertThat(event.get("data").get("address").asString()).isEqualTo(GreenMailConfig.OTHER_ADDRESS);
        assertThat(event.get("data").get("subject").asString()).isEqualTo("Invoice 4711");
        assertThat(event.get("data").get("from").asString()).isEqualTo("Sender <sender@luna.test>");
        assertThat(event.get("data").get("text").asString()).isEqualTo("Please pay the invoice by Friday.");
        assertThat(event.get("data").get("attachments").get(0).get("name").asString()).isEqualTo("invoice.pdf");
        assertThat(event.get("audit").get("version").asLong()).isZero();
    }

    @Test
    void thatPollingWithoutNewEmailsShouldResultIntoNoEmail() throws Exception {
        // given
        send(GreenMailConfig.TEST_ADDRESS, "Known email", plainText("Already there"));
        mailApplicationService.poll("test");

        // when
        mailApplicationService.poll("test");

        // then
        var mailbox = mailboxRepository.findByAccount("test").orElseThrow();
        assertThat(emailRepository.findByMailboxId(mailbox.id())).isEmpty();
        assertThat(mailbox.status()).isEqualTo(MailboxStatus.OK);
    }

    @Test
    void thatAChangedUidValidityShouldResultIntoTheCurrentStateWithoutOlderEmails() throws Exception {
        // given
        mailApplicationService.poll("test");
        var mailbox = mailboxRepository.findByAccount("test").orElseThrow();
        mailboxRepository.save(mailbox.toBuilder().uidValidity(mailbox.uidValidity() + 1).lastUid(0L).build());
        send(GreenMailConfig.TEST_ADDRESS, "During the change", plainText("Not fetched"));

        // when
        mailApplicationService.poll("test");

        // then
        var polled = mailboxRepository.findByAccount("test").orElseThrow();
        assertThat(polled.uidValidity()).isEqualTo(mailbox.uidValidity());
        assertThat(polled.lastUid()).isPositive();
        assertThat(emailRepository.findByMailboxId(polled.id())).isEmpty();
    }

    @Test
    void thatAFailingAccountShouldResultIntoAFailedMailboxWithItsError() {
        // when
        mailApplicationService.poll("broken");
        mailApplicationService.poll("broken");

        // then
        var mailbox = mailboxRepository.findByAccount("broken").orElseThrow();
        assertThat(mailbox.status()).isEqualTo(MailboxStatus.FAILED);
        assertThat(mailbox.failures()).isEqualTo(2);
        assertThat(mailbox.lastError()).isNotBlank();
        assertThat(mailbox.uidValidity()).isNull();
    }

    @Test
    void thatASuccessfulPollAfterAFailureShouldResultIntoAnOkMailbox() {
        // given
        mailApplicationService.poll("test");
        var mailbox = mailboxRepository.findByAccount("test").orElseThrow();
        mailboxRepository.save(mailbox.toBuilder().status(MailboxStatus.FAILED).failures(3).lastError("down").build());

        // when
        mailApplicationService.poll("test");

        // then
        var polled = mailboxRepository.findByAccount("test").orElseThrow();
        assertThat(polled.status()).isEqualTo(MailboxStatus.OK);
        assertThat(polled.failures()).isZero();
        assertThat(polled.lastError()).isNull();
    }

    private MimeMultipart plainText(String text) throws Exception {
        var content = new MimeMultipart();
        var part = new MimeBodyPart();
        part.setText(text);
        content.addBodyPart(part);
        return content;
    }

    private MimeBodyPart html(String html) throws Exception {
        var part = new MimeBodyPart();
        part.setContent(html, "text/html; charset=UTF-8");
        return part;
    }

    private void send(String to, String subject, MimeMultipart content) throws Exception {
        var properties = new Properties();
        properties.put("mail.smtp.host", greenMailContainer.getHost());
        properties.put("mail.smtp.port", String.valueOf(greenMailContainer.getMappedPort(GreenMailConfig.SMTP_PORT)));
        var message = new MimeMessage(Session.getInstance(properties));
        message.setFrom(new InternetAddress("sender@luna.test", "Sender"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setContent(content);
        Transport.send(message);
    }

    private JsonNode awaitRecord() {
        var values = new ArrayList<String>();
        await().atMost(TIMEOUT).until(() -> {
            KafkaTestUtils.getRecords(consumer, Duration.ofMillis(200)).records(EMAIL_TOPIC)
                    .forEach(record -> values.add(record.value()));
            return !values.isEmpty();
        });
        return jsonMapper.readTree(values.getFirst());
    }
}
