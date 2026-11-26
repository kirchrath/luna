package de.kirchrath.luna.mail.outbound;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.mail.config.MailProperties;
import de.kirchrath.luna.mail.services.HtmlTextConverter;
import de.kirchrath.luna.mail.services.TimeProvider;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/** Reads the parts of an email that Luna uses: header, text (plain text preferred) and attachment names. */
@Component
public class EmailParser {

    private static final String MULTIPART = "multipart/*";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TEXT_HTML = "text/html";
    private static final String EMPTY = "";
    private static final int UNKNOWN_SIZE = -1;

    private final HtmlTextConverter htmlTextConverter;
    private final TimeProvider timeProvider;
    private final MailProperties mailProperties;

    public EmailParser(HtmlTextConverter htmlTextConverter, TimeProvider timeProvider, MailProperties mailProperties) {
        this.htmlTextConverter = htmlTextConverter;
        this.timeProvider = timeProvider;
        this.mailProperties = mailProperties;
    }

    public FetchedEmail parse(long uid, MimeMessage message) throws MessagingException, IOException {
        var content = new Content();
        collect(message, content);
        var text = content.text();
        var truncated = text.length() > mailProperties.maxTextLength();
        var receivedDate = message.getReceivedDate();
        return new FetchedEmail(
                uid,
                message.getMessageID(),
                first(message.getFrom()),
                all(message.getAllRecipients()),
                message.getSubject(),
                toOffsetDateTime(message.getSentDate()),
                receivedDate == null ? timeProvider.now() : toOffsetDateTime(receivedDate),
                truncated ? text.substring(0, mailProperties.maxTextLength()) : text,
                truncated,
                List.copyOf(content.attachments));
    }

    private void collect(Part part, Content content) throws MessagingException, IOException {
        if (part.isMimeType(MULTIPART)) {
            var multipart = (Multipart) part.getContent();
            for (var index = 0; index < multipart.getCount(); index++) {
                collect(multipart.getBodyPart(index), content);
            }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || part.getFileName() != null) {
            var size = part.getSize();
            content.attachments.add(new FetchedAttachment(
                    part.getFileName(),
                    new ContentType(part.getContentType()).getBaseType().toLowerCase(Locale.ROOT),
                    size == UNKNOWN_SIZE ? null : (long) size));
        } else if (part.isMimeType(TEXT_PLAIN) && content.plain == null) {
            content.plain = (String) part.getContent();
        } else if (part.isMimeType(TEXT_HTML) && content.html == null) {
            content.html = (String) part.getContent();
        }
    }

    private String first(Address[] addresses) {
        var all = all(addresses);
        return all.isEmpty() ? null : all.getFirst();
    }

    private List<String> all(Address[] addresses) {
        var result = new ArrayList<String>();
        if (addresses != null) {
            for (var address : addresses) {
                result.add(display((InternetAddress) address));
            }
        }
        return List.copyOf(result);
    }

    /** "Name <address>" without the quotes and encodings of the header, or just the address. */
    private String display(InternetAddress address) {
        var personal = address.getPersonal();
        return personal == null ? address.getAddress() : personal + " <" + address.getAddress() + ">";
    }

    private OffsetDateTime toOffsetDateTime(Date date) {
        return date == null ? null : timeProvider.atServerOffset(date.toInstant());
    }

    private final class Content {

        private final List<FetchedAttachment> attachments = new ArrayList<>();
        private String plain;
        private String html;

        private String text() {
            if (plain != null) {
                return plain.strip();
            }
            return html == null ? EMPTY : htmlTextConverter.toText(html);
        }
    }
}
