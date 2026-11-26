package de.kirchrath.luna.alert.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record AlertDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AlertSeverityDto severity,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String affectedSystem,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AlertStatusDto status,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime acknowledgedDate,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime resolvedDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AlertSourceDto source,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) UUID sourceId) {
}
