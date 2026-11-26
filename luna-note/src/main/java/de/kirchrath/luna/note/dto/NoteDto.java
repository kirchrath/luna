package de.kirchrath.luna.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NoteDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NoteDataDto data,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AuditDto audit) {
}
