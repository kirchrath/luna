package de.kirchrath.luna.alert.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record AlertListItemDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AlertSeverityDto severity,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String affectedSystem,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AlertStatusDto status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdDate) {
}
