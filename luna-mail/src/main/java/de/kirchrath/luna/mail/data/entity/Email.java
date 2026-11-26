package de.kirchrath.luna.mail.data.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;

import lombok.Builder;
import lombok.With;

@Builder(toBuilder = true)
public record Email(
        @With @Id UUID id,
        UUID mailboxId,
        long uidValidity,
        long uid,
        String messageId,
        String sender,
        List<String> recipients,
        String subject,
        OffsetDateTime sentDate,
        OffsetDateTime receivedDate,
        String text,
        boolean textTruncated,
        @MappedCollection(idColumn = "email", keyColumn = "email_key") List<EmailAttachment> attachments,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<Email> {
}
