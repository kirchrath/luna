package de.kirchrath.luna.core.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Note element published by luna-core")
public record NoteEventDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AuditDto audit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NoteEventDataDto data) implements ElementEventDto {
}
