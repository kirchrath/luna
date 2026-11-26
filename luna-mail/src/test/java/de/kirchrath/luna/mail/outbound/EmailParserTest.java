package de.kirchrath.luna.mail.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import de.kirchrath.luna.mail.config.MailProperties;
import de.kirchrath.luna.mail.services.HtmlTextConverter;
import de.kirchrath.luna.mail.services.TimeProvider;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

class EmailParserTest {

    private static final int MAX_TEXT_LENGTH = 20;
    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 26, 12, 0, 0, 0, ZoneOffset.ofHours(1));
    private static final Session SESSION = Session.getInstance(new Properties());

    private final TimeProvider timeProvider = new TimeProvider() {
        @Override
        public OffsetDateTime now() {
            return NOW;
        }
    };

    private final EmailParser emailParser = new EmailParser(
            new HtmlTextConverter(),
            timeProvider,
            new MailProperties(Duration.ofMinutes(1), MAX_TEXT_LENGTH, Map.of(), null, Map.of()));

    @Test
    void thatParsingAPlainTextEmailShouldResultIntoItsHeaderAndText() throws Exception {
        // given
        var sent = new Date(1_764_150_000_000L);
        var message = new MimeMessage(SESSION);
        message.setFrom(new InternetAddress("jürgen@luna.test", "Jürgen Müller"));
        message.setRecipients(Message.RecipientType.TO, "Luna <luna@luna.test>, other@luna.test");
        message.setRecipients(Message.RecipientType.CC, "copy@luna.test");
        message.setSubject("Short");
        message.setSentDate(sent);
        message.setText("  Call me back  ");
        message.saveChanges();

        // when
        var email = emailParser.parse(7, message);

        // then
        assertThat(email.uid()).isEqualTo(7);
        assertThat(email.messageId()).isNotBlank();
        assertThat(email.sender()).isEqualTo("Jürgen Müller <jürgen@luna.test>");
        assertThat(email.recipients()).containsExactly("Luna <luna@luna.test>", "other@luna.test", "copy@luna.test");
        assertThat(email.subject()).isEqualTo("Short");
        assertThat(email.sentDate()).isEqualTo(timeProvider.atServerOffset(sent.toInstant()));
        assertThat(email.receivedDate()).isEqualTo(NOW);
        assertThat(email.text()).isEqualTo("Call me back");
        assertThat(email.textTruncated()).isFalse();
        assertThat(email.attachments()).isEmpty();
    }

    @Test
    void thatParsingAnEmailWithoutHeaderAndTextShouldResultIntoEmptyValues() throws Exception {
        // given
        var message = new MimeMessage(SESSION);
        var content = new MimeMultipart();
        var image = new MimeBodyPart();
        image.setDataHandler(new DataHandler(new ByteArrayDataSource(new byte[] {1}, "image/png")));
        content.addBodyPart(image);
        message.setContent(content);
        message.saveChanges();
        message.removeHeader("Date");

        // when
        var email = emailParser.parse(1, message);

        // then
        assertThat(email.sender()).isNull();
        assertThat(email.recipients()).isEmpty();
        assertThat(email.subject()).isNull();
        assertThat(email.sentDate()).isNull();
        assertThat(email.text()).isEmpty();
        assertThat(email.attachments()).isEmpty();
    }

    @Test
    void thatParsingALongHtmlEmailShouldResultIntoTruncatedTextOfTheFirstHtmlPart() throws Exception {
        // given
        var message = new MimeMessage(SESSION);
        var content = new MimeMultipart("alternative");
        content.addBodyPart(part("<p>The quick brown fox jumps over the lazy dog</p>", "text/html; charset=UTF-8"));
        content.addBodyPart(part("<p>Second</p>", "text/html; charset=UTF-8"));
        message.setContent(content);
        message.saveChanges();

        // when
        var email = emailParser.parse(1, message);

        // then
        assertThat(email.text()).isEqualTo("The quick brown fox ");
        assertThat(email.textTruncated()).isTrue();
    }

    @Test
    void thatParsingAnEmailWithAttachmentsShouldResultIntoTheFirstPlainTextAndTheAttachmentNames() throws Exception {
        // given
        var content = new MimeMultipart();
        content.addBodyPart(part("First text", "text/plain; charset=UTF-8"));
        content.addBodyPart(part("Second text", "text/plain; charset=UTF-8"));
        content.addBodyPart(part("<p>Html</p>", "text/html; charset=UTF-8"));
        var invoice = new MimeBodyPart();
        invoice.setDataHandler(new DataHandler(new ByteArrayDataSource(new byte[] {1, 2, 3}, "Application/PDF")));
        invoice.setFileName("invoice.pdf");
        content.addBodyPart(invoice);
        var unnamed = new MimeBodyPart();
        unnamed.setDataHandler(new DataHandler(new ByteArrayDataSource(new byte[] {4}, "text/csv")));
        unnamed.setDisposition(Part.ATTACHMENT);
        content.addBodyPart(unnamed);
        var logo = new MimeBodyPart();
        logo.setDataHandler(new DataHandler(new ByteArrayDataSource(new byte[] {5}, "image/png")));
        logo.setDisposition(Part.INLINE);
        logo.setFileName("logo.png");
        content.addBodyPart(logo);
        var original = new MimeMessage(SESSION);
        original.setContent(content);
        original.saveChanges();
        var bytes = new ByteArrayOutputStream();
        original.writeTo(bytes);
        var parsed = new MimeMessage(SESSION, new ByteArrayInputStream(bytes.toByteArray()));
        var inMemory = new MimeMessage(SESSION);
        inMemory.setContent(content);
        inMemory.saveChanges();

        // when
        var email = emailParser.parse(1, parsed);
        var withUnknownSizes = emailParser.parse(2, inMemory);

        // then
        assertThat(email.text()).isEqualTo("First text");
        assertThat(email.attachments()).hasSize(3);
        assertThat(email.attachments().getFirst().name()).isEqualTo("invoice.pdf");
        assertThat(email.attachments().getFirst().contentType()).isEqualTo("application/pdf");
        assertThat(email.attachments().getFirst().size()).isPositive();
        assertThat(email.attachments().get(1).name()).isNull();
        assertThat(email.attachments().get(1).contentType()).isEqualTo("text/csv");
        assertThat(email.attachments().get(2).name()).isEqualTo("logo.png");
        assertThat(withUnknownSizes.attachments().getFirst().size()).isNull();
    }

    private MimeBodyPart part(String text, String contentType) throws Exception {
        var part = new MimeBodyPart();
        part.setContent(text, contentType);
        return part;
    }
}
