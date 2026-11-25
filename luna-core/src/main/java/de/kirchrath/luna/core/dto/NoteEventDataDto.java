package de.kirchrath.luna.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record NoteEventDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String summary) {
}
