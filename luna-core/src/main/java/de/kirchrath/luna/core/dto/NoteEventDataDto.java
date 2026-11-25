package de.kirchrath.luna.core.dto;

import jakarta.validation.constraints.NotBlank;

public record NoteEventDataDto(
        @NotBlank String title,
        @NotBlank String summary) {
}
