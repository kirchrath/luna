package de.kirchrath.luna.core.data.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import lombok.With;

public record AlertElement(
        @With @Id UUID id,
        UUID outboundEventId,
        String title,
        String message,
        AlertSeverity severity,
        String affectedSystem,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<AlertElement> {
}
