package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppointmentEventDataDto(
        @NotBlank String title,
        @NotNull OffsetDateTime start,
        OffsetDateTime end,
        @NotNull Boolean allDay,
        String location,
        String description) {
}
