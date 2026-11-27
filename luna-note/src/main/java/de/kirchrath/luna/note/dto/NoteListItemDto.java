package de.kirchrath.luna.note.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record NoteListItemDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LifeAreaDto area,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NoteStatusDto status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean edited,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdDate) {
}
