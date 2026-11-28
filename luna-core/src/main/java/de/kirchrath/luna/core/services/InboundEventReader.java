package de.kirchrath.luna.core.services;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.mail.EmailAttachmentDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDataDto;
import tools.jackson.databind.json.JsonMapper;

/** Reads the stored data of an inbound event according to its source. */
@Component
public class InboundEventReader {

    private static final String LINE_BREAK = "\n";
    private static final String SEPARATOR = ", ";
    private static final String FROM = "From: ";
    private static final String TO = "To: ";
    private static final String SUBJECT = "Subject: ";
    private static final String RECEIVED = "Received: ";
    private static final String ATTACHMENTS = "Attachments: ";
    private static final String CONTENT_TYPE_START = " (";
    private static final String CONTENT_TYPE_END = ")";
    private static final String EMPTY_LINE = "";

    private final JsonMapper jsonMapper;
    private final TimeProvider timeProvider;

    public InboundEventReader(JsonMapper jsonMapper, TimeProvider timeProvider) {
        this.jsonMapper = jsonMapper;
        this.timeProvider = timeProvider;
    }

    /** The input of a manual inbound event, {@code null} for other sources. */
    public ManualInputDataDto manualInput(InboundEvent inboundEvent) {
        return inboundEvent.source() == EventSource.MANUAL
                ? jsonMapper.readValue(inboundEvent.data().json(), ManualInputDataDto.class)
                : null;
    }

    /** The email of a mail inbound event, {@code null} for other sources. */
    public EmailEventDataDto email(InboundEvent inboundEvent) {
        return inboundEvent.source() == EventSource.MAIL
                ? jsonMapper.readValue(inboundEvent.data().json(), EmailEventDataDto.class)
                : null;
    }

    /**
     * Manual inputs are analyzed as entered, relative to their creation. Emails are analyzed with their header and
     * attachment names, relative to their arrival in the mailbox.
     */
    public AnalysisInput analysisInput(InboundEvent inboundEvent) {
        if (inboundEvent.source() == EventSource.MAIL) {
            var email = email(inboundEvent);
            // Stored dates are in UTC; relative dates such as "tomorrow" refer to the day at the server.
            var received = timeProvider.atServerOffset(email.getReceivedDate().toInstant());
            return new AnalysisInput(inboundEvent.area(), text(email, received), null, received);
        }
        var input = manualInput(inboundEvent);
        return new AnalysisInput(inboundEvent.area(), input.text(), input.typeHint(), inboundEvent.createdDate());
    }

    private String text(EmailEventDataDto email, OffsetDateTime received) {
        var lines = new ArrayList<String>();
        if (email.getFrom() != null) {
            lines.add(FROM + email.getFrom());
        }
        lines.add(TO + email.getTo().stream().collect(Collectors.joining(SEPARATOR)));
        if (email.getSubject() != null) {
            lines.add(SUBJECT + email.getSubject());
        }
        lines.add(RECEIVED + received);
        if (!email.getAttachments().isEmpty()) {
            lines.add(ATTACHMENTS + email.getAttachments().stream()
                    .map(this::attachment)
                    .collect(Collectors.joining(SEPARATOR)));
        }
        lines.add(EMPTY_LINE);
        lines.add(email.getText());
        return lines.stream().collect(Collectors.joining(LINE_BREAK));
    }

    private String attachment(EmailAttachmentDto attachment) {
        return attachment.getName() == null
                ? attachment.getContentType()
                : attachment.getName() + CONTENT_TYPE_START + attachment.getContentType() + CONTENT_TYPE_END;
    }
}
