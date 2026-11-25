package de.kirchrath.luna.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertEventDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String message,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull AlertSeverityDto severity,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String affectedSystem) {
}
