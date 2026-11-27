package de.kirchrath.luna.mail.data.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import lombok.Builder;
import lombok.With;

/**
 * The polling state of one configured account: emails up to {@code lastUid} within {@code uidValidity} are known.
 */
@Builder(toBuilder = true)
public record Mailbox(
        @With @Id UUID id,
        String account,
        String address,
        String folder,
        Long uidValidity,
        Long lastUid,
        MailboxStatus status,
        int failures,
        String lastError,
        OffsetDateTime lastPolledDate,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<Mailbox> {
}
