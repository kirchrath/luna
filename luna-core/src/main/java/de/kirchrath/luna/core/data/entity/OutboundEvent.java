package de.kirchrath.luna.core.data.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

public record OutboundEvent(
        @Id UUID id,
        UUID inboundEventId,
        LunaElementType type,
        OutboundEventStatus status,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<OutboundEvent> {

    @Override
    public OutboundEvent withId(UUID id) {
        return new OutboundEvent(id, inboundEventId, type, status, createdDate, lastModifiedDate, version);
    }
}
