package de.kirchrath.luna.task.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime dueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskPriorityDto priority,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskStatusDto status,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime completedDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskSourceDto source,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) UUID sourceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LifeAreaDto area,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean edited) {
}
