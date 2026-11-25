package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskEventDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime dueDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull TaskPriorityDto priority) {
}
