package de.kirchrath.luna.alert.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** Full state of an alert, published on Kafka after every change. */
public record AlertEventDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AuditDto audit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AlertDataDto data) {
}
