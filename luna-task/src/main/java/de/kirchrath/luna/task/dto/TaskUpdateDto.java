package de.kirchrath.luna.task.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** New data of a task; {@code version} is the version the client read (optimistic locking). */
public record TaskUpdateDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 500) String title,
        @Size(max = 10_000) String description,
        OffsetDateTime dueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull TaskPriorityDto priority,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long version) {
}
