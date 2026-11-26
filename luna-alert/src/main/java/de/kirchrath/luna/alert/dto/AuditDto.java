package de.kirchrath.luna.alert.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuditDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime lastModifiedDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version) {
}
