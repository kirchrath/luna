package de.kirchrath.luna.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AppointmentDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AppointmentDataDto data,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AuditDto audit) {
}
