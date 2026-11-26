package de.kirchrath.luna.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** New data of a note; {@code version} is the version the client read (optimistic locking). */
public record NoteUpdateDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 500) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 10_000) String summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long version) {
}
