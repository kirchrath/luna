package de.kirchrath.luna.core.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record NoteEventDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AuditDto audit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LifeAreaDto area,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NoteEventDataDto data) implements ElementEventDto {
}
