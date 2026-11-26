package de.kirchrath.luna.alert.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AlertDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AlertDataDto data,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AuditDto audit) {
}
