package de.kirchrath.luna.note.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record NoteListDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<NoteListItemDto> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount) {
}
