package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppointmentEventDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull OffsetDateTime start,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime end,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Boolean allDay,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String location,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String description) {
}
