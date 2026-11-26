package de.kirchrath.luna.alert.data.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;

import lombok.With;

/**
 * Last processed version of a alert from luna-core. {@code version} is the version of the core event, not an
 * optimistic lock: only events with a newer version are processed further.
 */
public record CoreAlertProjection(@With @Id UUID id, UUID coreAlertId, @With Long version)
        implements EntityWithId<CoreAlertProjection> {
}
