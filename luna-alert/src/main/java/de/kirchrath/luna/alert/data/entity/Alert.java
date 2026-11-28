package de.kirchrath.luna.alert.data.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import lombok.Builder;
import lombok.With;

/**
 * An alert. Its content always comes from luna-core; the user only changes the status. {@code severityRank} orders
 * the list by severity (most severe first), because the severity itself is stored as text.
 */
@Builder(toBuilder = true)
public record Alert(
        @With @Id UUID id,
        AlertSource source,
        UUID sourceId,
        LifeArea area,
        String title,
        String message,
        AlertSeverity severity,
        int severityRank,
        String affectedSystem,
        AlertStatus status,
        OffsetDateTime acknowledgedDate,
        OffsetDateTime resolvedDate,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<Alert> {
}
