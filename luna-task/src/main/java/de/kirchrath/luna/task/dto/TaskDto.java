package de.kirchrath.luna.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDataDto data,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AuditDto audit) {
}
