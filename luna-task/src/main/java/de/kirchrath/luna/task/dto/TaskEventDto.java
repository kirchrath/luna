package de.kirchrath.luna.task.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** Full state of a task, published on Kafka after every change. */
public record TaskEventDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AuditDto audit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDataDto data) {
}
