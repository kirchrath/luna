package de.kirchrath.luna.appointment.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * New data of an appointment; {@code version} is the version the client read (optimistic locking). All-day
 * appointments start and end at 00:00 local time, {@code end} being the last day.
 */
public record AppointmentUpdateDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 500) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull OffsetDateTime start,
        OffsetDateTime end,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean allDay,
        @Size(max = 500) String location,
        @Size(max = 10_000) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long version) {
}
