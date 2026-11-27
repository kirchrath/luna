package de.kirchrath.luna.mail.outbound;

import java.time.OffsetDateTime;
import java.util.List;

public record FetchedEmail(
        long uid,
        String messageId,
        String sender,
        List<String> recipients,
        String subject,
        OffsetDateTime sentDate,
        OffsetDateTime receivedDate,
        String text,
        boolean textTruncated,
        List<FetchedAttachment> attachments) {
}
