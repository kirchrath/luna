package de.kirchrath.luna.note.data.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import lombok.Builder;
import lombok.With;

@Builder(toBuilder = true)
public record Note(
        @With @Id UUID id,
        NoteSource source,
        UUID sourceId,
        String title,
        String summary,
        NoteStatus status,
        OffsetDateTime archivedDate,
        boolean edited,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<Note> {
}
