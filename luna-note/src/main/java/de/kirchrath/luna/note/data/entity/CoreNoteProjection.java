package de.kirchrath.luna.note.data.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;

import lombok.With;

/**
 * Last processed version of a note from luna-core. {@code version} is the version of the core event, not an
 * optimistic lock: only events with a newer version are processed further.
 */
public record CoreNoteProjection(@With @Id UUID id, UUID coreNoteId, @With Long version)
        implements EntityWithId<CoreNoteProjection> {
}
