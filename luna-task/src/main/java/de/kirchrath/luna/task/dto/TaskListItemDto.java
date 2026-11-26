package de.kirchrath.luna.task.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskListItemDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        OffsetDateTime dueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskPriorityDto priority,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskStatusDto status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean edited) {
}
