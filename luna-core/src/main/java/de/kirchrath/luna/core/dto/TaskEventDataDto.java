package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskEventDataDto(
        @NotBlank String title,
        String description,
        OffsetDateTime dueDate,
        @NotNull TaskPriorityDto priority) {
}
