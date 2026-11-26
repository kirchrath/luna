package de.kirchrath.luna.appointment.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record AppointmentListDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AppointmentListItemDto> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount) {
}
