package de.kirchrath.luna.appointment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record AppointmentListItemDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime start,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime end,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean allDay,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String location,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AppointmentStatusDto status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean edited) {
}
