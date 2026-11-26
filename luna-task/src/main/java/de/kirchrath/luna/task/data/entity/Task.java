package de.kirchrath.luna.task.data.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import lombok.Builder;
import lombok.With;

@Builder(toBuilder = true)
public record Task(
        @With @Id UUID id,
        TaskSource source,
        UUID sourceId,
        String title,
        String description,
        OffsetDateTime dueDate,
        TaskPriority priority,
        TaskStatus status,
        OffsetDateTime completedDate,
        boolean edited,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<Task> {
}
