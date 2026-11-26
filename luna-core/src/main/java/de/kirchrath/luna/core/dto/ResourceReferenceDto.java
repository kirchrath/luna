package de.kirchrath.luna.core.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record ResourceReferenceDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id) {
}
