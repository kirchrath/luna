package de.kirchrath.luna.core.data.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

public record InboundEvent(
        @Id UUID id,
        EventSource source,
        EventData data,
        InboundEventStatus status,
        int attempts,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<InboundEvent> {

    @Override
    public InboundEvent withId(UUID id) {
        return new InboundEvent(id, source, data, status, attempts, createdDate, lastModifiedDate, version);
    }

    public InboundEvent withStatus(InboundEventStatus status) {
        return new InboundEvent(id, source, data, status, attempts, createdDate, lastModifiedDate, version);
    }

    public InboundEvent withAttempts(int attempts) {
        return new InboundEvent(id, source, data, status, attempts, createdDate, lastModifiedDate, version);
    }
}
