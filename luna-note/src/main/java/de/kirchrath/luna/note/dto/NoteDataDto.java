package de.kirchrath.luna.note.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record NoteDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NoteStatusDto status,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime archivedDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) NoteSourceDto source,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) UUID sourceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LifeAreaDto area,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean edited) {
}
