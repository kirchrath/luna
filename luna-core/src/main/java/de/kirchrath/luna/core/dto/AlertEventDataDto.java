package de.kirchrath.luna.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertEventDataDto(
        @NotBlank String title,
        @NotBlank String message,
        @NotNull AlertSeverityDto severity,
        String affectedSystem) {
}
