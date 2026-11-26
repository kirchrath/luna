package de.kirchrath.luna.task.data.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;

import lombok.With;

/**
 * Last processed version of a task from luna-core. {@code version} is the version of the core event, not an
 * optimistic lock: only events with a newer version are processed further.
 */
public record CoreTaskProjection(@With @Id UUID id, UUID coreTaskId, @With Long version)
        implements EntityWithId<CoreTaskProjection> {
}
